package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OrbitRepository(private val database: AppDatabase) {
    private val preferenceDao = database.preferenceDao()
    private val taskDao = database.taskDao()
    private val noteDao = database.noteDao()

    // --- Name/Preferences ---
    fun getUserNameFlow(): Flow<String?> {
        return preferenceDao.getPreferenceFlow(KEY_USER_NAME).map { it?.value }
    }

    suspend fun saveUserName(name: String) {
        preferenceDao.insertPreference(PreferenceEntity(KEY_USER_NAME, name))
    }

    fun getPreferenceFlow(key: String): Flow<String?> {
        return preferenceDao.getPreferenceFlow(key).map { it?.value }
    }

    suspend fun getPreferenceValueDirect(key: String): String? {
        return preferenceDao.getPreferenceValueDirect(key)
    }

    suspend fun savePreference(key: String, value: String) {
        preferenceDao.insertPreference(PreferenceEntity(key, value))
    }

    // --- Tasks ---
    val allActiveTasks: Flow<List<TaskEntity>> = taskDao.getAllActiveTasksFlow()
    val binTasks: Flow<List<TaskEntity>> = taskDao.getBinTasksFlow()

    fun getActiveTasksForDate(date: String): Flow<List<TaskEntity>> {
        return taskDao.getActiveTasksForDateFlow(date)
    }

    fun getAllTasksForDate(date: String): Flow<List<TaskEntity>> {
        return taskDao.getAllTasksForDateFlow(date)
    }

    fun getRecentlyFinishedTasks(sinceTime: Long): Flow<List<TaskEntity>> {
        return taskDao.getRecentlyFinishedTasksFlow(sinceTime)
    }

    suspend fun insertTask(task: TaskEntity) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun cleanupCompletedAndDeleted() {
        val fortyEightHoursAgo = System.currentTimeMillis() - 48 * 60 * 60 * 1000L
        taskDao.deleteCompletedOlderThan(fortyEightHoursAgo)
        taskDao.deleteDeletedOlderThan(fortyEightHoursAgo)
    }

    // --- Notes ---
    val allActiveNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotesFlow()
    val deletedNotes: Flow<List<NoteEntity>> = noteDao.getDeletedNotesFlow()

    suspend fun insertNote(note: NoteEntity) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
    }

    companion object {
        private const val KEY_USER_NAME = "key_user_name"
    }
}
