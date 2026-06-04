package com.example.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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

            // =========================
            // 🔥 1. SAFE RESCHEDULE
            // =========================
            val upcomingTasks = taskDao.getUpcomingTasksList(now)

            for (task in upcomingTasks) {
                try {
                    if (!task.isCompleted && task.reminderTime != null) {
                        alarmScheduler.scheduleTaskAlarm(task)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // =========================
            // 🔥 2. OVERDUE TASKS
            // =========================
            val overdueTasks = taskDao.getOverdueTasksList(now)
                .filter { !it.isCompleted }

            if (overdueTasks.isNotEmpty() && canNotify()) {

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
                    .setContentText("You have ${overdueTasks.size} overdue tasks")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(clickPendingIntent)
                    .setAutoCancel(true)
                    .build()

                NotificationManagerCompat.from(context)
                    .notify(8888, notification)
            }

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    // =========================
    // 🔥 Permission Safe Check
    // =========================
    private fun canNotify(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }
}
