package com.example.workout

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Starts and stops recordings in OpenTracks through its public API. The user has to switch the API on
 * in OpenTracks' own settings. Nothing is read back from OpenTracks.
 */
object OpenTracks {
    private val PACKAGES = listOf(
        "de.dennisguse.opentracks",
        "de.dennisguse.opentracks.playStore",
        "de.dennisguse.opentracks.nightly"
    )
    private const val START = "de.dennisguse.opentracks.publicapi.StartRecording"
    private const val STOP = "de.dennisguse.opentracks.publicapi.StopRecording"

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

    fun start(context: Context): Boolean {
        val pkg = installedPackage(context) ?: return false
        val ok = launch(context, pkg, START) {
            putExtra("TRACK_NAME", "Run")
            putExtra("TRACK_CATEGORY", "running")
        }
        if (ok) Storage.setRunStarted(System.currentTimeMillis())
        return ok
    }

    /** Stops the recording and writes the run to the history. */
    fun stop(context: Context) {
        val pkg = installedPackage(context)
        if (pkg != null) launch(context, pkg, STOP)
        val started = Storage.runStarted.value
        if (started > 0L) {
            val seconds = ((System.currentTimeMillis() - started) / 1000).toInt()
            Storage.addEntry(
                LogEntry(
                    startedAt = started,
                    dayTitle = "Run",
                    completed = true,
                    totalExercises = 0,
                    durationSeconds = seconds,
                    unit = Storage.unit.value,
                    sets = emptyList(),
                    calories = estimateCalories(9.0, seconds, Storage.profile.value)
                )
            )
        }
        Storage.setRunStarted(0L)
    }
}
