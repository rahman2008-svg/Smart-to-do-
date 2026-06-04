package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val result = goAsync()

        val database = AppDatabase.getDatabase(context)
        val taskDao = database.taskDao()
        val notificationHelper = NotificationHelper(context)

        // 🔥 SAFE SCOPE (no leak, crash safe)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {

            try {

                // 🔥 Get today start/end range
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = calendar.timeInMillis

                calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val endOfDay = calendar.timeInMillis

                // 🔥 Direct list query (NO Flow.first())
                val todayTasks =
                    taskDao.getTasksForDateRangeList(startOfDay, endOfDay)

                val activeTasks = todayTasks.filter { !it.isCompleted }

                val highPriorityCount =
                    activeTasks.count { it.priority.equals("High", true) }

                // 🔥 Send summary notification
                notificationHelper.showDailySummaryNotification(
                    todayCount = activeTasks.size,
                    highPriorityCount = highPriorityCount
                )

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
