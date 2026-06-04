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

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {

            try {

                // =========================
                // 🔥 TODAY RANGE (clean)
                // =========================
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                // =========================
                // 🔥 DATA FETCH (LIST ONLY)
                // =========================
                val todayTasks =
                    taskDao.getTasksForDateRangeList(startOfDay, endOfDay)

                val activeTasks = todayTasks.filter { !it.isCompleted }

                val highPriorityCount =
                    activeTasks.count { it.priority.equals("High", ignoreCase = true) }

                // =========================
                // 🔥 NOTIFICATION
                // =========================
                notificationHelper.showDailySummary(
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
