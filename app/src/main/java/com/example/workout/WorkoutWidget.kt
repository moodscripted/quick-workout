package com.example.workout

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Home screen widget: the suggested workout and your week. Tapping it opens that workout. */
class WorkoutWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Storage.init(context)
        val views = buildViews(context)
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, views)
    }

    companion object {
        const val EXTRA_OPEN_DAY = "open_day"

        /** Refreshes every widget on the home screen, for example after a workout is saved. */
        fun update(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, WorkoutWidget::class.java))
            if (ids.isEmpty()) return
            Storage.init(app)
            val views = buildViews(app)
            for (id in ids) manager.updateAppWidget(id, views)
        }

        private fun buildViews(context: Context): RemoteViews {
            val entries = Storage.log.value
            val next = suggestNext(entries)
            val summary = summarize(entries)
            val views = RemoteViews(context.packageName, R.layout.widget_workout)
            val today = java.time.LocalDate.now()
            val runDay = Storage.runDays() and (1 shl (today.dayOfWeek.value - 1)) != 0
            val ranToday = entries.any {
                it.dayTitle == "Run" &&
                    java.time.Instant.ofEpochMilli(it.startedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today
            }
            val goal = Storage.goal.value
            val week = weeklyTotals(entries).last().first
            views.setTextViewText(R.id.widget_label, if (runDay && !ranToday) "RUN DAY" else "NEXT WORKOUT")
            views.setTextViewText(R.id.widget_next, if (runDay && !ranToday) "Go for a run" else next.title)
            views.setTextViewText(
                R.id.widget_stats,
                (if (goal > 0) "$week of $goal this week" else "$week this week") + "  ·  ${summary.streakDays} day streak"
            )
            val open = PendingIntent.getActivity(
                context, 200,
                Intent(context, MainActivity::class.java)
                    .putExtra(EXTRA_OPEN_DAY, next.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            return views
        }
    }
}
