package com.example.workout

import android.net.Uri
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.IntentCompat
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Receives the data handoff from OpenTracks when a run starts and shows live time, distance and pace.
 * It stays open so the read access OpenTracks grants lasts until the run ends.
 */
class RunDashboardActivity : ComponentActivity() {
    private var uri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Storage.init(this)
        accept(intent)
        if (uri == null) uri = Storage.runUri()?.let { Uri.parse(it) }
        setContent {
            WorkoutTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RunScreen(uri, onBack = { finish() }, onEnded = { finish() })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        accept(intent)
    }

    /** Only addresses that belong to OpenTracks are accepted. */
    private fun accept(intent: Intent?) {
        val list = intent?.let {
            IntentCompat.getParcelableArrayListExtra(it, OpenTracks.DASHBOARD_PAYLOAD, Uri::class.java)
        }
        val first = list?.firstOrNull() ?: return
        if (first.authority?.startsWith("de.dennisguse.opentracks") != true) return
        Storage.setRunUri(first.toString())
        uri = first
    }
}

@Composable
private fun RunScreen(uri: Uri?, onBack: () -> Unit, onEnded: () -> Unit) {
    val context = LocalContext.current
    val unit by Storage.unit.collectAsState()
    var stats by remember { mutableStateOf<RunStats?>(null) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val started by Storage.runStarted.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    // A clock of our own, so the time keeps running even when OpenTracks is not reporting movement.
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    // The run was ended from somewhere else, for example the home screen.
    LaunchedEffect(started) { if (started == 0L) onEnded() }

    LaunchedEffect(uri) {
        if (uri == null) return@LaunchedEffect
        while (true) {
            stats = withContext(Dispatchers.IO) { OpenTracks.readStats(context, uri) } ?: stats
            delay(2000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("RUN IN PROGRESS", fontSize = 12.sp, letterSpacing = 2.sp, color = muted)
        val s = stats
        Text(
            formatClock(if (started > 0L) ((now - started) / 1000).toInt() else 0),
            fontSize = 72.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            if (s == null) "elapsed" else "elapsed, moving ${formatClock((s.movingMs / 1000).toInt())}",
            fontSize = 12.sp,
            color = muted
        )
        Text(
            if (s == null) "-" else formatDistance(s.distanceM, unit),
            fontSize = 32.sp,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            if (s == null) "-" else formatPace(s.distanceM, s.movingMs, unit),
            fontSize = 20.sp,
            color = muted,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (uri == null) {
            Text(
                "Waiting for OpenTracks. If nothing appears, switch on Automatic data transfer in its settings.",
                fontSize = 12.sp,
                color = muted,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        Column(modifier = Modifier.padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            PrimaryButton("End run", { OpenTracks.stop(context, stats); onEnded() })
            QuietButton("Back (keeps recording)", onBack)
        }
    }
}
