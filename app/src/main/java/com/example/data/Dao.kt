package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preferences WHERE `key` = :key LIMIT 1")
    fun getPreferenceFlow(key: String): Flow<PreferenceEntity?>

    @Query("SELECT value FROM preferences WHERE `key` = :key LIMIT 1")
    suspend fun getPreferenceValueDirect(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: PreferenceEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND isDeleted = 0 ORDER BY id DESC")
    fun getAllActiveTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date AND isCompleted = 0 AND isDeleted = 0 ORDER BY id DESC")
    fun getActiveTasksForDateFlow(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date AND isDeleted = 0 ORDER BY id DESC")
    fun getAllTasksForDateFlow(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE (isCompleted = 1 AND isDeleted = 0) OR (isDeleted = 1) ORDER BY completedTime DESC")
    fun getBinTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND isDeleted = 0 AND completedTime >= :sinceTime ORDER BY completedTime DESC")
    fun getRecentlyFinishedTasksFlow(sinceTime: Long): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE isCompleted = 1 AND completedTime < :thresholdTime")
    suspend fun deleteCompletedOlderThan(thresholdTime: Long)

    @Query("DELETE FROM tasks WHERE isDeleted = 1 AND deletedTime < :thresholdTime")
    suspend fun deleteDeletedOlderThan(thresholdTime: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY createdTime DESC")
    fun getAllActiveNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedTime DESC")
    fun getDeletedNotesFlow(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}
