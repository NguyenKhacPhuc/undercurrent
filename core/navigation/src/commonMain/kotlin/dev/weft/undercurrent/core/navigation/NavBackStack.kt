package dev.weft.undercurrent.core.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Marker for every entry that can sit on a [NavBackStack]. Mirrors the
 * shape of `androidx.navigation3.runtime.NavKey` deliberately —
 * see [NavBackStack].
 */
interface NavKey

/**
 * Compose-aware list of [NavKey] entries. The top entry (last) is
 * the current screen; the rest is the history that [AppIntent.Back]
 * pops one at a time. Reads are tracked by Compose's snapshot system
 * so `snapshotFlow { backStack.lastOrNull() }` recomposes / re-emits
 * on every mutation.
 *
 * Why we ship our own instead of using `androidx.navigation3.runtime
 * .NavBackStack`:
 *
 *   - The official nav3 runtime artifact brings transitive
 *     `androidx.compose.runtime:runtime-saveable`, which collides at
 *     K/N IR-link time with the JetBrains-flavored
 *     `org.jetbrains.compose.runtime:runtime-saveable` that CMP
 *     already provides (`IrPropertySymbolImpl is already bound`
 *     against `LocalSaveableStateRegistry`).
 *   - The JetBrains-flavored `org.jetbrains.androidx.navigation3
 *     :navigation3-ui` patches that, but requires CMP 1.10+ — and
 *     CMP 1.10's `material3` artifact still ships as alpha + the
 *     `material-icons-extended` artifact is dropped entirely past
 *     1.7.3, so the bump has its own migration cost.
 *
 * Until either of those constraints relaxes, we keep the back-stack
 * data model + the same API shape (`NavBackStack(vararg initial)`,
 * `lastOrNull()`, `add`, `removeAt(lastIndex)`, `clear()`,
 * `lastIndex`, `size`). Swapping in the real lib later means
 * deleting this file and changing the import.
 *
 * Implementation: delegates to a private [SnapshotStateList] so every
 * mutation triggers a snapshot write and Compose readers recompose.
 */
class NavBackStack<T : NavKey> private constructor(
    private val items: SnapshotStateList<T>,
) : MutableList<T> by items {

    constructor(vararg initial: T) : this(
        mutableStateListOf<T>().apply { for (entry in initial) add(entry) },
    )
}
