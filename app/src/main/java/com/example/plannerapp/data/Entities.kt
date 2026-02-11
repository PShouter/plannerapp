package com.example.plannerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String,
    val reminderMinutesBefore: Int? = null
)

@Entity
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDate: String,
    val done: Boolean = false,
    val archived: Boolean = false,
    val reminderMinutesBefore: Int? = null
)

@Entity
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val text: String,
    val rating: Int,
    val photoUri: String? = null
)

@Entity
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val details: String = ""
)

@Entity(primaryKeys = ["habitId", "date"])
data class HabitMarkEntity(
    val habitId: Long,
    val date: String,
    val status: Int // 1 done, -1 missed, 0 none
)
