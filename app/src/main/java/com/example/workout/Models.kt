package com.example.workout

import java.net.URLEncoder

object Timing {
    const val WORK_SECONDS = 60
    const val INTRO_SECONDS = 10
}

fun formatClock(totalSeconds: Int): String = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

data class Exercise(
    val name: String,
    val group: String,
    val how: String,
    /** Greater than 0 means quantity based: the user taps Done instead of running a timer. */
    val reps: Int = 0,
    /** Fixed length in seconds. 0 means use the "time per move" setting. */
    val seconds: Int = 0,
    /** Voice says "Switch sides" at the halfway mark. */
    val switchSides: Boolean = false,
    /** Spoken at the halfway mark instead of the default, for example "Reverse direction." */
    val halfwayCue: String = "",
    /** Spoken at set points instead of the halfway call: fraction of the time still left, and the text. */
    val cues: List<Pair<Double, String>> = emptyList(),
    /** Shows a weight stepper. The last weight used is remembered and written to the history. */
    val weighted: Boolean = false,
    val videoQuery: String = "$name exercise",
    /** Get-ready countdown in seconds. -1 means use the "get ready" setting. */
    val introSeconds: Int = -1
) {
    val isReps: Boolean get() = reps > 0

    fun intro(setting: Int): Int = if (introSeconds >= 0) introSeconds else setting

    fun workSeconds(setting: Int): Int = if (seconds > 0) seconds else setting

    /**
     * Moments where the user has to change sides or position: pairs of (seconds of work left, spoken text),
     * latest first. Empty when the switch pause is off or the move has nothing to switch.
     */
    fun switchPoints(total: Int, delay: Int): List<Pair<Int, String>> {
        if (delay <= 0 || isReps) return emptyList()
        val raw = when {
            cues.isNotEmpty() -> cues.map { (fraction, text) -> Math.round(fraction * total).toInt() to text }
            switchSides -> listOf(total / 2 to "Switch sides.")
            else -> emptyList()
        }
        return raw.filter { it.first in 1 until total }.distinctBy { it.first }.sortedByDescending { it.first }
    }

    /** Extra seconds the switch pauses add to this move. */
    fun pauseSeconds(timing: TimingSettings): Int =
        switchPoints(workSeconds(timing.work), timing.switchDelay).size * timing.switchDelay

    val videoUrl: String
        get() = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(videoQuery, "UTF-8")
}

sealed class Step {
    data class Work(val exercise: Exercise, val label: String = "") : Step()
    data class Rest(val label: String, val seconds: Int) : Step()
}

data class Day(
    val id: String,
    val title: String,
    val blurb: String,
    val steps: List<Step>,
    /** Rough intensity (MET) used for the calorie estimate. */
    val met: Double = 4.0,
    /** Ask for a rep count after each timed exercise. */
    val trackReps: Boolean = true
) {
    val exerciseCount: Int get() = steps.count { it is Step.Work }

    /** Null when the day is rep based, since the length then depends on the person. */
    fun approxMinutes(timing: TimingSettings): Int? {
        if (steps.any { it is Step.Work && it.exercise.isReps }) return null
        val seconds = steps.sumOf {
            when (it) {
                is Step.Work -> it.exercise.intro(timing.intro) + it.exercise.workSeconds(timing.work) + it.exercise.pauseSeconds(timing)
                is Step.Rest -> it.seconds
            }
        }
        return (seconds + 30) / 60
    }
}
