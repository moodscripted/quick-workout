package com.example.workout

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date

@Composable
private fun Title(text: String) {
    Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
}

@Composable
private fun <T> Choices(label: String, options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        options.forEach { (text, value) ->
            TextButton(onClick = { onSelect(value) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Text(
                    text,
                    fontSize = 15.sp,
                    fontWeight = if (value == selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (value == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AppearanceSection() {
    val theme by Storage.theme.collectAsState()
    Title("Appearance")
    Choices("Theme", listOf("System" to "system", "Light" to "light", "Dark" to "dark"), theme) { Storage.setTheme(it) }
}

@Composable
fun RestSection() {
    val timing by Storage.timing.collectAsState()
    Title("Breaks")
    Choices("Length of listed breaks", listOf("Full" to 100, "Half" to 50, "None" to 0), timing.restPercent) {
        Storage.setTiming(timing.copy(restPercent = it))
    }
    Text(
        "None runs the moves back to back, like a circuit. Cool downs are shortened too.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun GoalSection() {
    val goal by Storage.goal.collectAsState()
    Title("Weekly goal")
    Choices("Workouts per week", listOf("Off" to 0, "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6), goal) { Storage.setGoal(it) }
}

@Composable
fun RunDaysSection() {
    var mask by remember { mutableStateOf(Storage.runDays()) }
    val names = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    Title("Run days")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        names.forEachIndexed { i, name ->
            val on = mask and (1 shl i) != 0
            TextButton(
                onClick = { mask = mask xor (1 shl i); Storage.setRunDays(mask) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    name,
                    fontSize = 15.sp,
                    fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                    color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Text(
        "On these days the reminder asks for a run instead of a workout. Runs are recorded in OpenTracks, " +
            "which needs its API setting switched on (OpenTracks calls it Automatic data transfer).",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun SexRow() {
    val profile by Storage.profile.collectAsState()
    Choices(
        "Sex (for the calorie estimate)",
        listOf("Male" to true, "Female" to false),
        profile.male
    ) { Storage.setProfile(profile.copy(male = it)) }
}

private const val PRIVACY_TEXT =
    "Quick Workout keeps everything on this phone: your history, weights, settings and any backups you export. " +
        "It has no account, no ads and no analytics, and it does not send your data anywhere. " +
        "Voice updates use the speech engine already on your phone. " +
        "Notifications are used for workout controls and reminders. " +
        "Delete the app, or clear its storage in Android settings, to remove all of it."

@Composable
fun PrivacySection() {
    var open by remember { mutableStateOf(false) }
    Title("About")
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = remember {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "Version ${info.versionName} (build ${androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info)})"
        } catch (e: Exception) {
            "Version unknown"
        }
    }
    Text(version, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    TextButton(onClick = { open = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
        Text("Privacy policy", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Privacy") },
            text = { Text(PRIVACY_TEXT, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Close") } }
        )
    }
}

/** Offers to save or throw away a workout that was cut short when the app was killed. */
@Composable
fun RecoveryDialog() {
    var pending by remember { mutableStateOf(Storage.pending()) }
    val entry = pending ?: return
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Unfinished workout") },
        text = {
            Text(
                "${entry.dayTitle} stopped after ${entry.doneExercises} of ${entry.totalExercises} moves, " +
                    "on ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.startedAt))}. " +
                    "Save what you did to your history?"
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (Storage.log.value.none { it.startedAt == entry.startedAt }) Storage.addEntry(entry)
                Storage.clearPending()
                pending = null
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { Storage.clearPending(); pending = null }) { Text("Discard") }
        }
    )
}

/** Weeks of workouts as bars, newest on the right. */
@Composable
fun WeeklyChart(entries: List<LogEntry>) {
    val totals = remember(entries) { weeklyTotals(entries) }
    val bar = MaterialTheme.colorScheme.primary
    val faint = MaterialTheme.colorScheme.outlineVariant
    val max = (totals.maxOfOrNull { it.first } ?: 0).coerceAtLeast(1)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text("Workouts per week", fontSize = 14.sp)
        Text(
            "Last 8 weeks, ${totals.sumOf { it.first }} workouts, ${totals.sumOf { it.second }} min",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Canvas(modifier = Modifier.fillMaxWidth().height(70.dp).padding(top = 8.dp)) {
            val slot = size.width / totals.size
            val width = slot * 0.6f
            totals.forEachIndexed { i, (count, _) ->
                val h = size.height * count / max
                val x = i * slot + (slot - width) / 2
                drawRoundRect(faint, Offset(x, size.height - 3.dp.toPx()), Size(width, 3.dp.toPx()), CornerRadius(2.dp.toPx()))
                if (count > 0) {
                    drawRoundRect(bar, Offset(x, size.height - h), Size(width, h), CornerRadius(3.dp.toPx()))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("8 wk ago", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("this week", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Sends a plain text summary of a finished workout to any app. */
fun shareEntry(context: Context, entry: LogEntry) {
    val text = buildString {
        append("${entry.dayTitle}, ")
        append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.startedAt)))
        append("\n${entry.doneExercises} of ${entry.totalExercises} moves, ${(entry.durationSeconds + 30) / 60} min")
        entry.calories?.let { append(", about $it cal") }
        entry.sets.forEach { set ->
            val parts = listOfNotNull(
                set.reps?.let { "$it reps" },
                set.weight?.let { "${if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it)} ${entry.unit}" }
            )
            append("\n- ${set.name}")
            if (parts.isNotEmpty()) append(": ${parts.joinToString(" at ")}")
        }
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share workout"))
}
