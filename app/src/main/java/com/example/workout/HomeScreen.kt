package com.example.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private class Look(val emoji: String, val color: Color)

private fun lookOf(day: Day): Look = when (day.id) {
    "warm-up" -> Look("🔥", Color(0xFFFF8A3D))
    "chest-back" -> Look("💪", Color(0xFF3D8BFF))
    "arms" -> Look("🏋️", Color(0xFFB04DFF))
    "legs-back" -> Look("🦵", Color(0xFF2DBE6C))
    "abs" -> Look("⚡", Color(0xFFFFC21F))
    "chest-shoulders-triceps" -> Look("🔵", Color(0xFFFF4D6A))
    "stretch" -> Look("\uD83E\uDDD8", Color(0xFF3DB8A5))
    "cardio-power" -> Look("\uD83E\uDD98", Color(0xFFE0457B))
    "pure-cardio" -> Look("\uD83D\uDCA8", Color(0xFF2FA8E0))
    "cardio-abs" -> Look("\uD83C\uDF00", Color(0xFF9B6BFF))
    "cardio-circuit" -> Look("\uD83D\uDCA5", Color(0xFFFF5A36))
    "back-biceps" -> Look("🦾", Color(0xFF1FC8C8))
    else -> Look("⭐", Color(0xFF8A8A8A))
}

private fun lastDone(day: Day, entries: List<LogEntry>): String {
    val entry = entries.firstOrNull { it.dayTitle == day.title } ?: return "Not done yet"
    val then = Instant.ofEpochMilli(entry.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = ChronoUnit.DAYS.between(then, LocalDate.now()).toInt()
    return when {
        days <= 0 -> "Done today"
        days == 1 -> "Done yesterday"
        else -> "Done $days days ago"
    }
}

@Composable
private fun Badge(look: Look, size: Int) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(look.color.copy(alpha = 0.22f)).clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) { Text(look.emoji, fontSize = (size * 0.5).sp) }
}

@Composable
private fun cardBrush(look: Look): Brush {
    val base = MaterialTheme.colorScheme.surfaceVariant
    return Brush.linearGradient(listOf(look.color.copy(alpha = 0.30f), base))
}

@Composable
private fun HeroCard(day: Day, entries: List<LogEntry>, onClick: () -> Unit) {
    val look = lookOf(day)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBrush(look))
            .clickable(onClickLabel = "Open ${day.title}", role = Role.Button, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Badge(look, 56)
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text("UP NEXT", fontSize = 11.sp, color = look.color, fontWeight = FontWeight.Medium)
            Text(day.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Text(
                summaryOf(day) + " • " + lastDone(day, entries),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text("Start ›", fontSize = 15.sp, color = look.color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DayCard(day: Day, entries: List<LogEntry>, onClick: () -> Unit) {
    val look = lookOf(day)
    val record = remember(entries) {
        val index = entries.indexOfFirst { it.dayTitle == day.title }
        index >= 0 && findBests(entries[index], entries.drop(index + 1)).isNotEmpty()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardBrush(look))
            .clickable(onClickLabel = "Open ${day.title}", role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Badge(look, 42)
            if (record) Text("\uD83C\uDFC6 PB", fontSize = 12.sp, color = look.color, fontWeight = FontWeight.Medium)
        }
        Column {
            Text(day.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                summaryOf(day),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(lastDone(day, entries), fontSize = 11.sp, color = look.color, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun RunCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val started by Storage.runStarted.collectAsState()
    var message by remember { mutableStateOf<String?>(null) }
    val look = Look("\uD83C\uDFC3", Color(0xFF39B54A))
    val running = started > 0L
    val runDay = Storage.runDays() and (1 shl (LocalDate.now().dayOfWeek.value - 1)) != 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBrush(look))
            .clickable(onClickLabel = if (running) "Open the live run screen" else "Start a run in OpenTracks", role = Role.Button) {
                if (running) {
                    // Go back to the live run screen instead of ending the run.
                    context.startActivity(
                        android.content.Intent(context, RunDashboardActivity::class.java)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                } else {
                    val installed = OpenTracks.installedPackage(context) != null
                    message = when {
                        !installed -> "OpenTracks is not installed."
                        !OpenTracks.start(context) -> "Could not start OpenTracks. Switch on its Public API in its settings."
                        else -> null
                    }
                }
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Badge(look, 42)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                if (running) "Run in progress" else if (runDay) "Go for a run (run day)" else "Go for a run",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                message ?: if (running) "Tap to see it live." else "Records in OpenTracks.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            if (running) "End run" else "Start \u203A",
            fontSize = 15.sp,
            color = look.color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable(enabled = running) { OpenTracks.stop(context) }
                .padding(8.dp)
        )
    }
}

@Composable
internal fun DayList(
    days: List<Day>,
    suggested: Day,
    entries: List<LogEntry>,
    bests: List<String>,
    onReview: () -> Unit,
    finished: Boolean,
    calories: Int?,
    onOpenLog: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProgress: () -> Unit,
    onNew: () -> Unit,
    onPick: (Day) -> Unit
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val goal by Storage.goal.collectAsState()
    var showBackup by remember { mutableStateOf(Storage.backupDue()) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("Quick Workout", fontSize = 24.sp, fontWeight = FontWeight.Medium)
                Text(
                    when {
                        !finished -> "Voice updates play over your music."
                        calories != null -> "Workout complete. About $calories calories burned."
                        else -> "Workout complete. Add your weight in Settings for a calorie estimate."
                    },
                    fontSize = 13.sp,
                    color = muted,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (finished) {
                    bests.forEach {
                        Text("New best: $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    QuietButton("Add or edit reps and weights", onReview)
                }
            }
        }
        if (finished && entries.firstOrNull()?.dayTitle == "Warm-Up") {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text("Warm-up done. Ready for ${suggested.title}?", fontSize = 14.sp)
                    PrimaryButton("Start ${suggested.title}", { onPick(suggested) })
                }
            }
        }
        if (showBackup) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "It has been a while since your last backup. Settings has Back up to file.",
                        fontSize = 12.sp,
                        color = muted,
                        modifier = Modifier.weight(1f)
                    )
                    QuietButton("Later") { Storage.snoozeBackupReminder(); showBackup = false }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { HeroCard(suggested, entries) { onPick(suggested) } }
        if (goal > 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val done = weeklyTotals(entries).last().first
                Column {
                    Text(
                        if (done >= goal) "Weekly goal reached: $done of $goal workouts" else "This week: $done of $goal workouts",
                        fontSize = 12.sp,
                        color = muted
                    )
                    LinearProgressIndicator(
                        progress = { (done.toFloat() / goal).coerceAtMost(1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuietButton("History", onOpenLog)
                QuietButton("Progress", onOpenProgress)
                QuietButton("Settings", onOpenSettings)
            }
        }
        items(days) { day -> DayCard(day, entries) { onPick(day) } }
        item(span = { GridItemSpan(maxLineSpan) }) { RunCard() }
        item(span = { GridItemSpan(maxLineSpan) }) {
            val context = androidx.compose.ui.platform.LocalContext.current
            var importMessage by remember { mutableStateOf<String?>(null) }
            Column {
                Row {
                    QuietButton("+ Create your own workout", onNew)
                    QuietButton("Paste a shared workout") {
                        val clip = (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                            .primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                        val added = Storage.importCustomText(clip)
                        importMessage = if (added != null) "Added ${added.title}." else "Nothing to import. Copy the shared text first."
                    }
                }
                importMessage?.let { Text(it, fontSize = 12.sp, color = muted) }
            }
        }
    }
}
