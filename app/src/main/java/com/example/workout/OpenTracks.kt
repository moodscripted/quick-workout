package com.example.workout

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import kotlin.math.roundToInt

/** Numbers OpenTracks reports for a recording. */
data class RunStats(val distanceM: Double, val movingMs: Long, val totalMs: Long)

/**
 * Starts and stops recordings in OpenTracks through its public API and reads distance and time back.
 * In OpenTracks' settings the public API must be on for starting and stopping, and "Automatic data transfer"
 * must be on for the numbers. Without the second one a run still logs, with time only.
 */
object OpenTracks {
    private val PACKAGES = listOf(
        "de.dennisguse.opentracks",
        "de.dennisguse.opentracks.playStore",
        "de.dennisguse.opentracks.nightly"
    )
    private const val START = "de.dennisguse.opentracks.publicapi.StartRecording"
    private const val STOP = "de.dennisguse.opentracks.publicapi.StopRecording"
    const val DASHBOARD_ACTION = "Intent.OpenTracks-Dashboard"
    const val DASHBOARD_PAYLOAD = "Intent.OpenTracks-Dashboard.Payload"

    fun installedPackage(context: Context): String? = PACKAGES.firstOrNull {
        try {
            context.packageManager.getPackageInfo(it, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun launch(context: Context, pkg: String, cls: String, extras: Intent.() -> Unit = {}): Boolean =
        try {
            context.startActivity(
                Intent().setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).apply(extras)
            )
            true
        } catch (e: Exception) {
            false
        }

    private const val RUN_CHANNEL = "run"
    private const val RUN_NOTIFICATION = 2

    /** An ongoing notification that opens the live run screen, so the run is easy to get back to. */
    private fun showNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(RUN_CHANNEL, "Run in progress", NotificationManager.IMPORTANCE_LOW))
            val open = PendingIntent.getActivity(
                context, 300,
                Intent(context, RunDashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            manager.notify(
                RUN_NOTIFICATION,
                NotificationCompat.Builder(context, RUN_CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle("Run in progress")
                    .setContentText("Tap to see time, distance and pace.")
                    .setContentIntent(open)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build()
            )
        } catch (e: Exception) {
            // No notification permission or no channel support: the run itself is unaffected.
        }
    }

    private fun hideNotification(context: Context) {
        try {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(RUN_NOTIFICATION)
        } catch (e: Exception) {
            // Nothing to cancel.
        }
    }

    fun start(context: Context): Boolean {
        val pkg = installedPackage(context) ?: return false
        val ok = launch(context, pkg, START) {
            putExtra("TRACK_NAME", "Run")
            putExtra("TRACK_CATEGORY", "running")
            putExtra("TRACK_ICON", "running")
            putExtra("STATS_TARGET_PACKAGE", context.packageName)
            putExtra("STATS_TARGET_CLASS", RunDashboardActivity::class.java.name)
        }
        if (ok) {
            Storage.setRunUri(null)
            Storage.setRunStarted(System.currentTimeMillis())
            showNotification(context)
        }
        return ok
    }

    /** Reads the first track at [uri]. Returns null when access was not granted or nothing is there. */
    fun readStats(context: Context, uri: Uri): RunStats? {
        // OpenTracks offers old and new column names; try the old ones first, then the new ones.
        val projections = listOf(
            arrayOf("totaldistance", "totaltime", "movingtime"),
            arrayOf("distance", "duration_total", "duration_moving")
        )
        for (projection in projections) {
            try {
                context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                    if (c.moveToFirst()) return RunStats(c.getDouble(0), c.getLong(2), c.getLong(1))
                }
            } catch (e: Exception) {
                // Try the next set of names.
            }
        }
        return null
    }

    /** Stops the recording and writes the run to the history. [stats] are the latest numbers if known. */
    fun stop(context: Context, stats: RunStats? = null) {
        val pkg = installedPackage(context)
        val latest = stats ?: Storage.runUri()?.let { readStats(context, Uri.parse(it)) }
        if (pkg != null) launch(context, pkg, STOP)
        val started = Storage.runStarted.value
        if (started > 0L) {
            val wall = ((System.currentTimeMillis() - started) / 1000).toInt()
            val seconds = latest?.totalMs?.let { (it / 1000).toInt() }?.takeIf { it > 0 } ?: wall
            Storage.addEntry(
                LogEntry(
                    startedAt = started,
                    dayTitle = "Run",
                    completed = true,
                    totalExercises = 0,
                    durationSeconds = seconds,
                    unit = Storage.unit.value,
                    sets = emptyList(),
                    calories = estimateCalories(9.0, seconds, Storage.profile.value),
                    distanceM = latest?.distanceM?.roundToInt() ?: 0
                )
            )
        }
        Storage.setRunStarted(0L)
        Storage.setRunUri(null)
        hideNotification(context)
        WorkoutWidget.update(context)
    }
}

/** "5.20 km", or miles when the weight unit is pounds. */
fun formatDistance(meters: Double, unit: String): String =
    if (unit == "lb") "%.2f mi".format(meters / 1609.344) else "%.2f km".format(meters / 1000.0)

/** Minutes and seconds per kilometre or mile, or a dash when there is no distance yet. */
fun formatPace(meters: Double, movingMs: Long, unit: String): String {
    val perUnit = if (unit == "lb") 1609.344 else 1000.0
    if (meters < 50 || movingMs <= 0) return "-"
    val seconds = (movingMs / 1000.0 / (meters / perUnit)).roundToInt()
    return "%d:%02d %s".format(seconds / 60, seconds % 60, if (unit == "lb") "/mi" else "/km")
}
