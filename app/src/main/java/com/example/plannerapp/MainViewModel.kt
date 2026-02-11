package com.example.plannerapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.plannerapp.data.AppDatabase
import com.example.plannerapp.data.DiaryEntryEntity
import com.example.plannerapp.data.EventEntity
import com.example.plannerapp.data.HabitEntity
import com.example.plannerapp.data.HabitMarkEntity
import com.example.plannerapp.data.ReminderWorker
import com.example.plannerapp.data.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).plannerDao()
    private val workManager = WorkManager.getInstance(application)

    val events = dao.observeEvents().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tasks = dao.observeTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedTasks = dao.observeArchivedTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val diaryEntries = dao.observeDiaryEntries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val habits = dao.observeHabits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val habitMarks = dao.observeHabitMarks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEvent(title: String, date: String, reminderMinutesBefore: Int?) {
        if (title.isBlank() || date.isBlank()) return
        viewModelScope.launch {
            dao.insertEvent(EventEntity(title = title.trim(), date = date.trim(), reminderMinutesBefore = reminderMinutesBefore))
            scheduleReminderIfNeeded("Событие: $title", "Дата: $date", reminderMinutesBefore)
        }
    }

    fun removeEvent(id: Long) {
        viewModelScope.launch { dao.deleteEvent(id) }
    }

    fun addTask(title: String, dueDate: String, reminderMinutesBefore: Int?) {
        if (title.isBlank() || dueDate.isBlank()) return
        viewModelScope.launch {
            dao.insertTask(TaskEntity(title = title.trim(), dueDate = dueDate.trim(), reminderMinutesBefore = reminderMinutesBefore))
            scheduleReminderIfNeeded("Задача: $title", "Срок: $dueDate", reminderMinutesBefore)
        }
    }

    fun toggleTaskDone(task: TaskEntity) {
        viewModelScope.launch { dao.updateTask(task.copy(done = !task.done)) }
    }

    fun archiveTask(task: TaskEntity) {
        viewModelScope.launch { dao.updateTask(task.copy(archived = true)) }
    }

    fun addDiaryEntry(date: String, text: String, rating: Int, photoUri: String?) {
        if (date.isBlank() || text.isBlank()) return
        viewModelScope.launch {
            dao.insertDiary(DiaryEntryEntity(date = date.trim(), text = text.trim(), rating = rating.coerceIn(1, 5), photoUri = photoUri?.trim()?.ifBlank { null }))
        }
    }

    fun addHabit(title: String, details: String) {
        if (title.isBlank()) return
        viewModelScope.launch { dao.insertHabit(HabitEntity(title = title.trim(), details = details.trim())) }
    }

    fun markHabit(habitId: Long, date: String, status: Int) {
        if (date.isBlank()) return
        viewModelScope.launch { dao.upsertHabitMark(HabitMarkEntity(habitId = habitId, date = date.trim(), status = status)) }
    }

    private fun scheduleReminderIfNeeded(title: String, body: String, reminderMinutesBefore: Int?) {
        val minutes = reminderMinutesBefore ?: return
        if (minutes <= 0) return
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
            .setInputData(Data.Builder().putString("title", title).putString("body", body).build())
            .build()
        workManager.enqueue(request)
    }
}
