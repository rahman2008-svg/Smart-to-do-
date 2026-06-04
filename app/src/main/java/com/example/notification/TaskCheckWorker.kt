package com.example.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.local.AppDatabase

class TaskCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        val context = applicationContext
        val database = AppDatabase.getDatabase(context)
        val taskDao = database.taskDao()
        val alarmScheduler = AlarmScheduler(context)

        return try {

            val now = System.currentTimeMillis()

            // 🔥 1. RESCHEDULE ONLY UNSCHEDULED UPCOMING TASKS (NO DUPLICATES)
            val upcomingTasks = taskDao.getUpcomingTasksList(now)

            for (task in upcomingTasks) {
                if (task.reminderTime != null && !task.isCompleted) {
                    alarmScheduler.scheduleTaskAlarm(task)
                }
            }

            // 🔥 2. OVERDUE TASKS ALERT (SAFE SINGLE NOTIFICATION)
            val overdueTasks = taskDao.getOverdueTasksList(now)
                .filter { !it.isCompleted }

            if (overdueTasks.isNotEmpty()) {

                if (canNotify()) {

                    val clickIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }

                    val clickPendingIntent = PendingIntent.getActivity(
                        context,
                        8888,
                        clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val notification = NotificationCompat.Builder(
                        context,
                        NotificationHelper.CHANNEL_TASK_ALERTS_ID
                    )
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle("Overdue Tasks ⚠️")
                        .setContentText("${overdueTasks.size} tasks are overdue!")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(clickPendingIntent)
                        .setAutoCancel(true)
                        .build()

                    NotificationManagerCompat.from(context)
                        .notify(8888, notification)
                }
            }

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    // 🔥 Android 13+ notification permission check
    private fun canNotify(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
