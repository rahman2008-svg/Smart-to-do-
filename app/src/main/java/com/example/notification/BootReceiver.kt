package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val result = goAsync()

        val database = AppDatabase.getDatabase(context)
        val taskDao = database.taskDao()
        val preferenceManager = PreferenceManager(context)
        val alarmScheduler = AlarmScheduler(context)

        // 🔥 SAFE SCOPE
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {

            try {

                // =========================
                // 🔥 1. Restore Daily Alarm
                // =========================
                val dailyEnabled =
                    preferenceManager.dailyReminderEnabledFlow.first()

                if (dailyEnabled) {

                    val dailyTime =
                        preferenceManager.dailyReminderTimeFlow.firstOrNull() ?: "08:00"

                    val parts = dailyTime.split(":")

                    if (parts.size == 2) {

                        val hour = parts[0].toIntOrNull() ?: 8
                        val minute = parts[1].toIntOrNull() ?: 0

                        alarmScheduler.scheduleDailyReminder(hour, minute)
                    }
                }

                // =========================
                // 🔥 2. Restore Task Alarms
                // =========================
                val now = System.currentTimeMillis()

                val upcomingTasks =
                    taskDao.getUpcomingTasksList(now)

                for (task in upcomingTasks) {

                    if (task.reminderTime != null && !task.isCompleted) {

                        alarmScheduler.scheduleTaskAlarm(task)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {

                try {
                    result.finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
