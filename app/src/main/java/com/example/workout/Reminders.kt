package com.example.workout

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.time.ZonedDateTime

/** Schedules one reminder at a time. Each alarm sets up the next one when it fires. */
object Reminders {
    const val CHANNEL_ID = "reminders"
    private const val REQUEST_CODE = 100
    private const val NOTIFICATION_ID = 2

    /** Epoch millis of the next time the reminder should fire, or null if no day is selected. */
    fun nextTrigger(reminder: Reminder, now: ZonedDateTime = ZonedDateTime.now()): Long? {
        if ((reminder.days and 0x7F) == 0) return null
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if ((reminder.days and (1 shl (date.dayOfWeek.value - 1))) == 0) continue
            val at = date.atTime(reminder.hour, reminder.minute).atZone(now.zone)
            if (at.isAfter(now)) return at.toInstant().toEpochMilli()
        }
        return null
    }

    /** Cancels any pending reminder and schedules the next one if reminders are on. */
    fun schedule(context: Context) {
        val app = context.applicationContext
        Storage.init(app)
        val alarms = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(app)
        alarms.cancel(pending)
        val reminder = Storage.reminder.value
        if (!reminder.enabled) return
        val at = nextTrigger(reminder) ?: return
        // Not exact on purpose: it may fire a few minutes late, and needs no special permission.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    fun notifyNow(context: Context, run: Boolean = false) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val next = suggestNext(Storage.log.value).title
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(if (run) "Time for a run" else "Time for $next")
            .setContentText(if (run) "Tap to open, then start recording in OpenTracks." else goalLine())
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun goalLine(): String {
        val goal = Storage.goal.value
        if (goal <= 0) return "Tap to open and start."
        return "${weeklyTotals(Storage.log.value).last().first} of $goal workouts this week. Tap to start."
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Storage.init(context)
        val today = java.time.LocalDate.now()
        fun doneToday(match: (LogEntry) -> Boolean) = Storage.log.value.any {
            match(it) &&
                java.time.Instant.ofEpochMilli(it.startedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today
        }
        val runDay = Storage.runDays() and (1 shl (today.dayOfWeek.value - 1)) != 0
        val enabled = Storage.reminder.value.enabled
        if (enabled) {
            if (runDay) {
                if (!doneToday { it.dayTitle == "Run" }) Reminders.notifyNow(context, run = true)
            } else if (!doneToday { it.dayTitle != "Warm-Up" }) {
                Reminders.notifyNow(context)
            }
        }
        Reminders.schedule(context)
    }
}

/** Puts the reminder back after a reboot or a clock or time zone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Reminders.schedule(context)
        }
    }
}
