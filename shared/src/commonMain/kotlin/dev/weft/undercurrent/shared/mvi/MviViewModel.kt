package dev.weft.undercurrent.shared.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Generic MVI store base.
 *
 * Each feature defines its own [State] (a data class), [Intent]
 * (a sealed interface), and [Effect] (sealed interface — for
 * one-shot side effects like snackbars / navigation hints), then
 * extends this class and implements [dispatch] as the single entry
 * point.
 *
 * Surface:
 *  - [state] — observable immutable State flow. Compose reads it via
 *    `collectAsState` / `collectAsStateWithLifecycle`.
 *  - [effects] — one-shot side effects. Consumed via
 *    `LaunchedEffect { store.effects.collect { … } }`.
 *  - [dispatch] — the only public mutation. Subclasses route intents
 *    to private handlers; handlers call [update] for state changes
 *    and [emit] for effects.
 *
 * Inherits `viewModelScope` from [ViewModel] — async handlers launch
 * coroutines there and they're cancelled when the screen unmounts
 * (i.e. the ViewModelStoreOwner clears).
 *
 * KMP — commonMain. The lifecycle-viewmodel KMP artifact gives us
 * `androidx.lifecycle.ViewModel` on both Android + iOS targets.
 *
 * Convention: define the State / Intent / Effect types in the same
 * file as the Store (or in sibling files in the same package). Keep
 * them small — one feature, one screen.
 *
 * Example:
 *
 * ```kotlin
 * data class FoosState(val foos: List<Foo> = emptyList())
 * sealed interface FoosIntent {
 *     data object Refresh : FoosIntent
 *     data class Delete(val id: String) : FoosIntent
 * }
 * sealed interface FoosEffect {
 *     data class Toast(val message: String) : FoosEffect
 * }
 *
 * class FoosStore(private val repo: FooRepo) :
 *     MviViewModel<FoosState, FoosIntent, FoosEffect>(FoosState()) {
 *
 *     init {
 *         viewModelScope.launch {
 *             repo.foos.collect { foos -> update { it.copy(foos = foos) } }
 *         }
 *     }
 *
 *     override fun dispatch(intent: FoosIntent) {
 *         when (intent) {
 *             FoosIntent.Refresh -> viewModelScope.launch { repo.refresh() }
 *             is FoosIntent.Delete -> viewModelScope.launch {
 *                 runCatching { repo.delete(intent.id) }
 *                     .onFailure { emit(FoosEffect.Toast("Couldn't delete")) }
 *             }
 *         }
 *     }
 * }
 * ```
 */
abstract class MviViewModel<State, Intent, Effect>(
    initialState: State,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    /** Convenience read of the current state inside handlers. */
    protected val current: State get() = _state.value

    /**
     * Entry point — subclasses route the [intent] to a private handler.
     * May be called from any thread; state updates are atomic and
     * effects are buffered.
     */
    abstract fun dispatch(intent: Intent)

    /**
     * Atomic state update; same semantics as
     * [MutableStateFlow.update] but the closure receives the current
     * state. Calls inside handlers; safe from any thread.
     */
    protected fun update(reducer: (State) -> State) {
        _state.value = reducer(_state.value)
    }

    /**
     * Emit a one-shot side effect to [effects] subscribers. If the
     * channel buffer fills (rare — Channel.BUFFERED default), the
     * oldest effect is dropped to keep the producer unblocked.
     */
    protected fun emit(effect: Effect) {
        _effects.trySend(effect)
    }
}
