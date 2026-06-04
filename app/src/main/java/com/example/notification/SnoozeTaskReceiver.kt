package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SnoozeTaskReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != NotificationHelper.ACTION_SNOOZE) return

        val taskId = intent.getIntExtra(
            NotificationHelper.EXTRA_TASK_ID,
            -1
        )

        if (taskId == -1) return

        // 🔥 cancel current notification safely
        try {
            NotificationManagerCompat.from(context).cancel(taskId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val result = goAsync()

        val database = AppDatabase.getDatabase(context)
        val taskDao = database.taskDao()
        val alarmScheduler = AlarmScheduler(context)

        // 🔥 SAFE COROUTINE SCOPE
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {

            try {

                val task = taskDao.getTaskById(taskId)

                if (task != null && !task.isCompleted) {

                    // 🔥 Snooze = 15 minutes later
                    val snoozedTime =
                        System.currentTimeMillis() + (15 * 60 * 1000)

                    // ✔ update task safely
                    val updatedTask = task.copy(
                        reminderTime = snoozedTime
                    )

                    taskDao.updateTask(updatedTask)

                    // ✔ reschedule alarm
                    alarmScheduler.scheduleTaskAlarm(updatedTask)
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
