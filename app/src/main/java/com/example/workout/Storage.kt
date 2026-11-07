package com.example.workout

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class LoggedSet(val name: String, val weight: Double?, val reps: Int? = null)

data class LogEntry(
    val startedAt: Long,
    val dayTitle: String,
    val completed: Boolean,
    val totalExercises: Int,
    val durationSeconds: Int,
    val unit: String,
    val sets: List<LoggedSet>,
    /** Estimated calories burned, or null when no body weight was set. */
    val calories: Int? = null,
    val note: String = ""
) {
    val doneExercises: Int get() = sets.size
}

/** Body details used only for the calorie estimate. Always stored in metric units. */
data class Profile(
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val age: Int? = null,
    /** True for male, false for female, null when not set. Only used to refine the calorie estimate. */
    val male: Boolean? = null
)

/**
 * Calories = MET x body weight (kg) x hours, the standard compendium formula.
 * Height and age do not enter this formula; they are saved with the profile only.
 */
fun estimateCalories(met: Double, durationSeconds: Int, weightKg: Double?): Int? {
    if (weightKg == null || weightKg <= 0.0) return null
    return (met * weightKg * durationSeconds / 3600.0).roundToInt()
}

/**
 * Same MET formula, scaled by the Mifflin-St Jeor resting energy per kilogram when height, age and sex are
 * all known. The scale is held between 0.8 and 1.25 so a typo cannot produce a silly number.
 */
fun estimateCalories(met: Double, durationSeconds: Int, profile: Profile): Int? {
    val weight = profile.weightKg
    val base = estimateCalories(met, durationSeconds, weight) ?: return null
    val height = profile.heightCm
    val age = profile.age
    val male = profile.male
    if (weight == null || height == null || age == null || male == null) return base
    val bmr = 10.0 * weight + 6.25 * height - 5.0 * age + if (male) 5.0 else -161.0
    // A typical adult burns about 1 kcal per kg per hour at rest.
    val scale = (bmr / 24.0 / weight).coerceIn(0.8, 1.25)
    return (base * scale).roundToInt()
}

/** A workout the user put together from the built-in exercises. */
data class CustomWorkout(val id: String, val title: String, val exercises: List<String>)

data class VoiceSettings(
    val halfway: Boolean = true,
    val tenSeconds: Boolean = true,
    val volume: Float = 1f,
    val rate: Float = 1f,
    val vibrate: Boolean = true,
    val beeps: Boolean = false
)

/** [days] is a bit mask, bit 0 = Monday through bit 6 = Sunday. */
data class Reminder(
    val enabled: Boolean = false,
    val hour: Int = 18,
    val minute: Int = 0,
    val days: Int = 0b0011111
)

/** Seconds for the get-ready countdown and for each timed move (warm-up moves keep their own length). */
data class TimingSettings(
    val intro: Int = Timing.INTRO_SECONDS,
    val work: Int = Timing.WORK_SECONDS,
    /** Share of each listed break to keep: 100, 50 or 0 (circuit style). */
    val restPercent: Int = 100
)

/** One body weight reading, in kilograms. */
data class WeightEntry(val at: Long, val kg: Double)

/** Workout history, settings and last-used weights, kept in SharedPreferences. */
object Storage {
    private const val MAX_ENTRIES = 500

    private lateinit var prefs: SharedPreferences
    private var ready = false

    private val _log = MutableStateFlow<List<LogEntry>>(emptyList())
    val log: StateFlow<List<LogEntry>> = _log

    private val _unit = MutableStateFlow("lb")
    val unit: StateFlow<String> = _unit

    private val _profile = MutableStateFlow(Profile())
    val profile: StateFlow<Profile> = _profile

    private val _custom = MutableStateFlow<List<CustomWorkout>>(emptyList())
    val custom: StateFlow<List<CustomWorkout>> = _custom

    private val _voice = MutableStateFlow(VoiceSettings())
    val voice: StateFlow<VoiceSettings> = _voice

    private val _reminder = MutableStateFlow(Reminder())
    val reminder: StateFlow<Reminder> = _reminder

    private val _timing = MutableStateFlow(TimingSettings())
    val timing: StateFlow<TimingSettings> = _timing

    private val _theme = MutableStateFlow("system")
    /** "system", "light" or "dark". */
    val theme: StateFlow<String> = _theme

    /** Newest first. */
    private val _bodyLog = MutableStateFlow<List<WeightEntry>>(emptyList())
    val bodyLog: StateFlow<List<WeightEntry>> = _bodyLog

    @Synchronized
    fun init(context: Context) {
        if (ready) return
        prefs = context.applicationContext.getSharedPreferences("workout", Context.MODE_PRIVATE)
        load()
        ready = true
    }

    /** Reads everything from the saved preferences into the live values. */
    private fun load() {
        _unit.value = prefs.getString("unit", "lb") ?: "lb"
        _log.value = parse(prefs.getString("log", null))
        _profile.value = Profile(
            heightCm = if (prefs.contains("h_cm")) prefs.getFloat("h_cm", 0f).toDouble() else null,
            weightKg = if (prefs.contains("w_kg")) prefs.getFloat("w_kg", 0f).toDouble() else null,
            age = if (prefs.contains("age")) prefs.getInt("age", 0) else null,
            male = if (prefs.contains("sex")) prefs.getBoolean("sex", true) else null
        )
        _custom.value = parseCustom(prefs.getString("custom", null))
        _voice.value = VoiceSettings(
            halfway = prefs.getBoolean("v_half", true),
            tenSeconds = prefs.getBoolean("v_ten", true),
            volume = prefs.getFloat("v_vol", 1f),
            rate = prefs.getFloat("v_rate", 1f),
            vibrate = prefs.getBoolean("v_vib", true),
            beeps = prefs.getBoolean("v_beep", false)
        )
        _reminder.value = Reminder(
            enabled = prefs.getBoolean("r_on", false),
            hour = prefs.getInt("r_h", 18),
            minute = prefs.getInt("r_m", 0),
            days = prefs.getInt("r_days", 0b0011111)
        )
        _timing.value = TimingSettings(
            intro = prefs.getInt("t_intro", Timing.INTRO_SECONDS),
            work = prefs.getInt("t_work", Timing.WORK_SECONDS),
            restPercent = prefs.getInt("t_rest", 100)
        )
        _theme.value = prefs.getString("theme", "system") ?: "system"
        _goal.value = prefs.getInt("goal", 0)
        _runStarted.value = prefs.getLong("run_started", 0L)
        _bodyLog.value = parseBody(prefs.getString("body", null))
    }

    private val _runStarted = MutableStateFlow(0L)
    /** When the current run began, or 0 when none is going. */
    val runStarted: StateFlow<Long> = _runStarted

    fun setRunStarted(value: Long) {
        _runStarted.value = value
        prefs.edit().putLong("run_started", value).apply()
    }

    /** Days of the week meant for running, Monday as bit 0. */
    fun runDays(): Int = prefs.getInt("run_days", 0)

    fun setRunDays(mask: Int) {
        prefs.edit().putInt("run_days", mask).apply()
    }

    private val _goal = MutableStateFlow(0)
    /** Workouts per week the user is aiming for, 0 when off. */
    val goal: StateFlow<Int> = _goal

    fun setGoal(value: Int) {
        _goal.value = value
        prefs.edit().putInt("goal", value).apply()
    }

    fun dayNote(dayId: String): String = prefs.getString("dnote:$dayId", "") ?: ""

    fun setDayNote(dayId: String, note: String) {
        val e = prefs.edit()
        if (note.isBlank()) e.remove("dnote:$dayId") else e.putString("dnote:$dayId", note)
        e.apply()
    }

    fun markBackup() {
        prefs.edit().putLong("last_backup", System.currentTimeMillis()).apply()
    }

    /** Hides the backup reminder for a while. */
    fun snoozeBackupReminder() {
        prefs.edit().putLong("backup_snooze", System.currentTimeMillis()).apply()
    }

    /** True when there is history worth keeping and no backup or snooze in the last 30 days. */
    fun backupDue(now: Long = System.currentTimeMillis()): Boolean {
        val log = _log.value
        if (log.size < 3) return false
        val last = maxOf(
            prefs.getLong("last_backup", 0L),
            prefs.getLong("backup_snooze", 0L),
            log.minOf { it.startedAt }
        )
        return now - last > 30L * 24 * 3600 * 1000
    }

    fun setTheme(value: String) {
        _theme.value = value
        prefs.edit().putString("theme", value).apply()
    }

    /** A workout still in progress, kept so it can be recovered if the app is killed. */
    fun setPending(entry: LogEntry) {
        prefs.edit().putString("pending", serialize(listOf(entry))).apply()
    }

    fun pending(): LogEntry? = parse(prefs.getString("pending", null)).firstOrNull()

    fun clearPending() {
        prefs.edit().remove("pending").apply()
    }

    fun setTiming(timing: TimingSettings) {
        _timing.value = timing
        prefs.edit().putInt("t_intro", timing.intro).putInt("t_work", timing.work).putInt("t_rest", timing.restPercent).apply()
    }

    /** Logs a body weight reading and uses it for the calorie estimate from now on. */
    fun addBodyWeight(kg: Double) {
        saveBody((listOf(WeightEntry(System.currentTimeMillis(), kg)) + _bodyLog.value).take(1000))
        setProfile(_profile.value.copy(weightKg = kg))
    }

    fun deleteBodyWeight(at: Long) {
        val updated = _bodyLog.value.filter { it.at != at }
        saveBody(updated)
        updated.firstOrNull()?.let { setProfile(_profile.value.copy(weightKg = it.kg)) }
    }

    private fun saveBody(list: List<WeightEntry>) {
        _bodyLog.value = list
        val array = JSONArray()
        for (w in list) array.put(JSONObject().put("t", w.at).put("kg", w.kg))
        prefs.edit().putString("body", array.toString()).apply()
    }

    private fun parseBody(raw: String?): List<WeightEntry> {
        if (raw == null) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                WeightEntry(o.getLong("t"), o.getDouble("kg"))
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    /** The history as a spreadsheet file: one row per set, oldest workout first. */
    fun exportCsv(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val out = StringBuilder("date,workout,completed,minutes,calories,exercise,reps,weight,unit,note\n")
        for (e in _log.value.reversed()) {
            val base = listOf(
                dateFormat.format(Date(e.startedAt)),
                e.dayTitle,
                if (e.completed) "yes" else "no",
                ((e.durationSeconds + 30) / 60).toString(),
                e.calories?.toString() ?: ""
            )
            val sets: List<LoggedSet?> = if (e.sets.isEmpty()) listOf(null) else e.sets
            for (s in sets) {
                val weight = s?.weight?.let { w -> if (w % 1.0 == 0.0) w.toInt().toString() else w.toString() } ?: ""
                val row = base + listOf(s?.name ?: "", s?.reps?.toString() ?: "", weight, e.unit, e.note)
                out.append(row.joinToString(",") { csvCell(it) }).append('\n')
            }
        }
        return out.toString()
    }

    private fun csvCell(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    fun setVoice(voice: VoiceSettings) {
        _voice.value = voice
        prefs.edit()
            .putBoolean("v_half", voice.halfway)
            .putBoolean("v_ten", voice.tenSeconds)
            .putFloat("v_vol", voice.volume)
            .putFloat("v_rate", voice.rate)
            .putBoolean("v_vib", voice.vibrate)
            .putBoolean("v_beep", voice.beeps)
            .apply()
    }

    fun setReminder(reminder: Reminder) {
        _reminder.value = reminder
        prefs.edit()
            .putBoolean("r_on", reminder.enabled)
            .putInt("r_h", reminder.hour)
            .putInt("r_m", reminder.minute)
            .putInt("r_days", reminder.days)
            .apply()
    }

    fun saveCustom(workout: CustomWorkout) {
        val list = _custom.value
        val updated = if (list.any { it.id == workout.id }) {
            list.map { if (it.id == workout.id) workout else it }
        } else {
            list + workout
        }
        persistCustom(updated)
    }

    fun deleteCustom(id: String) = persistCustom(_custom.value.filter { it.id != id })

    private fun persistCustom(list: List<CustomWorkout>) {
        _custom.value = list
        val array = JSONArray()
        for (w in list) {
            array.put(
                JSONObject()
                    .put("id", w.id)
                    .put("title", w.title)
                    .put("exercises", JSONArray(w.exercises))
            )
        }
        prefs.edit().putString("custom", array.toString()).apply()
    }

    private fun parseCustom(raw: String?): List<CustomWorkout> {
        if (raw == null) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val names = o.getJSONArray("exercises")
                CustomWorkout(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    exercises = (0 until names.length()).map { names.getString(it) }
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    /** Everything saved on this phone as one JSON document, for backups. */
    fun exportJson(): String {
        val data = JSONObject()
        for ((key, value) in prefs.all) {
            val typed = when (value) {
                is String -> JSONObject().put("t", "s").put("v", value)
                is Boolean -> JSONObject().put("t", "b").put("v", value)
                is Int -> JSONObject().put("t", "i").put("v", value)
                is Long -> JSONObject().put("t", "l").put("v", value)
                is Float -> JSONObject().put("t", "f").put("v", value.toDouble())
                else -> null
            }
            if (typed != null) data.put(key, typed)
        }
        return JSONObject().put("app", "quick-workout").put("version", 1).put("data", data).toString()
    }

    /** Replaces everything on this phone with a backup. Returns false, changing nothing, if it is not valid. */
    @Synchronized
    fun importJson(raw: String): Boolean {
        return try {
            val root = JSONObject(raw)
            if (root.optString("app") != "quick-workout") return false
            val data = root.getJSONObject("data")
            val editor = prefs.edit().clear()
            for (key in data.keys()) {
                val typed = data.getJSONObject(key)
                when (typed.getString("t")) {
                    "s" -> editor.putString(key, typed.getString("v"))
                    "b" -> editor.putBoolean(key, typed.getBoolean("v"))
                    "i" -> editor.putInt(key, typed.getInt("v"))
                    "l" -> editor.putLong(key, typed.getLong("v"))
                    "f" -> editor.putFloat(key, typed.getDouble("v").toFloat())
                }
            }
            editor.commit()
            load()
            true
        } catch (e: JSONException) {
            false
        }
    }

    fun setUnit(unit: String) {
        _unit.value = unit
        prefs.edit().putString("unit", unit).apply()
    }

    fun setProfile(profile: Profile) {
        _profile.value = profile
        val e = prefs.edit()
        if (profile.heightCm == null) e.remove("h_cm") else e.putFloat("h_cm", profile.heightCm.toFloat())
        if (profile.weightKg == null) e.remove("w_kg") else e.putFloat("w_kg", profile.weightKg.toFloat())
        if (profile.age == null) e.remove("age") else e.putInt("age", profile.age)
        if (profile.male == null) e.remove("sex") else e.putBoolean("sex", profile.male)
        e.apply()
    }

    /** The weight last used for this exercise, or null if none has been set. */
    fun weightFor(name: String): Double? {
        val key = "w:$name"
        return if (prefs.contains(key)) prefs.getFloat(key, 0f).toDouble() else null
    }

    fun setWeight(name: String, weight: Double?) {
        val key = "w:$name"
        val editor = prefs.edit()
        if (weight == null) editor.remove(key) else editor.putFloat(key, weight.toFloat())
        editor.apply()
    }

    /** The reps logged for this exercise the last time the user entered any, or null. */
    fun lastReps(name: String): Int? =
        _log.value.asSequence()
            .flatMap { it.sets.asSequence() }
            .firstOrNull { it.name == name && it.reps != null }
            ?.reps

    fun addEntry(entry: LogEntry) = saveLog((listOf(entry) + _log.value).take(MAX_ENTRIES))

    fun updateEntry(entry: LogEntry) =
        saveLog(_log.value.map { if (it.startedAt == entry.startedAt) entry else it })

    fun deleteEntry(startedAt: Long) = saveLog(_log.value.filter { it.startedAt != startedAt })

    private fun saveLog(entries: List<LogEntry>) {
        _log.value = entries
        prefs.edit().putString("log", serialize(entries)).apply()
    }

    private fun serialize(entries: List<LogEntry>): String {
        val array = JSONArray()
        for (e in entries) {
            val sets = JSONArray()
            for (s in e.sets) {
                val o = JSONObject().put("name", s.name)
                if (s.weight != null) o.put("weight", s.weight)
                if (s.reps != null) o.put("reps", s.reps)
                sets.put(o)
            }
            array.put(
                JSONObject()
                    .put("startedAt", e.startedAt)
                    .put("dayTitle", e.dayTitle)
                    .put("completed", e.completed)
                    .put("total", e.totalExercises)
                    .put("duration", e.durationSeconds)
                    .put("unit", e.unit)
                    .put("sets", sets)
                    .apply { if (e.calories != null) put("kcal", e.calories) }
                    .put("note", e.note)
            )
        }
        return array.toString()
    }

    private fun parse(raw: String?): List<LogEntry> {
        if (raw == null) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val sets = o.getJSONArray("sets")
                LogEntry(
                    startedAt = o.getLong("startedAt"),
                    dayTitle = o.getString("dayTitle"),
                    completed = o.getBoolean("completed"),
                    totalExercises = o.getInt("total"),
                    durationSeconds = o.getInt("duration"),
                    unit = o.optString("unit", "lb"),
                    calories = if (o.has("kcal")) o.getInt("kcal") else null,
                    note = o.optString("note", ""),
                    sets = (0 until sets.length()).map { j ->
                        val s = sets.getJSONObject(j)
                        LoggedSet(
                            s.getString("name"),
                            if (s.has("weight")) s.getDouble("weight") else null,
                            if (s.has("reps")) s.getInt("reps") else null
                        )
                    }
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }
}
