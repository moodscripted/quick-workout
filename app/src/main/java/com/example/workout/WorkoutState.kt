package com.example.workout

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class Phase { IDLE, INTRO, WORK, REPS, BREAK, DONE }

data class WorkoutState(
    val phase: Phase = Phase.IDLE,
    val dayTitle: String = "",
    /** Exercise name, or the break label during a break. */
    val title: String = "",
    val group: String = "",
    val how: String = "",
    val videoUrl: String = "",
    val reps: Int = 0,
    val weighted: Boolean = false,
    val secondsLeft: Int = 0,
    /** Exercise number during INTRO/WORK/REPS; number of exercises finished during BREAK. */
    val number: Int = 0,
    val total: Int = 0,
    /** What comes right after this step: an exercise name, or a break with its length. */
    val nextName: String = "",
    /** The exercise after a break, when the next step is a break. */
    val thenName: String = "",
    /** Where this move sits in the workout, for example "Circuit 2, round 1 of 3". Empty when unlabelled. */
    val section: String = "",
    /** Planned length of the whole workout in seconds, 0 when it cannot be estimated. */
    val planned: Int = 0,
    /** Estimated seconds left in the whole workout, -1 when unknown. */
    val remaining: Int = -1,
    /** Set on DONE: the calorie estimate for the finished workout, if body weight is known. */
    val calories: Int? = null
)

/** The "how many reps did you get" box for the exercise that just finished. */
data class RepPrompt(
    val name: String,
    /** Last time's count, shown only as a hint. */
    val lastTime: Int,
    val count: Int = 0,
    /** True once the user has typed a number. Only then is it logged. */
    val touched: Boolean = false
)

object WorkoutStateHolder {
    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state

    fun set(newState: WorkoutState) {
        _state.value = newState
    }

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused

    fun setPaused(value: Boolean) {
        _paused.value = value
    }

    private val _repPrompt = MutableStateFlow<RepPrompt?>(null)
    val repPrompt: StateFlow<RepPrompt?> = _repPrompt

    fun openRepPrompt(name: String, lastTime: Int) {
        _repPrompt.value = RepPrompt(name, lastTime)
    }

    /** A null [value] means the box was cleared, so nothing is logged. */
    fun setRepCount(value: Int?) {
        _repPrompt.update { p -> p?.copy(count = value ?: 0, touched = value != null) }
    }

    fun closeRepPrompt(): RepPrompt? {
        val p = _repPrompt.value
        _repPrompt.value = null
        return p
    }
}
