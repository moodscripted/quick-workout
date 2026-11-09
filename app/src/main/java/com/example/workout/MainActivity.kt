package com.example.workout

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    /** A workout the home screen widget asked to open. */
    private var openDayId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Storage.init(this)
        openDayId = intent?.getStringExtra(WorkoutWidget.EXTRA_OPEN_DAY)
        enableEdgeToEdge()
        setContent {
            WorkoutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecoveryDialogHost()
                    WorkoutScreen(
                        openDayId = openDayId,
                        onOpenDayHandled = {
                            openDayId = null
                            intent?.removeExtra(WorkoutWidget.EXTRA_OPEN_DAY)
                        },
                        onStart = { day ->
                            ContextCompat.startForegroundService(
                                this,
                                Intent(this, WorkoutService::class.java)
                                    .setAction(WorkoutService.ACTION_START)
                                    .putExtra(WorkoutService.EXTRA_DAY, day.id)
                            )
                        },
                        onStop = {
                            startService(
                                Intent(this, WorkoutService::class.java)
                                    .setAction(WorkoutService.ACTION_STOP)
                            )
                        },
                        onRepsDone = {
                            startService(
                                Intent(this, WorkoutService::class.java)
                                    .setAction(WorkoutService.ACTION_NEXT)
                            )
                        },
                        onPause = {
                            startService(
                                Intent(this, WorkoutService::class.java)
                                    .setAction(WorkoutService.ACTION_PAUSE)
                            )
                        },
                        onSkip = {
                            startService(
                                Intent(this, WorkoutService::class.java)
                                    .setAction(WorkoutService.ACTION_SKIP)
                            )
                        },
                        onVideo = { url ->
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: ActivityNotFoundException) {
                                // No browser installed; nothing to do.
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openDayId = intent.getStringExtra(WorkoutWidget.EXTRA_OPEN_DAY)
    }
}

@Composable
fun WorkoutScreen(
    openDayId: String?,
    onOpenDayHandled: () -> Unit,
    onStart: (Day) -> Unit,
    onStop: () -> Unit,
    onRepsDone: () -> Unit,
    onPause: () -> Unit,
    onSkip: () -> Unit,
    onVideo: (String) -> Unit
) {
    val state by WorkoutStateHolder.state.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val keepScreenSetting by Storage.keepScreen.collectAsState()
    var showLog by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showProgress by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showBuilder by rememberSaveable { mutableStateOf(false) }
    var builderEditId by rememberSaveable { mutableStateOf<String?>(null) }
    var builderCopyId by rememberSaveable { mutableStateOf<String?>(null) }
    val custom by Storage.custom.collectAsState()
    val entries by Storage.log.collectAsState()
    val days = remember(custom) { Days.all + custom.map { Days.fromCustom(it) } }
    val suggested = remember(entries) { suggestNext(entries) }
    val selectedDay = remember(selectedId, custom) { Days.find(selectedId) }
    var logOpenId by rememberSaveable { mutableStateOf<Long?>(null) }
    val bests = remember(entries, state.phase) {
        if (state.phase == Phase.DONE) {
            entries.firstOrNull()?.let { findBests(it, entries.drop(1)) } ?: emptyList()
        } else {
            emptyList()
        }
    }

    LaunchedEffect(openDayId) {
        val id = openDayId
        if (id != null) {
            showLog = false
            showSettings = false
            showProgress = false
            showBuilder = false
            selectedId = id
            onOpenDayHandled()
        }
    }

    val active = state.phase != Phase.IDLE && state.phase != Phase.DONE
    DisposableEffect(active, keepScreenSetting) {
        view.keepScreenOn = active && keepScreenSetting
        onDispose { view.keepScreenOn = false }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The workout runs either way; only the notification is hidden if denied. */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        if (active) {
            ActiveWorkout(state, onStop, onRepsDone, onPause, onSkip, onVideo)
        } else if (showBuilder) {
            WorkoutBuilder(
                existing = builderEditId?.let { id -> custom.firstOrNull { it.id == id } },
                copyFrom = builderCopyId?.let { Days.find(it) },
                onClose = {
                    showBuilder = false
                    builderEditId = null
                    builderCopyId = null
                }
            )
        } else if (showProgress) {
            ProgressScreen(onBack = { showProgress = false })
        } else if (showSettings) {
            SettingsScreen(onBack = { showSettings = false })
        } else if (showLog) {
            LogScreen(initialOpenId = logOpenId, onBack = { showLog = false; logOpenId = null })
        } else if (selectedDay != null) {
            DayDetail(
                day = selectedDay,
                isCustom = Days.isCustom(selectedDay.id),
                onVideo = onVideo,
                onBack = { selectedId = null },
                onEdit = {
                    if (Days.isCustom(selectedDay.id)) {
                        builderEditId = selectedDay.id.removePrefix("custom-")
                    } else {
                        builderCopyId = selectedDay.id
                    }
                    showBuilder = true
                    selectedId = null
                },
                onDelete = {
                    Storage.deleteCustom(selectedDay.id.removePrefix("custom-"))
                    selectedId = null
                },
                onStart = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    selectedId = null
                    onStart(selectedDay)
                }
            )
        } else {
            DayList(
                days = days,
                suggested = suggested,
                entries = entries,
                bests = bests,
                onReview = {
                    logOpenId = entries.firstOrNull()?.startedAt
                    showLog = true
                },
                finished = state.phase == Phase.DONE,
                calories = state.calories,
                onNew = {
                    builderEditId = null
                    builderCopyId = null
                    showBuilder = true
                },
                onOpenLog = { showLog = true },
                onOpenSettings = { showSettings = true },
                onOpenProgress = { showProgress = true },
                onPick = { day -> selectedId = day.id }
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Small building blocks
// ---------------------------------------------------------------------------------------------

@Composable
internal fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val large by Storage.largeControls.collectAsState()
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        modifier = if (large) Modifier.heightIn(min = 54.dp) else Modifier,
        contentPadding = PaddingValues(horizontal = if (large) 36.dp else 28.dp, vertical = if (large) 12.dp else 8.dp)
    ) { Text(text, fontSize = if (large) 19.sp else 15.sp) }
}

@Composable
internal fun QuietButton(text: String, onClick: () -> Unit) {
    val large by Storage.largeControls.collectAsState()
    TextButton(
        onClick = onClick,
        modifier = if (large) Modifier.heightIn(min = 48.dp) else Modifier,
        contentPadding = PaddingValues(horizontal = if (large) 12.dp else 8.dp, vertical = if (large) 8.dp else 4.dp)
    ) {
        Text(text, fontSize = if (large) 17.sp else 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun StepperButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp)
    ) { Text(label, fontSize = 16.sp) }
}

internal fun summaryOf(day: Day): String {
    day.approxMinutes(Storage.timing.value)?.let { return "${day.exerciseCount} exercises, about $it min" }
    val reps = day.steps.filterIsInstance<Step.Work>().map { it.exercise.reps }.distinct()
    val allReps = day.steps.filterIsInstance<Step.Work>().all { it.exercise.isReps }
    return if (allReps && reps.size == 1) "${day.exerciseCount} exercises, ${reps[0]} reps each"
    else "${day.exerciseCount} exercises"
}

// ---------------------------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------------------------

// ---------------------------------------------------------------------------------------------
// Workout preview
// ---------------------------------------------------------------------------------------------

/** Preview of one workout. Nothing starts until the Start button is pressed. */
@Composable
private fun DayDetail(
    day: Day,
    isCustom: Boolean,
    onVideo: (String) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit
) {
    BackHandler(onBack = onBack)
    var confirmDelete by remember { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var expanded by rememberSaveable { mutableStateOf<Int?>(null) }
    val lines = remember(day) {
        var number = 0
        day.steps.map { step ->
            when (step) {
                is Step.Work -> {
                    number++
                    val extra = if (step.exercise.isReps) " (${step.exercise.reps} reps)" else ""
                    "$number. ${step.exercise.name}$extra" to step.exercise
                }
                is Step.Rest -> "${step.label}: ${formatTime(step.seconds)}" to null
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this workout?") },
            text = { Text(day.title) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuietButton("Back", onBack)
            Row {
                if (isCustom) {
                    val shareContext = LocalContext.current
                    QuietButton("Share") {
                        Storage.custom.value.firstOrNull { "custom-${it.id}" == day.id }?.let {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${it.title}\n${Storage.customToText(it)}")
                            }
                            shareContext.startActivity(Intent.createChooser(send, "Share workout"))
                        }
                    }
                    QuietButton("Edit", onEdit)
                    QuietButton("Delete") { confirmDelete = true }
                } else {
                    QuietButton("Copy to edit", onEdit)
                }
            }
        }
        Text(day.title, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        Text(day.blurb, fontSize = 13.sp, color = muted, modifier = Modifier.padding(top = 4.dp))
        Text(summaryOf(day), fontSize = 13.sp, color = muted, modifier = Modifier.padding(top = 2.dp))
        var note by remember(day.id) { mutableStateOf(Storage.dayNote(day.id)) }
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(200); Storage.setDayNote(day.id, note) },
            label = { Text("Your note for this workout", fontSize = 13.sp) },
            maxLines = 2,
            textStyle = TextStyle(fontSize = 14.sp),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Text(
            "Tap an exercise to see how it is done.",
            fontSize = 12.sp,
            color = muted,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            itemsIndexed(lines) { index, (text, exercise) ->
                val open = expanded == index
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (exercise != null) Modifier.clickable { expanded = if (open) null else index }
                            else Modifier
                        )
                        .padding(vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text,
                            fontSize = 15.sp,
                            color = if (exercise == null) muted else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (exercise != null) {
                            Text(if (open) "Hide" else "How to", fontSize = 13.sp, color = muted)
                        }
                    }
                    if (open && exercise != null) {
                        Text(exercise.how, fontSize = 14.sp, color = muted, modifier = Modifier.padding(top = 6.dp))
                        if (exercise.weighted) {
                            Text(
                                "Uses dumbbells. Set the weight on the exercise screen.",
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        QuietButton("Watch how it is done") { onVideo(exercise.videoUrl) }
                    }
                }
                HorizontalDivider()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center
        ) { PrimaryButton("Start workout", onStart) }
    }
}

// ---------------------------------------------------------------------------------------------
// Workout in progress
// ---------------------------------------------------------------------------------------------

@Composable
private fun ActiveWorkout(
    state: WorkoutState,
    onStop: () -> Unit,
    onRepsDone: () -> Unit,
    onPause: () -> Unit,
    onSkip: () -> Unit,
    onVideo: (String) -> Unit
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val paused by WorkoutStateHolder.paused.collectAsState()
    val prompt by WorkoutStateHolder.repPrompt.collectAsState()
    val onBreak = state.phase == Phase.BREAK
    val finishedCount = if (onBreak) state.number else (state.number - 1).coerceAtLeast(0)
    val accent = if (state.phase == Phase.WORK) MaterialTheme.colorScheme.primary else muted
    val label = when (state.phase) {
        Phase.INTRO -> "NEXT UP"
        Phase.WORK -> "GO"
        Phase.REPS -> "REPS"
        else -> state.title.uppercase()
    }
    val progressText = if (onBreak) "$finishedCount of ${state.total} done"
    else "${state.number} of ${state.total}"

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${state.dayTitle}  ·  $progressText", fontSize = 13.sp, color = muted)
        LinearProgressIndicator(
            progress = { if (state.total == 0) 0f else finishedCount.toFloat() / state.total },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(2.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (paused) "PAUSED" else label, color = accent, fontSize = 12.sp, letterSpacing = 2.sp)
            if (state.section.isNotEmpty()) {
                Text(state.section, fontSize = 13.sp, color = muted, modifier = Modifier.padding(top = 2.dp))
            }
            if (state.planned > 0 && state.remaining >= 0) {
                Text(
                    "About ${(state.remaining + 59) / 60} min left",
                    fontSize = 12.sp,
                    color = muted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (onBreak) {
                Text(
                    formatTime(state.secondsLeft),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                if (state.nextName.isNotEmpty()) {
                    Text("Next up: ${state.nextName}", fontSize = 17.sp, textAlign = TextAlign.Center)
                    SwapButton(state.nextName)
                }
            } else {
                Text(
                    state.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (state.phase == Phase.REPS) {
                    Text(state.reps.toString(), fontSize = 80.sp, fontWeight = FontWeight.Light)
                    Text("reps", fontSize = 14.sp, color = muted)
                } else {
                    Text(
                        state.secondsLeft.toString(),
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                val history by Storage.log.collectAsState()
                lastTimeLine(state.title, history)?.let {
                    Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                }
                if (state.weighted) {
                    WeightRow(exerciseName = state.title, exerciseNumber = state.number)
                }
                Text(
                    state.how,
                    fontSize = 14.sp,
                    color = muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
                QuietButton("How it is done") { onVideo(state.videoUrl) }
            }
        }

        val repsPrompt = prompt
        if (repsPrompt != null && (state.phase == Phase.INTRO || onBreak)) {
            RepsPrompt(repsPrompt)
        }

        if (!onBreak && state.phase != Phase.INTRO) {
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Text("NEXT", fontSize = 11.sp, letterSpacing = 2.sp, color = muted)
                Text(
                    if (state.nextName.isEmpty()) "Last exercise. Finish strong." else state.nextName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (state.thenName.isNotEmpty()) {
                    Text("Then: ${state.thenName}", fontSize = 13.sp, color = muted)
                }
            }
        }

        if (paused) {
            PrimaryButton("Resume", onPause)
        } else if (state.phase == Phase.REPS) {
            PrimaryButton("Done", onRepsDone)
        } else if (state.phase == Phase.WORK) {
            PrimaryButton("Done early", onRepsDone)
        } else if (state.phase == Phase.INTRO) {
            QuietButton("Start now", onRepsDone)
        } else if (onBreak) {
            QuietButton("End break", onRepsDone)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!paused && state.phase != Phase.REPS) QuietButton("Pause", onPause) else Spacer(Modifier.size(1.dp))
            QuietButton("Skip", onSkip)
            QuietButton("Stop workout", onStop)
        }
    }
}

/** Rep count for the exercise that just finished. Only saved once the user taps + or -. */
@Composable
private fun RepsPrompt(prompt: RepPrompt) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    HorizontalDivider()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Reps you got on ${prompt.name}",
            fontSize = 12.sp,
            color = muted,
            textAlign = TextAlign.Center
        )
        var text by remember(prompt.name) {
            mutableStateOf(if (prompt.touched) prompt.count.toString() else "")
        }
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it.filter { c -> c.isDigit() }.take(3)
                WorkoutStateHolder.setRepCount(text.toIntOrNull())
            },
            placeholder = {
                if (prompt.lastTime > 0) Text("Last: ${prompt.lastTime}", fontSize = 14.sp)
            },
            singleLine = true,
            textStyle = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .width(120.dp)
                .padding(top = 6.dp)
        )
    }
}

/** Weight stepper. Changes are saved at once, so the workout log picks up whatever is showing. */
@Composable
private fun WeightRow(exerciseName: String, exerciseNumber: Int) {
    val unit by Storage.unit.collectAsState()
    var weight by remember(exerciseName, exerciseNumber) { mutableStateOf(Storage.weightFor(exerciseName)) }
    val step = if (unit == "kg") 2.5 else 5.0

    val history by Storage.log.collectAsState()
    val suggestion = remember(history, exerciseName, unit) { weightSuggestion(exerciseName, history, unit) }
    if (suggestion != null && (weight ?: 0.0) < suggestion) {
        Text(
            "Reps went up three sessions running. Try ${formatWeight(suggestion)} $unit.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
    Text(
        "Weight, each dumbbell",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        StepperButton("-") {
            val current = weight
            if (current != null) {
                val next = current - step
                val value = if (next > 0.0) next else null
                weight = value
                Storage.setWeight(exerciseName, value)
            }
        }
        Text(
            weight?.let { "${formatWeight(it)} $unit" } ?: "Set weight",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        StepperButton("+") {
            val value = (weight ?: 0.0) + step
            weight = value
            Storage.setWeight(exerciseName, value)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// History
// ---------------------------------------------------------------------------------------------

@Composable
private fun LogScreen(initialOpenId: Long?, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val entries by Storage.log.collectAsState()
    var openId by rememberSaveable { mutableStateOf(initialOpenId) }

    Column(modifier = Modifier.fillMaxSize()) {
        QuietButton("Back", onBack)
        Text("History", fontSize = 22.sp, fontWeight = FontWeight.Medium)
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
        if (entries.isEmpty()) {
            Text(
                "Nothing logged yet. Finish or stop a workout and it shows up here. Tap an entry to edit or delete it.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            LazyColumn {
                items(entries, key = { it.startedAt }) { entry ->
                    LogRow(
                        entry = entry,
                        open = openId == entry.startedAt,
                        onToggle = { openId = if (openId == entry.startedAt) null else entry.startedAt }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry, open: Boolean, onToggle: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var confirmDelete by remember { mutableStateOf(false) }
    val minutes = ((entry.durationSeconds + 30) / 60).coerceAtLeast(1)

    fun edit(index: Int, change: (LoggedSet) -> LoggedSet) {
        Storage.updateEntry(
            entry.copy(sets = entry.sets.mapIndexed { i, s -> if (i == index) change(s) else s })
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("${entry.dayTitle}, ${formatDate(entry.startedAt)}") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    Storage.deleteEntry(entry.startedAt)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp)
    ) {
        Text(entry.dayTitle, fontSize = 16.sp)
        Text(formatDate(entry.startedAt), fontSize = 13.sp, color = muted)
        Text(
            (if (entry.totalExercises == 0) "$minutes min" + (if (entry.distanceM > 0) ", " + formatDistance(entry.distanceM.toDouble(), entry.unit) else "") else
                "${if (entry.completed) "Completed" else "Partial"}, " +
                    "${entry.doneExercises} of ${entry.totalExercises} exercises, $minutes min") +
                (entry.calories?.let { ", about $it cal" } ?: ""),
            fontSize = 13.sp,
            color = muted
        )
        if (!open) {
            if (entry.note.isNotBlank()) {
                Text(entry.note, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = muted)
            }
            // Reps and weight together, one line per set.
            entry.sets.forEach { set ->
                val parts = listOfNotNull(
                    set.reps?.let { "$it reps" },
                    set.weight?.let { "${formatWeight(it)} ${entry.unit}" }
                )
                if (parts.isNotEmpty()) {
                    Text("${set.name}: ${parts.joinToString(" at ")}", fontSize = 13.sp, color = muted)
                }
            }
        } else {
            entry.sets.forEachIndexed { index, set ->
                val weighted = set.weight != null ||
                    Days.exercisePool.firstOrNull { it.name == set.name }?.weighted == true
                Text(set.name, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reps", fontSize = 12.sp, color = muted, modifier = Modifier.width(80.dp))
                    var repsText by remember(entry.startedAt, index) {
                        mutableStateOf(set.reps?.toString() ?: "")
                    }
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = {
                            repsText = it.filter { c -> c.isDigit() }.take(3)
                            val typed = repsText.toIntOrNull()
                            edit(index) { s -> s.copy(reps = typed) }
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(90.dp)
                    )
                }
                if (weighted) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Weight (${entry.unit})", fontSize = 12.sp, color = muted, modifier = Modifier.width(80.dp))
                        var weightText by remember(entry.startedAt, index) {
                            mutableStateOf(set.weight?.let { formatWeight(it) } ?: "")
                        }
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = {
                                weightText = it.filter { c -> c.isDigit() || c == '.' }.take(6)
                                val typed = weightText.toDoubleOrNull()?.takeIf { w -> w > 0.0 }
                                edit(index) { s -> s.copy(weight = typed) }
                            },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 15.sp, textAlign = TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(90.dp)
                        )
                    }
                }
            }
            var noteText by remember(entry.startedAt) { mutableStateOf(entry.note) }
            OutlinedTextField(
                value = noteText,
                onValueChange = {
                    noteText = it.take(200)
                    Storage.updateEntry(entry.copy(note = noteText))
                },
                label = { Text("Note", fontSize = 13.sp) },
                maxLines = 3,
                textStyle = TextStyle(fontSize = 15.sp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
            val shareContext = LocalContext.current
            Row {
                QuietButton("Share") { shareEntry(shareContext, entry) }
                QuietButton("Delete workout") { confirmDelete = true }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Progress
// ---------------------------------------------------------------------------------------------

@Composable
private fun ProgressScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val entries by Storage.log.collectAsState()
    val unit by Storage.unit.collectAsState()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val summary = remember(entries) { summarize(entries) }
    val exercises = remember(entries) { exerciseProgress(entries) }

    fun weightText(kg: Double): String {
        val value = if (unit == "lb") kg * LB_PER_KG else kg
        return "${formatWeight(Math.round(value * 10) / 10.0)} $unit"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        QuietButton("Back", onBack)
        Text("Progress", fontSize = 22.sp, fontWeight = FontWeight.Medium)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Stat(summary.last7Days.toString(), "last 7 days")
                    Stat(summary.streakDays.toString(), "day streak")
                    Stat(summary.total.toString(), "workouts")
                }
            }
            if (summary.calories > 0) {
                item {
                    Text(
                        "About ${summary.calories} calories burned in total",
                        fontSize = 12.sp,
                        color = muted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            item { HorizontalDivider() }
            run {
                val meters = runMetersThisWeek(entries)
                if (meters > 0) {
                    item {
                        Text(
                            "Run distance this week: ${formatDistance(meters.toDouble(), unit)}",
                            fontSize = 12.sp,
                            color = muted,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            item { WeeklyChart(entries) }
            item { HorizontalDivider() }
            item { MonthCalendar(entries) }
            item { HorizontalDivider() }
            item { BodyWeightSection(unit) }
            item { HorizontalDivider() }
            if (exercises.isEmpty()) {
                item {
                    Text(
                        "Rep counts and weights show up here once you log them during a workout.",
                        fontSize = 14.sp,
                        color = muted,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } else {
                items(exercises, key = { it.name }) { progress ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(progress.name, fontSize = 15.sp)
                        if (progress.bestReps != null) {
                            Text(
                                "best ${progress.bestReps} reps, last ${progress.lastReps}",
                                fontSize = 13.sp,
                                color = muted
                            )
                            if (progress.repsSeries.size >= 2) {
                                Sparkline(progress.repsSeries.map { it.toDouble() })
                            }
                        }
                        if (progress.bestWeightKg != null && progress.lastWeightKg != null) {
                            Text(
                                "best ${weightText(progress.bestWeightKg)}, last ${weightText(progress.lastWeightKg)}",
                                fontSize = 13.sp,
                                color = muted
                            )
                            if (progress.weightSeries.size >= 2) Sparkline(progress.weightSeries)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

/** Body weight readings. The newest one is used for the calorie estimate. */
@Composable
private fun BodyWeightSection(unit: String) {
    val log by Storage.bodyLog.collectAsState()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var text by remember { mutableStateOf("") }

    fun shownWeight(kg: Double): String {
        val value = if (unit == "lb") kg * LB_PER_KG else kg
        return "${formatWeight(Math.round(value * 10) / 10.0)} $unit"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text("Body weight", fontSize = 13.sp, color = muted)
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                placeholder = { Text(unit, fontSize = 14.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(120.dp)
            )
            QuietButton("Log") {
                val value = text.toDoubleOrNull()?.takeIf { it > 0.0 }
                if (value != null) {
                    Storage.addBodyWeight(if (unit == "lb") value / LB_PER_KG else value)
                    text = ""
                }
            }
            log.firstOrNull()?.let {
                Text("Latest: ${shownWeight(it.kg)}", fontSize = 13.sp, color = muted)
            }
        }
        if (log.size >= 2) Sparkline(log.reversed().map { it.kg })
        Text(
            "Your latest entry is used for the calorie estimate.",
            fontSize = 12.sp,
            color = muted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** A small line chart of [values], oldest first. */
@Composable
private fun Sparkline(values: List<Double>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(top = 6.dp)
    ) {
        if (values.size < 2) return@Canvas
        val low = values.minOrNull() ?: return@Canvas
        val high = values.maxOrNull() ?: return@Canvas
        val range = high - low
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v ->
            val fraction = if (range > 0.0) ((v - low) / range).toFloat() else 0.5f
            Offset(i * stepX, size.height - 4f - fraction * (size.height - 8f))
        }
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
        drawCircle(color, radius = 3.dp.toPx(), center = points.last())
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Light)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------------------------

private fun shown(value: Double?): String =
    value?.let { formatWeight(Math.round(it * 10) / 10.0) } ?: ""

private const val LB_PER_KG = 2.2046226
private const val CM_PER_INCH = 2.54

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val unit by Storage.unit.collectAsState()
    val imperial = unit == "lb"
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    // Fields are shown in the chosen unit and saved in metric. They reset when the unit changes.
    var weight by remember(unit) {
        val kg = Storage.profile.value.weightKg
        mutableStateOf(shown(if (imperial) kg?.times(LB_PER_KG) else kg))
    }
    var height by remember(unit) {
        val cm = Storage.profile.value.heightCm
        mutableStateOf(shown(if (imperial) cm?.div(CM_PER_INCH) else cm))
    }
    var age by remember { mutableStateOf(Storage.profile.value.age?.toString() ?: "") }

    fun commit(w: String, h: String, a: String) {
        val wv = w.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
        val hv = h.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
        Storage.setProfile(
            Profile(
                heightCm = hv?.let { if (imperial) it * CM_PER_INCH else it },
                weightKg = wv?.let { if (imperial) it / LB_PER_KG else it },
                age = a.toIntOrNull()?.takeIf { it in 1..120 },
                male = Storage.profile.value.male
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        QuietButton("Back", onBack)
        Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Medium)
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))

        Text("Weight unit", fontSize = 13.sp, color = muted, modifier = Modifier.padding(top = 16.dp))
        Row {
            UnitChoice("lb / in", imperial) { Storage.setUnit("lb") }
            UnitChoice("kg / cm", !imperial) { Storage.setUnit("kg") }
        }

        Text("About you", fontSize = 13.sp, color = muted, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
        SettingsField("Body weight (${if (imperial) "lb" else "kg"})", weight, KeyboardType.Decimal) {
            weight = it; commit(it, height, age)
        }
        SettingsField("Height (${if (imperial) "in" else "cm"})", height, KeyboardType.Decimal) {
            height = it; commit(weight, it, age)
        }
        SettingsField("Age", age, KeyboardType.Number) {
            age = it; commit(weight, height, it)
        }
        Text(
            "Used only for the calorie estimate after each workout. Saved on this phone. " +
                "The estimate is rough: it uses your body weight, the workout length and a typical effort level for that workout.",
            fontSize = 12.sp,
            color = muted,
            modifier = Modifier.padding(top = 12.dp)
        )
        SexRow()
        AppearanceSection()
        TimingSection()
        RestSection()
        GoalSection()
        DisplaySection()
        RunDaysSection()
        VoiceSection()
        ReminderSection()
        BackupSection()
        PrivacySection()
    }
}

@Composable
private fun UnitChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsField(label: String, value: String, type: KeyboardType, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6)) },
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp),
        keyboardOptions = KeyboardOptions(keyboardType = type),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

private fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toInt().toString() else "%.1f".format(weight)

private fun formatDate(millis: Long): String =
    SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))

private fun formatTime(totalSeconds: Int): String =
    "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

/** Shows the recovery prompt only when no workout is running. */
@Composable
private fun RecoveryDialogHost() {
    val state by WorkoutStateHolder.state.collectAsState()
    if (state.phase == Phase.IDLE) RecoveryDialog()
}

/** Lets the user replace the next move with another one for the same muscle group, for this workout only. */
@Composable
private fun SwapButton(nextName: String) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val group = Days.exercisePool.firstOrNull { it.name == nextName }?.group ?: return
    QuietButton("Swap this move") { open = true }
    if (open) {
        val options = Days.exercisePool.filter { it.group == group && it.name != nextName }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Swap $nextName for") },
            text = {
                LazyColumn {
                    items(options) { option ->
                        Text(
                            option.name,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    open = false
                                    context.startService(
                                        Intent(context, WorkoutService::class.java)
                                            .setAction(WorkoutService.ACTION_SWAP)
                                            .putExtra(WorkoutService.EXTRA_NAME, option.name)
                                    )
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }
        )
    }
}
