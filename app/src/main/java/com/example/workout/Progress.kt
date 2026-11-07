package com.example.workout

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val WARM_UP_TITLE = "Warm-Up"
private const val KG_TO_LB = 2.2046226

data class Summary(val last7Days: Int, val streakDays: Int, val total: Int, val calories: Int)

data class ExerciseProgress(
    val name: String,
    val times: Int,
    val bestReps: Int?,
    val lastReps: Int?,
    val bestWeightKg: Double?,
    val lastWeightKg: Double?,
    /** Best reps in each workout, oldest first. */
    val repsSeries: List<Int> = emptyList(),
    /** Heaviest weight in each workout in kilograms, oldest first. */
    val weightSeries: List<Double> = emptyList()
)

private fun dayOf(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

/** Workout counts and streak. Warm-ups do not count as workouts here. */
fun summarize(entries: List<LogEntry>, today: LocalDate = LocalDate.now()): Summary {
    val workouts = entries.filter { it.dayTitle != WARM_UP_TITLE }
    val days = workouts.map { dayOf(it.startedAt) }.toSet()
    val weekStart = today.minusDays(6)
    val last7 = workouts.count { !dayOf(it.startedAt).isBefore(weekStart) }

    // A streak survives until the end of today, so it still counts if you have not trained yet today.
    var cursor = if (today in days) today else today.minusDays(1)
    var streak = 0
    while (cursor in days) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return Summary(last7, streak, workouts.size, entries.sumOf { it.calories ?: 0 })
}

private fun toKg(weight: Double, unit: String): Double = if (unit == "lb") weight / KG_TO_LB else weight

private fun shownWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toInt().toString() else "%.1f".format(weight)

/** Per-exercise best and latest numbers, plus one value per workout for charts. */
fun exerciseProgress(entries: List<LogEntry>): List<ExerciseProgress> {
    val reps = LinkedHashMap<String, MutableList<Int>>()
    val kilos = LinkedHashMap<String, MutableList<Double>>()
    val counts = LinkedHashMap<String, Int>()
    // Entries are newest first, so walk them backwards to get oldest first.
    for (entry in entries.reversed()) {
        for ((name, sets) in entry.sets.groupBy { it.name }) {
            counts[name] = (counts[name] ?: 0) + sets.size
            sets.mapNotNull { it.reps }.maxOrNull()?.let { reps.getOrPut(name) { mutableListOf() }.add(it) }
            sets.mapNotNull { set -> set.weight?.let { toKg(it, entry.unit) } }.maxOrNull()
                ?.let { kilos.getOrPut(name) { mutableListOf() }.add(it) }
        }
    }
    return counts.keys.mapNotNull { name ->
        val r = reps[name].orEmpty()
        val k = kilos[name].orEmpty()
        if (r.isEmpty() && k.isEmpty()) null
        else ExerciseProgress(
            name = name,
            times = counts.getValue(name),
            bestReps = r.maxOrNull(),
            lastReps = r.lastOrNull(),
            bestWeightKg = k.maxOrNull(),
            lastWeightKg = k.lastOrNull(),
            repsSeries = r,
            weightSeries = k
        )
    }.sortedByDescending { it.times }
}

/**
 * Moves where this workout beat everything logged before it. A move with nothing logged before it
 * does not count, so the first workout never reports bests.
 */
fun findBests(entry: LogEntry, previous: List<LogEntry>): List<String> {
    val result = mutableListOf<String>()
    for ((name, sets) in entry.sets.groupBy { it.name }) {
        val prior = previous.flatMap { e -> e.sets.filter { it.name == name }.map { it to e.unit } }
        val priorReps = prior.mapNotNull { it.first.reps }.maxOrNull()
        val priorKg = prior.mapNotNull { (set, unit) -> set.weight?.let { toKg(it, unit) } }.maxOrNull()
        val reps = sets.mapNotNull { it.reps }.maxOrNull()
        val heaviest = sets.mapNotNull { it.weight }.maxOrNull()
        val parts = mutableListOf<String>()
        if (reps != null && priorReps != null && reps > priorReps) parts += "$reps reps"
        if (heaviest != null && priorKg != null && toKg(heaviest, entry.unit) > priorKg + 0.01) {
            parts += "${shownWeight(heaviest)} ${entry.unit}"
        }
        if (parts.isNotEmpty()) result += "$name: ${parts.joinToString(", ")}"
    }
    return result
}

/**
 * The main workout done least recently. One that was never done comes first, and ties keep the
 * built-in order. Entries are newest first, so a larger position means longer ago.
 */
fun suggestNext(entries: List<LogEntry>): Day {
    val cycle = Days.all.filter { it.id != "warm-up" }
    return cycle.maxByOrNull { day ->
        val position = entries.indexOfFirst { it.dayTitle == day.title }
        if (position < 0) Int.MAX_VALUE else position
    } ?: cycle.first()
}

/** One finished session of a move: its best reps and the weight used, with the unit it was logged in. */
private class PastSet(val reps: Int?, val weight: Double?, val unit: String)

private fun pastSets(name: String, entries: List<LogEntry>): List<PastSet> =
    entries.mapNotNull { entry ->
        val sets = entry.sets.filter { it.name == name }
        if (sets.isEmpty()) return@mapNotNull null
        val reps = sets.mapNotNull { it.reps }.maxOrNull()
        val weight = sets.mapNotNull { it.weight }.maxOrNull()
        if (reps == null && weight == null) null else PastSet(reps, weight, entry.unit)
    }

/** "Last time: 22 reps at 15 lb", or null when the move has never been logged. */
fun lastTimeLine(name: String, entries: List<LogEntry>): String? {
    val last = pastSets(name, entries).firstOrNull() ?: return null
    val parts = listOfNotNull(last.reps?.let { "$it reps" }, last.weight?.let { "${shownWeight(it)} ${last.unit}" })
    return "Last time: ${parts.joinToString(" at ")}"
}

/**
 * A heavier weight to try, in [unit], once the reps rose in each of the last three sessions at the same
 * weight. Null when there is no such streak.
 */
fun weightSuggestion(name: String, entries: List<LogEntry>, unit: String): Double? {
    val recent = pastSets(name, entries).filter { it.reps != null && it.weight != null }.take(3)
    if (recent.size < 3) return null
    val kilos = recent.map { toKg(it.weight!!, it.unit) }
    if (kilos.any { Math.abs(it - kilos[0]) > 0.05 }) return null
    val (newest, middle, oldest) = recent.map { it.reps!! }
    if (!(newest > middle && middle > oldest)) return null
    val step = if (unit == "kg") 2.5 else 5.0
    val current = if (unit == "lb") kilos[0] * KG_TO_LB else kilos[0]
    return Math.round(current / step) * step + step
}

/** Workouts finished in each of the last [weeks] weeks (oldest first) with minutes spent. */
fun weeklyTotals(entries: List<LogEntry>, weeks: Int = 8, today: LocalDate = LocalDate.now()): List<Pair<Int, Int>> {
    val start = today.minusDays(today.dayOfWeek.value - 1L)
    return (weeks - 1 downTo 0).map { back ->
        val from = start.minusWeeks(back.toLong())
        val to = from.plusDays(6)
        val inWeek = entries.filter {
            it.dayTitle != WARM_UP_TITLE && dayOf(it.startedAt).let { d -> !d.isBefore(from) && !d.isAfter(to) }
        }
        inWeek.size to inWeek.sumOf { it.durationSeconds } / 60
    }
}
