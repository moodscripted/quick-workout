package com.example.workout

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.IOException
import kotlin.math.roundToInt

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    shownValue: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(shownValue, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

// ---------------------------------------------------------------------------------------------
// Timing
// ---------------------------------------------------------------------------------------------

@Composable
private fun ChoiceRow(label: String, options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        options.forEach { option ->
            TextButton(
                onClick = { onSelect(option) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "$option s",
                    fontSize = 15.sp,
                    fontWeight = if (option == selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (option == selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TimingSection() {
    val timing by Storage.timing.collectAsState()
    SectionTitle("Timing")
    ChoiceRow("Get-ready time", listOf(5, 10, 15), timing.intro) { Storage.setTiming(timing.copy(intro = it)) }
    ChoiceRow("Time per move", listOf(45, 60, 90), timing.work) { Storage.setTiming(timing.copy(work = it)) }
    Text(
        "Warm-up moves stay at 40 seconds.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ---------------------------------------------------------------------------------------------
// Voice
// ---------------------------------------------------------------------------------------------

@Composable
fun VoiceSection() {
    val voice by Storage.voice.collectAsState()
    SectionTitle("Voice")
    ToggleRow("Halfway call", voice.halfway) { Storage.setVoice(voice.copy(halfway = it)) }
    ToggleRow("Ten-second warning", voice.tenSeconds) { Storage.setVoice(voice.copy(tenSeconds = it)) }
    ToggleRow("Vibration cues", voice.vibrate) { Storage.setVoice(voice.copy(vibrate = it)) }
    ToggleRow("Beep cues", voice.beeps) { Storage.setVoice(voice.copy(beeps = it)) }
    SliderRow("Voice volume", "${(voice.volume * 100).roundToInt()}%", voice.volume, 0.2f..1f) {
        Storage.setVoice(voice.copy(volume = it))
    }
    SliderRow("Speech speed", "%.1fx".format(voice.rate), voice.rate, 0.7f..1.5f) {
        Storage.setVoice(voice.copy(rate = it))
    }
}

// ---------------------------------------------------------------------------------------------
// Reminder
// ---------------------------------------------------------------------------------------------

private val DAY_LABELS = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

@Composable
fun ReminderSection() {
    val context = LocalContext.current
    val reminder by Storage.reminder.collectAsState()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun update(next: Reminder) {
        Storage.setReminder(next)
        Reminders.schedule(context)
    }

    SectionTitle("Reminder")
    ToggleRow("Workout reminder", reminder.enabled) { on ->
        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        update(reminder.copy(enabled = on))
    }
    if (reminder.enabled) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Time", fontSize = 15.sp, modifier = Modifier.weight(1f))
            StepperButton("-") { update(reminder.copy(hour = (reminder.hour + 23) % 24)) }
            Text(
                "%02d".format(reminder.hour),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            StepperButton("+") { update(reminder.copy(hour = (reminder.hour + 1) % 24)) }
            Text(":", fontSize = 17.sp, modifier = Modifier.padding(horizontal = 6.dp))
            StepperButton("-") { update(reminder.copy(minute = (reminder.minute + 55) % 60)) }
            Text(
                "%02d".format(reminder.minute),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            StepperButton("+") { update(reminder.copy(minute = (reminder.minute + 5) % 60)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DAY_LABELS.forEachIndexed { index, label ->
                val selected = (reminder.days and (1 shl index)) != 0
                TextButton(
                    onClick = { update(reminder.copy(days = reminder.days xor (1 shl index))) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            "Reminders can arrive a few minutes late.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Backup
// ---------------------------------------------------------------------------------------------

@Composable
fun BackupSection() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            message = try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(Storage.exportJson().toByteArray())
                }
                Storage.markBackup()
                "Backup saved."
            } catch (e: IOException) {
                "Could not save the backup."
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingRestore = uri }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            message = try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(Storage.exportCsv().toByteArray())
                }
                "History exported."
            } catch (e: IOException) {
                "Could not export the history."
            }
        }
    }

    SectionTitle("Backup")
    Row {
        QuietButton("Back up to file") { exportLauncher.launch("quick-workout-backup.json") }
        QuietButton("Restore from file") { importLauncher.launch(arrayOf("*/*")) }
    }
    QuietButton("Export history as CSV") { csvLauncher.launch("quick-workout-history.csv") }
    message?.let {
        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore backup?") },
            text = { Text("This replaces your history, settings and custom workouts on this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    message = restoreBackup(context, uri)
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text("Cancel") }
            }
        )
    }
}

private fun restoreBackup(context: Context, uri: Uri): String =
    try {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (text != null && Storage.importJson(text)) {
            Reminders.schedule(context)
            "Backup restored."
        } else {
            "That file is not a Quick Workout backup."
        }
    } catch (e: IOException) {
        "Could not read that file."
    } catch (e: SecurityException) {
        "Could not read that file."
    }
