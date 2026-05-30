package dev.weft.undercurrent.shared.mvi

import kotlinx.coroutines.CoroutineScope

/**
 * The pieces of an [MviViewModel] that a controller / collaborator
 * legitimately needs to do its job — read current state, mutate
 * state atomically, emit one-shot effects, and launch coroutines on
 * the VM's lifecycle scope.
 *
 * Why this exists: extracting per-feature handlers out of a root VM
 * into separate controller classes only works if the controllers can
 * touch state + effects + scope without the VM exposing its
 * `protected` MviViewModel methods directly. This interface is the
 * minimal contract. The VM provides itself as an [MviContext] via a
 * private inner object (which captures `update` / `emit` via outer-
 * class access).
 *
 * Unit-testable: controllers can be tested in isolation against a
 * hand-rolled fake `MviContext` that records updates + emissions.
 */
interface MviContext<S, E> {

    /** Current state — equivalent to MviViewModel.current. */
    val current: S

    /** The VM's coroutineScope — cancelled when the VM clears. */
    val scope: CoroutineScope

    /**
     * Atomic state update; same semantics as
     * [MviViewModel.update]. Safe from any thread.
     */
    fun update(reducer: (S) -> S)

    /**
     * Emit a one-shot side effect. Equivalent to [MviViewModel.emit].
     */
    fun emit(effect: E)
}
