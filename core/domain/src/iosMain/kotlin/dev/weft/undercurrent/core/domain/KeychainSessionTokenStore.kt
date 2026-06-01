package dev.weft.undercurrent.core.domain

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
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
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
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
 * iOS impl of [SessionTokenStore]. Single-slot Keychain entry keyed off
 * (`SERVICE`, `SESSION_TOKEN_ACCOUNT`).
 *
 * Mirrors the shape of [KeychainKeyVaultRepository]: same `kSecAttrService`
 * so per-app Keychain items live together; `kSecAttrAccessibleAfterFirstUnlock`
 * so the token is available once the device has been unlocked after boot.
 *
 * Keychain entries survive app uninstall by Apple's default — that's the
 * known iOS UX wart called out in the Inception. The Sign Out flow
 * (mobile-auth-wiring/06) is the user's clean path to remove the token.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KeychainSessionTokenStore : SessionTokenStore {

    override suspend fun save(token: String) {
        // Delete-then-add for idempotency. Avoids errSecDuplicateItem and
        // the more elaborate SecItemUpdate path.
        clear()
        val data = (token as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            ?: error("Failed to encode session token as UTF-8")
        memScoped {
            val query = CFDictionaryCreateMutable(
                kCFAllocatorDefault,
                4,
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr,
            )
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, retain(SERVICE))
            CFDictionaryAddValue(query, kSecAttrAccount, retain(SESSION_TOKEN_ACCOUNT))
            CFDictionaryAddValue(query, kSecValueData, retain(data))
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)
            val status = SecItemAdd(query, null)
            if (status != errSecSuccess) {
                error("Keychain SecItemAdd failed: status=$status")
            }
        }
    }

    override suspend fun read(): String? = memScoped {
        val query = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            4,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, retain(SERVICE))
        CFDictionaryAddValue(query, kSecAttrAccount, retain(SESSION_TOKEN_ACCOUNT))
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)

        val resultVar = alloc<CFTypeRefVar>()
        val status: OSStatus = SecItemCopyMatching(query, resultVar.ptr)
        if (status == errSecItemNotFound) return@memScoped null
        if (status != errSecSuccess) return@memScoped null

        val data = CFBridgingRelease(resultVar.value) as? NSData ?: return@memScoped null
        NSString.create(data, NSUTF8StringEncoding) as String?
    }

    override suspend fun clear() {
        memScoped {
            val query = CFDictionaryCreateMutable(
                kCFAllocatorDefault,
                2,
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr,
            )
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, retain(SERVICE))
            CFDictionaryAddValue(query, kSecAttrAccount, retain(SESSION_TOKEN_ACCOUNT))
            // Ignore the status — `errSecItemNotFound` is a valid outcome
            // (caller asked us to clear something that wasn't there).
            SecItemDelete(query)
        }
    }

    private companion object {
        const val SERVICE: String = "dev.weft.undercurrent"
        const val SESSION_TOKEN_ACCOUNT: String = "be_session_token"
    }
}

/** Bridge an Objective-C/Foundation object to a `+1`-retained `CFTypeRef`. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UNCHECKED_CAST")
private fun retain(obj: Any): CFTypeRef? = CFBridgingRetain(obj)
