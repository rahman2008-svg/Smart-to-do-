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

class CompleteTaskReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != NotificationHelper.ACTION_MARK_COMPLETE) return

        val taskId = intent.getIntExtra(
            NotificationHelper.EXTRA_TASK_ID,
            -1
        )

        if (taskId == -1) return

        // 🔥 cancel notification safely (no crash risk)
        try {
            NotificationManagerCompat.from(context).cancel(taskId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val result = goAsync()

        val database = AppDatabase.getDatabase(context)
        val taskDao = database.taskDao()

        // 🔥 SAFE COROUTINE SCOPE
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {

            try {

                val task = taskDao.getTaskById(taskId)

                if (task != null) {

                    val updatedTask = task.copy(
                        isCompleted = true,
                        completedAt = System.currentTimeMillis()
                    )

                    taskDao.updateTask(updatedTask)
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
