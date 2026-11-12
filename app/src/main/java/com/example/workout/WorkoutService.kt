package com.example.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

class WorkoutService : Service() {

    companion object {
        const val ACTION_START = "com.example.workout.START"
        const val ACTION_STOP = "com.example.workout.STOP"
        const val ACTION_NEXT = "com.example.workout.NEXT"
        const val ACTION_PAUSE = "com.example.workout.PAUSE"
        const val ACTION_SKIP = "com.example.workout.SKIP"
        const val ACTION_SWAP = "com.example.workout.SWAP"
        const val EXTRA_NAME = "name"
        const val EXTRA_DAY = "day"
        const val EXTRA_RESUME = "resume"
        private const val CHANNEL_ID = "workout"
        private const val NOTIFICATION_ID = 1
        private const val WAKE_LOCK_MS = 90 * 60 * 1000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var workoutJob: Job? = null
    private var repsDone: CompletableDeferred<Unit>? = null
    private var session: Session? = null

    /** Replaces the next move in the running workout. Set from the break screen. */
    @Volatile
    private var swapNext: Exercise? = null

    // Where the running workout is, for the time-left display.
    private var ctxSection = ""
    private var ctxAfter = 0
    private var ctxPlanned = 0
    private var paused = false
    private var skipRequested = false
    private var advanceRequested = false
    /** Set by [countdown] when a "next" tap, not the clock, ended it. */
    private var endedEarly = false
    private var workoutTitle = "Workout"

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    private var toneGenerator: ToneGenerator? = null
    /** Index in the session's sets of the exercise whose rep count is being asked for. */
    private var promptIndex = -1

    private var tts: TextToSpeech? = null
    private val ttsReady = CompletableDeferred<Boolean>()
    @Volatile private var voiceOk = false
    private val speakMutex = Mutex()

    private lateinit var audioManager: AudioManager
    private var focusRequest: AudioFocusRequest? = null

    // Speech is tagged as navigation guidance so music apps duck (or pause) for it,
    // the same way they do for turn-by-turn directions.
    private val speechAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Storage.init(this)
        createChannel()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setAudioAttributes(speechAttributes)
                voiceOk = true
            }
            ttsReady.complete(voiceOk)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resume = if (intent.getBooleanExtra(EXTRA_RESUME, false)) Storage.pending() else null
                val day = if (resume != null) Days.byTitle(resume.dayTitle) else Days.find(intent.getStringExtra(EXTRA_DAY))
                workoutTitle = day?.title ?: "Workout"
                // Must always enter the foreground after startForegroundService.
                startInForeground()
                if (workoutJob?.isActive == true) return START_NOT_STICKY
                if (day == null) {
                    stopWorkout()
                } else {
                    workoutJob = scope.launch { runDay(day, if (day.title == resume?.dayTitle) resume else null) }
                }
            }
            ACTION_NEXT -> {
                if (workoutJob?.isActive == true) {
                    // Finishes a timed set early, ends a get-ready or break early, or confirms a rep set.
                    advanceRequested = true
                    repsDone?.complete(Unit)
                } else {
                    stopSelf()
                }
            }
            ACTION_PAUSE -> if (workoutJob?.isActive == true) togglePause()
            ACTION_SKIP -> if (workoutJob?.isActive == true) {
                skipRequested = true
                repsDone?.complete(Unit)
            }
            ACTION_SWAP -> if (workoutJob?.isActive == true) {
                swapNext = Days.exercisePool.firstOrNull { it.name == intent.getStringExtra(EXTRA_NAME) }
            }
            ACTION_STOP -> stopWorkout()
        }
        return START_NOT_STICKY
    }

    private suspend fun runDay(day: Day, resume: LogEntry? = null) {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "workout:timer")
        wakeLock.acquire(WAKE_LOCK_MS)
        try {
            withTimeoutOrNull(5_000) { ttsReady.await() }
            session = if (resume != null) {
                // Carry on an interrupted workout: keep what was logged and the time already spent.
                Session(day, resume.startedAt, SystemClock.elapsedRealtime() - resume.durationSeconds * 1000L)
                    .also { it.sets.addAll(resume.sets) }
            } else {
                Session(day, System.currentTimeMillis(), SystemClock.elapsedRealtime())
            }
            val skipCount = resume?.sets?.size ?: 0
            var workSeen = 0
            paused = false
            skipRequested = false
            advanceRequested = false
            promptIndex = -1
            swapNext = null
            WorkoutStateHolder.setPaused(false)
            WorkoutStateHolder.closeRepPrompt()

            val steps = day.steps
            val total = day.exerciseCount
            var number = 0

            // Seconds left after each step, so the screen can show time remaining.
            val timingNow = Storage.timing.value
            val lengths = steps.map {
                when (it) {
                    is Step.Work -> it.exercise.intro(timingNow.intro) + it.exercise.workSeconds(timingNow.work) + it.exercise.pauseSeconds(timingNow)
                    is Step.Rest -> it.seconds * timingNow.restPercent / 100
                }
            }
            val suffix = IntArray(steps.size + 1)
            for (i in steps.indices.reversed()) suffix[i] = suffix[i + 1] + lengths[i]
            ctxPlanned = if (steps.any { it is Step.Work && it.exercise.isReps }) 0 else suffix[0]
            ctxSection = ""

            for ((index, step) in steps.withIndex()) {
                ctxAfter = suffix[index + 1]
                // Moves already done before the interruption are passed over, along with the breaks around them.
                if (workSeen < skipCount) {
                    if (step is Step.Work) {
                        workSeen++
                        number++
                    }
                    continue
                }
                if (step is Step.Work) ctxSection = step.label
                when (step) {
                    is Step.Work -> {
                        number++
                        val upcoming = steps.drop(index + 1)
                        val following = upcoming.firstOrNull()
                        val then = if (following is Step.Rest) {
                            upcoming.drop(1).filterIsInstance<Step.Work>().firstOrNull()?.exercise?.name.orEmpty()
                        } else ""
                        val chosen = swapNext ?: step.exercise
                        swapNext = null
                        runExercise(day, chosen, number, total, describe(following), then)
                    }
                    is Step.Rest -> {
                        val next = steps.drop(index + 1)
                            .filterIsInstance<Step.Work>()
                            .firstOrNull()?.exercise
                        val seconds = step.seconds * Storage.timing.value.restPercent / 100
                        if (seconds > 0) runRest(day, step.copy(seconds = seconds), next, number, total)
                    }
                }
            }

            val saved = saveSession()
            val calories = saved?.calories
            val bests = if (saved != null) {
                findBests(saved, Storage.log.value.filter { it.startedAt != saved.startedAt })
            } else {
                emptyList()
            }
            paused = false
            WorkoutStateHolder.setPaused(false)
            WorkoutStateHolder.set(WorkoutState(phase = Phase.DONE, dayTitle = day.title, calories = calories))
            val message = StringBuilder("Workout complete. Nice work.")
            if (calories != null) message.append(" About $calories calories burned.")
            if (bests.isNotEmpty()) {
                message.append(" You set ${bests.size} new personal ${if (bests.size == 1) "best" else "bests"}.")
            }
            speak(message.toString())
            stopForegroundCompat()
            stopSelf()
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private suspend fun runExercise(
        day: Day, ex: Exercise, number: Int, total: Int, next: String, then: String
    ) {
        // Seconds of switch pauses still ahead, counted into the time-left display.
        var pausesAhead = 0
        fun state(phase: Phase, seconds: Int = 0) = WorkoutState(
            phase = phase,
            dayTitle = day.title,
            title = ex.name,
            group = ex.group,
            how = ex.how,
            videoUrl = ex.videoUrl,
            reps = ex.reps,
            weighted = ex.weighted,
            secondsLeft = seconds,
            number = number,
            total = total,
            nextName = next,
            thenName = then,
            section = ctxSection,
            planned = ctxPlanned,
            remaining = when {
                ctxPlanned == 0 -> -1
                phase == Phase.INTRO -> seconds + ex.workSeconds(Storage.timing.value.work) + pausesAhead + ctxAfter
                else -> seconds + pausesAhead + ctxAfter
            }
        )

        val introTiming = Storage.timing.value
        val points = ex.switchPoints(ex.workSeconds(introTiming.work), introTiming.switchDelay)
        pausesAhead = points.size * introTiming.switchDelay
        val intro = if (number == 1) "Starting ${day.title}. First up: ${ex.name}." else "Next up: ${ex.name}."
        speakAsync(intro)
        val timing = Storage.timing.value
        val introSkipped = countdown(ex.intro(timing.intro)) {
            WorkoutStateHolder.set(state(Phase.INTRO, it))
            if (it in 1..3) pulse()
        }
        if (introSkipped) return
        // The previous exercise's rep count is final once the next one starts.
        commitReps()
        pulse(finish = true)

        if (ex.isReps) {
            val done = CompletableDeferred<Unit>()
            repsDone = done
            WorkoutStateHolder.set(state(Phase.REPS))
            speakAsync("${ex.reps} reps. Go.")
            done.await()
            repsDone = null
            if (consumeSkip()) return
        } else {
            speakAsync("Go.")
            val halfwayCue = when {
                ex.halfwayCue.isNotEmpty() -> ex.halfwayCue
                ex.switchSides -> "Switch sides."
                else -> ""
            }
            val total = ex.workSeconds(timing.work)
            val delay = timing.switchDelay
            val ends = points.map { it.first } + 0
            var segStart = total
            for ((i, endLeft) in ends.withIndex()) {
                pausesAhead = (points.size - i) * delay
                val skippedSegment = countdown(segStart - endLeft) { l ->
                    val left = endLeft + l
                    WorkoutStateHolder.set(state(Phase.WORK, left))
                    val voice = Storage.voice.value
                    if (points.isEmpty()) {
                        val cue = ex.cues.firstOrNull { (fraction, _) -> (fraction * total).roundToInt() == left }
                        if (cue != null) {
                            speakAsync(cue.second)
                            pulse()
                        }
                    }
                    if (ex.cues.isEmpty() && points.isEmpty() && left == total / 2) {
                        // Short moves in a circuit would be one long stream of halfway calls.
                        if (voice.halfway && (total >= 40 || halfwayCue.isNotEmpty())) {
                            speakAsync(
                                when {
                                    halfwayCue.isEmpty() -> "Halfway."
                                    total < 40 -> halfwayCue
                                    else -> "Halfway. $halfwayCue"
                                }
                            )
                        }
                        pulse()
                    }
                    if (left == 10 && total > 30 && voice.tenSeconds) speakAsync("Ten seconds.")
                    if (left in 1..3) pulse()
                }
                if (skippedSegment) return
                // "Done early" finishes the whole move, not just this part of it.
                if (endedEarly || i == points.size) break
                pulse(finish = true)
                speakAsync(points[i].second)
                pausesAhead = (points.size - i - 1) * delay
                val skippedPause = countdown(delay) { l ->
                    WorkoutStateHolder.set(state(Phase.SWITCH, l).copy(remaining = if (ctxPlanned == 0) -1 else l + endLeft + pausesAhead + ctxAfter))
                    if (l in 1..3) pulse()
                }
                if (skippedPause) return
                speakAsync("Go.")
                pulse(finish = true)
                segStart = endLeft
            }
            pulse(finish = true)
        }
        // Reached only when the exercise was finished, not skipped or stopped.
        val s = session ?: return
        s.sets.add(LoggedSet(ex.name, if (ex.weighted) Storage.weightFor(ex.name) else null))
        snapshot()
        if (day.trackReps && !ex.isReps) {
            promptIndex = s.sets.size - 1
            WorkoutStateHolder.openRepPrompt(ex.name, Storage.lastReps(ex.name) ?: 0)
        }
    }

    private suspend fun runRest(day: Day, rest: Step.Rest, next: Exercise?, done: Int, total: Int) {
        speakAsync("${rest.label}. ${spokenDuration(rest.seconds)}.")
        countdown(rest.seconds) { left ->
            WorkoutStateHolder.set(
                WorkoutState(
                    phase = Phase.BREAK,
                    dayTitle = day.title,
                    title = rest.label,
                    secondsLeft = left,
                    number = done,
                    total = total,
                    nextName = (swapNext ?: next)?.name.orEmpty(),
                    section = ctxSection,
                    planned = ctxPlanned,
                    remaining = if (ctxPlanned == 0) -1 else left + ctxAfter
                )
            )
        }
    }

    /**
     * Counts down from [seconds] using the real clock, so ticks do not drift over a long workout.
     * Time stops while paused. Returns true if it ended early because of a skip. A "next" tap ends it
     * early too, but counts as finished.
     */
    private suspend fun countdown(seconds: Int, onTick: (Int) -> Unit): Boolean {
        advanceRequested = false
        endedEarly = false
        var remainingMs = seconds * 1000L
        var last = -1
        var mark = SystemClock.elapsedRealtime()
        while (true) {
            val now = SystemClock.elapsedRealtime()
            if (paused) session?.let { it.pausedMs += now - mark } else remainingMs -= now - mark
            mark = now
            if (consumeSkip()) return true
            if (advanceRequested) {
                advanceRequested = false
                endedEarly = true
                return false
            }
            if (remainingMs <= 0) return false
            val left = ((remainingMs + 999) / 1000).toInt()
            if (left != last) {
                last = left
                onTick(left)
            }
            delay(minOf(100L, maxOf(1L, remainingMs - (left - 1) * 1000L)))
        }
    }

    private fun consumeSkip(): Boolean {
        val wasRequested = skipRequested
        skipRequested = false
        return wasRequested
    }

    private fun togglePause() {
        paused = !paused
        WorkoutStateHolder.setPaused(paused)
        updateNotification()
        speakAsync(if (paused) "Paused." else "Resuming.")
    }

    /** A short buzz and/or beep, as chosen in the settings. [finish] makes it longer. */
    private fun pulse(finish: Boolean = false) {
        val voice = Storage.voice.value
        if (voice.vibrate) {
            val effect = VibrationEffect.createOneShot(if (finish) 500L else 150L, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator?.vibrate(effect)
        }
        if (voice.beeps) {
            try {
                val tone = toneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).also { toneGenerator = it }
                tone.startTone(
                    if (finish) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP,
                    if (finish) 400 else 150
                )
            } catch (e: RuntimeException) {
                // No tone available on this device.
            }
        }
    }

    /** Stores the rep count the user entered for the last finished exercise. */
    private fun commitReps() {
        val prompt = WorkoutStateHolder.closeRepPrompt()
        val s = session
        if (prompt != null && prompt.touched && s != null && promptIndex in s.sets.indices) {
            s.sets[promptIndex] = s.sets[promptIndex].copy(reps = prompt.count)
            snapshot()
        }
        promptIndex = -1
    }

    private fun describe(step: Step?): String = when (step) {
        is Step.Work -> step.exercise.name
        is Step.Rest -> "${step.label} (${formatClock(step.seconds)})"
        null -> ""
    }

    private fun spokenDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val parts = mutableListOf<String>()
        if (minutes > 0) parts += "$minutes ${if (minutes == 1) "minute" else "minutes"}"
        if (seconds > 0) parts += "$seconds ${if (seconds == 1) "second" else "seconds"}"
        return parts.joinToString(" ")
    }

    private fun speakAsync(text: String) {
        scope.launch { speak(text) }
    }

    /** Speaks [text] while holding transient audio focus that lets other audio duck or pause. */
    private suspend fun speak(text: String) {
        if (!voiceOk) return
        speakMutex.withLock {
            val engine = tts ?: return@withLock
            val id = UUID.randomUUID().toString()
            val finished = CompletableDeferred<Unit>()

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == id) finished.complete(Unit)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == id) finished.complete(Unit)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (utteranceId == id) finished.complete(Unit)
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    if (utteranceId == id) finished.complete(Unit)
                }
            })

            requestFocus()
            try {
                val voice = Storage.voice.value
                engine.setSpeechRate(voice.rate)
                val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voice.volume) }
                if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, id) != TextToSpeech.SUCCESS) {
                    return@withLock
                }
                withTimeoutOrNull(20_000) { finished.await() }
            } finally {
                abandonFocus()
            }
        }
    }

    private fun requestFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(speechAttributes)
            .setOnAudioFocusChangeListener { }
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    /** Keeps a copy of the unfinished workout so it can be recovered if the app is killed. */
    private fun snapshot() {
        val s = session ?: return
        if (s.sets.isEmpty()) return
        val seconds = ((SystemClock.elapsedRealtime() - s.startElapsed - s.pausedMs) / 1000).toInt()
        Storage.setPending(
            LogEntry(
                startedAt = s.startedAt,
                dayTitle = s.day.title,
                completed = false,
                totalExercises = s.day.exerciseCount,
                durationSeconds = seconds,
                unit = Storage.unit.value,
                sets = s.sets.toList(),
                calories = estimateCalories(s.day.met, seconds, Storage.profile.value)
            )
        )
    }

    /** Writes the current workout to the history. Workouts with nothing finished are not logged. */
    private fun saveSession(): LogEntry? {
        commitReps()
        val s = session ?: return null
        session = null
        Storage.clearPending()
        if (s.sets.isEmpty()) return null
        val seconds = ((SystemClock.elapsedRealtime() - s.startElapsed - s.pausedMs) / 1000).toInt()
        val calories = estimateCalories(s.day.met, seconds, Storage.profile.value)
        val entry = LogEntry(
            startedAt = s.startedAt,
            dayTitle = s.day.title,
            completed = s.sets.size >= s.day.exerciseCount,
            totalExercises = s.day.exerciseCount,
            durationSeconds = seconds,
            unit = Storage.unit.value,
            sets = s.sets.toList(),
            calories = calories
        )
        Storage.addEntry(entry)
        WorkoutWidget.update(applicationContext)
        return entry
    }

    private fun stopWorkout() {
        saveSession()
        paused = false
        WorkoutStateHolder.setPaused(false)
        // Cancels the workout and every queued voice line.
        scope.coroutineContext.cancelChildren()
        workoutJob = null
        repsDone = null
        tts?.stop()
        abandonFocus()
        WorkoutStateHolder.set(WorkoutState())
        stopForegroundCompat()
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        fun action(code: Int, name: String) = PendingIntent.getService(
            this, code,
            Intent(this, WorkoutService::class.java).setAction(name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Workout in progress")
            .setContentText(if (paused) "$workoutTitle (paused)" else workoutTitle)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, if (paused) "Resume" else "Pause", action(2, ACTION_PAUSE))
            .addAction(android.R.drawable.ic_media_next, "Skip", action(3, ACTION_SKIP))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", action(1, ACTION_STOP))
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    private fun startInForeground() {
        val notification = buildNotification()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        )
    }

    private fun stopForegroundCompat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Workout", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onDestroy() {
        scope.cancel()
        toneGenerator?.release()
        tts?.shutdown()
        abandonFocus()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

private class Session(val day: Day, val startedAt: Long, val startElapsed: Long) {
    val sets = mutableListOf<LoggedSet>()
    var pausedMs = 0L
}
