package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.model.Task

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_TASK_ALERTS_ID = "channel_task_alerts"
        const val CHANNEL_DAILY_REMINDERS_ID = "channel_daily_reminders"

        const val EXTRA_TASK_ID = "extra_task_id"

        const val ACTION_MARK_COMPLETE = "com.example.notification.ACTION_MARK_COMPLETE"
        const val ACTION_SNOOZE = "com.example.notification.ACTION_SNOOZE"
    }

    init {
        createNotificationChannels()
    }

    // 🔥 Permission check (Android 13+)
    private fun canPostNotification(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }

    // 🔥 Channel creation
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val taskChannel = NotificationChannel(
                CHANNEL_TASK_ALERTS_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Task alerts and reminders"
                enableVibration(true)
            }

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY_REMINDERS_ID,
                "Daily Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily task summary notifications"
            }

            manager.createNotificationChannel(taskChannel)
            manager.createNotificationChannel(dailyChannel)
        }
    }

    // 🔥 MAIN TASK NOTIFICATION
    fun showTaskNotification(task: Task) {

        if (!canPostNotification()) return

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_TASK_ID, task.id)
        }

        val clickPendingIntent = PendingIntent.getActivity(
            context,
            task.id,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, CompleteTaskReceiver::class.java).apply {
            action = ACTION_MARK_COMPLETE
            putExtra(EXTRA_TASK_ID, task.id)
        }

        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id + 1000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, SnoozeTaskReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, task.id)
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id + 2000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TASK_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Task: ${task.title}")
            .setContentText(task.description.ifBlank { "Priority: ${task.priority}" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(clickPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Done", completePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze", snoozePendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(task.id, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 🔥 DAILY SUMMARY
    fun showDailySummary(todayCount: Int, highPriorityCount: Int) {

        if (!canPostNotification()) return

        val clickIntent = Intent(context, MainActivity::class.java)

        val clickPendingIntent = PendingIntent.getActivity(
            context,
            9999,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (todayCount > 0) {
            "Today: $todayCount tasks, $highPriorityCount high priority"
        } else {
            "No tasks today. Plan your day!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_REMINDERS_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Daily Summary")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(clickPendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(9999, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
