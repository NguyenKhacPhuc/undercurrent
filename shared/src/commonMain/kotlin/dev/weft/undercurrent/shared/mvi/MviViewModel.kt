package dev.weft.undercurrent.shared.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch as coroutinesLaunch

abstract class MviViewModel<State, Intent, Effect>(
    initialState: State,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    protected val current: State get() = _state.value

    abstract fun dispatch(intent: Intent): Job

    protected fun update(reducer: (State) -> State) {
        _state.value = reducer(_state.value)
    }

    protected fun emit(effect: Effect) {
        _effects.trySend(effect)
    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.coroutinesLaunch(block = block)

    protected fun <T> Flow<T>.collectInto(reducer: State.(T) -> State): Job =
        viewModelScope.coroutinesLaunch {
            collect { value -> update { it.reducer(value) } }
        }

    protected fun <T> Flow<T>.observe(block: suspend (T) -> Unit): Job =
        viewModelScope.coroutinesLaunch { collect(block) }

    protected fun setLoading(value: Boolean) {
        _loading.value = value
    }

    protected suspend inline fun <T> withLoading(block: () -> T): T {
        setLoading(true)
        try {
            return block()
        } finally {
            setLoading(false)
        }
    }
}
