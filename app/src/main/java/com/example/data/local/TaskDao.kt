package com.example.data.local

import androidx.room.*
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // =========================
    // 🔥 LIVE UI (Flow version)
    // =========================

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, priority DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskByIdFlow(id: Int): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE isCompleted = :isCompleted")
    fun getTasksByCompletion(isCompleted: Boolean): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE dueDate BETWEEN :startOfDay AND :endOfDay 
        ORDER BY priority DESC
    """)
    fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE isCompleted = 0 AND reminderTime >= :now 
        ORDER BY reminderTime ASC
    """)
    fun getUpcomingTasks(now: Long): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE isCompleted = 0 AND dueDate < :now 
        ORDER BY dueDate DESC
    """)
    fun getOverdueTasks(now: Long): Flow<List<Task>>

    // =========================
    // 🔥 BACKGROUND / WORKER SAFE (List version)
    // =========================

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND reminderTime >= :now")
    suspend fun getUpcomingTasksList(now: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDate < :now")
    suspend fun getOverdueTasksList(now: Long): List<Task>

    @Query("""
        SELECT * FROM tasks 
        WHERE dueDate BETWEEN :start AND :end
    """)
    suspend fun getTasksForDateRangeList(start: Long, end: Long): List<Task>

    // =========================
    // 🔥 BASIC CRUD
    // =========================

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}
