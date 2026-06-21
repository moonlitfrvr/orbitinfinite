package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val note: String? = null,
    val dueDate: String? = null, // Formatted as "yyyy-MM-dd" for calendar mapping
    val isCompleted: Boolean = false,
    val completedTime: Long? = null, // epoch millis for 48h cleanup check
    val isDeleted: Boolean = false,
    val deletedTime: Long? = null
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val imageUrl: String? = null, // Picked from system or local assets
    val links: String? = null, // Comma or newline separated links
    val createdTime: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedTime: Long? = null
)
