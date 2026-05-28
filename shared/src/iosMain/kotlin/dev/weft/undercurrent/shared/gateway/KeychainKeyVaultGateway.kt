package dev.weft.undercurrent.shared.gateway

import dev.weft.undercurrent.core.model.ProviderKind
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.OSStatus

/**
 * iOS impl of [KeyVaultGateway]. Backed by the system Keychain via
 * `kSecClassGenericPassword` items.
 *
 * Item shape:
 *  - `kSecAttrService` = "dev.weft.undercurrent"
 *  - `kSecAttrAccount` = the provider's lowercase name (e.g. "anthropic")
 *  - `kSecValueData` = the raw API key bytes (UTF-8)
 *  - `kSecAttrAccessible` = `kSecAttrAccessibleAfterFirstUnlock` —
 *    available once the device has been unlocked once after boot, then
 *    accessible while the device is locked. Right balance for an
 *    assistant app the user might fire from a notification.
 *
 * The Keychain survives app uninstall by default. If we want
 * uninstall-clears behaviour, the app needs to wipe on first launch
 * (track a NSUserDefaults flag). For now we follow Apple's default —
 * the user can revoke the key via Settings → Passwords.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KeychainKeyVaultGateway : KeyVaultGateway {

    override suspend fun putApiKey(provider: ProviderKind, apiKey: String) {
        // Delete-then-add for idempotency. Avoids errSecDuplicateItem
        // and avoids the more elaborate SecItemUpdate path.
        clearApiKey(provider)
        val data = (apiKey as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            ?: error("Failed to encode API key as UTF-8")
        memScoped {
            val query = CFDictionaryCreateMutable(
                kCFAllocatorDefault,
                4,
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr,
            )
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetainNS(SERVICE))
            CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetainNS(provider.account()))
            CFDictionaryAddValue(query, kSecValueData, CFBridgingRetainNS(data))
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)
            val status = SecItemAdd(query, null)
            if (status != errSecSuccess) {
                error("Keychain SecItemAdd failed: status=$status (provider=$provider)")
            }
        }
    }

    override suspend fun getApiKey(provider: ProviderKind): String? = memScoped {
        val query = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            4,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetainNS(SERVICE))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetainNS(provider.account()))
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        CFDictionaryAddValue(query, kSecReturnData, kotlin.native.internal.NativePtr.NULL.toCFBoolean(true))

        val resultVar = alloc<CFTypeRefVar>()
        val status: OSStatus = SecItemCopyMatching(query, resultVar.ptr)
        if (status == errSecItemNotFound) return@memScoped null
        if (status != errSecSuccess) return@memScoped null

        val data = CFBridgingRelease(resultVar.value) as? NSData ?: return@memScoped null
        NSString.create(data, NSUTF8StringEncoding) as String?
    }

    override suspend fun hasApiKey(provider: ProviderKind): Boolean =
        getApiKey(provider) != null

    override suspend fun clearApiKey(provider: ProviderKind) {
        memScoped {
            val query = CFDictionaryCreateMutable(
                kCFAllocatorDefault,
                2,
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr,
            )
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetainNS(SERVICE))
            CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetainNS(provider.account()))
            // Ignore the status — `errSecItemNotFound` is a valid outcome
            // (caller asked us to clear something that wasn't there) and
            // we don't have a meaningful recovery for the other failures.
            SecItemDelete(query)
        }
    }

    private fun ProviderKind.account(): String = name.lowercase()

    private companion object {
        const val SERVICE: String = "dev.weft.undercurrent"
    }
}

/**
 * Bridge a Kotlin/Objective-C object to a +1-retained `CFTypeRef`. The
 * CoreFoundation dictionary takes ownership; we don't need to release.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UNCHECKED_CAST")
private fun CFBridgingRetainNS(obj: Any): platform.CoreFoundation.CFTypeRef? =
    platform.Foundation.CFBridgingRetain(obj)

/**
 * Helper to materialize a CFBoolean for kSecReturnData = true. The
 * SecItem* dictionary accepts either `kCFBooleanTrue` or an NSNumber;
 * the explicit cast keeps the call site readable.
 */
@OptIn(ExperimentalForeignApi::class)
private fun kotlin.native.internal.NativePtr.toCFBoolean(value: Boolean): platform.CoreFoundation.CFTypeRef? =
    if (value) platform.CoreFoundation.kCFBooleanTrue else platform.CoreFoundation.kCFBooleanFalse
