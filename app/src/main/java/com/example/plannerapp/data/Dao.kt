package com.example.plannerapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {
    @Query("SELECT * FROM EventEntity ORDER BY date ASC")
    fun observeEvents(): Flow<List<EventEntity>>

    @Insert
    suspend fun insertEvent(event: EventEntity)

    @Query("DELETE FROM EventEntity WHERE id = :id")
    suspend fun deleteEvent(id: Long)

    @Query("SELECT * FROM TaskEntity WHERE archived = 0 ORDER BY dueDate ASC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM TaskEntity WHERE archived = 1 ORDER BY dueDate DESC")
    fun observeArchivedTasks(): Flow<List<TaskEntity>>

    @Insert
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("SELECT * FROM DiaryEntryEntity ORDER BY date DESC")
    fun observeDiaryEntries(): Flow<List<DiaryEntryEntity>>

    @Insert
    suspend fun insertDiary(entry: DiaryEntryEntity)

    @Query("SELECT * FROM HabitEntity ORDER BY id DESC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Insert
    suspend fun insertHabit(habit: HabitEntity): Long

    @Query("SELECT * FROM HabitMarkEntity")
    fun observeHabitMarks(): Flow<List<HabitMarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitMark(mark: HabitMarkEntity)
}
