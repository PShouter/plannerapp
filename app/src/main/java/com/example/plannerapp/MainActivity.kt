package com.example.plannerapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.plannerapp.data.TaskEntity

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MaterialTheme {
                PlannerApp(vm)
            }
        }
    }
}

enum class Screen(val route: String, val label: String) {
    Home("home", "Главная"),
    Calendar("calendar", "Календарь"),
    Todo("todo", "TODO"),
    Diary("diary", "Дневник"),
    Habits("habits", "Привычки"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerApp(vm: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val items = Screen.entries

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val icon = when (screen) {
                                Screen.Home -> Icons.Default.Home
                                Screen.Calendar -> Icons.Default.DateRange
                                Screen.Todo -> Icons.Default.List
                                Screen.Diary -> Icons.Default.Star
                                Screen.Habits -> Icons.Default.TrackChanges
                            }
                            Icon(icon, contentDescription = screen.label)
                        },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) { HomeScreen(vm) }
            composable(Screen.Calendar.route) { CalendarScreen(vm) }
            composable(Screen.Todo.route) { TodoScreen(vm) }
            composable(Screen.Diary.route) { DiaryScreen(vm) }
            composable(Screen.Habits.route) { HabitsScreen(vm) }
        }
    }
}

@Composable
private fun HomeScreen(vm: MainViewModel) {
    val events by vm.events.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val quote = "Маленький шаг каждый день ведёт к большим результатам."

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("PlannerApp", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Сводка недели")
        }
        item {
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("Календарь на неделю", fontWeight = FontWeight.SemiBold)
                    Text("Событий: ${events.size}")
                    events.take(3).forEach { Text("• ${it.date} — ${it.title}") }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("TODO сегодня", fontWeight = FontWeight.SemiBold)
                    Text("Активных задач: ${tasks.count { !it.done }}")
                    tasks.take(3).forEach { Text("• ${it.dueDate} — ${it.title}") }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(listOf(Color(0xFFCEEAF7), Color(0xFFE7F7E9)))
                    )
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Успокаивающая фотография (заглушка)")
                    Spacer(Modifier.height(8.dp))
                    Text("🌿 Вид на горы и озеро")
                }
            }
        }
        item {
            Card {
                Text("Мотивация: $quote", modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun CalendarScreen(vm: MainViewModel) {
    val events by vm.events.collectAsState()
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var reminder by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                vm.addEvent(title, date, reminder.toIntOrNull())
                title = ""
                date = ""
                reminder = ""
            }) { Icon(Icons.Default.Add, contentDescription = "add") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(12.dp)) {
            Text("Календарь", style = MaterialTheme.typography.headlineMedium)
            Text("Формат даты: YYYY-MM-DD")
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название события") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Дата") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = reminder, onValueChange = { reminder = it }, label = { Text("Напоминание через минут (опц.)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(events) { event ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(event.title, fontWeight = FontWeight.SemiBold)
                                Text(event.date)
                            }
                            Button(onClick = { vm.removeEvent(event.id) }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoScreen(vm: MainViewModel) {
    val tasks by vm.tasks.collectAsState()
    val archived by vm.archivedTasks.collectAsState()
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var reminder by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("TODO-лист", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Задача") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Срок YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = reminder, onValueChange = { reminder = it }, label = { Text("Напоминание через минут (опц.)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            vm.addTask(title, date, reminder.toIntOrNull())
            title = ""; date = ""; reminder = ""
        }, modifier = Modifier.padding(top = 8.dp)) { Text("Добавить") }

        Spacer(Modifier.height(12.dp))
        Text("Невыполненные / активные")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks) { task -> TaskItem(task, vm) }
            item { Spacer(Modifier.height(6.dp)) }
            item { Text("Архив") }
            items(archived) { task -> Card(Modifier.fillMaxWidth()) { Text("${task.dueDate}: ${task.title}", Modifier.padding(12.dp)) } }
        }
    }
}

@Composable
private fun TaskItem(task: TaskEntity, vm: MainViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.done, onCheckedChange = { vm.toggleTaskDone(task) })
            Column(Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.SemiBold)
                Text(task.dueDate)
            }
            Button(onClick = { vm.archiveTask(task) }) { Text("В архив") }
        }
    }
}

@Composable
private fun DiaryScreen(vm: MainViewModel) {
    val entries by vm.diaryEntries.collectAsState()
    var date by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("5") }
    var photo by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Личный дневник", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Дата YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Запись") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = rating, onValueChange = { rating = it }, label = { Text("Рейтинг 1..5") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = photo, onValueChange = { photo = it }, label = { Text("URI фото (опц.)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            vm.addDiaryEntry(date, text, rating.toIntOrNull() ?: 5, photo)
            date = ""; text = ""; rating = "5"; photo = ""
        }, modifier = Modifier.padding(top = 8.dp)) { Text("Сохранить") }

        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { e ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(e.date, fontWeight = FontWeight.SemiBold)
                        Text("Оценка: ${"⭐".repeat(e.rating)}")
                        Text(e.text)
                        if (!e.photoUri.isNullOrBlank()) Text("Фото: ${e.photoUri}")
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitsScreen(vm: MainViewModel) {
    val habits by vm.habits.collectAsState()
    val marks by vm.habitMarks.collectAsState()
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Трекер привычек", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Новая привычка") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Подробности") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.addHabit(title, details); title = ""; details = "" }, modifier = Modifier.padding(top = 8.dp)) { Text("+ Добавить") }
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Дата отметки YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(habits) { h ->
                val mark = marks.firstOrNull { it.habitId == h.id && it.date == date }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(h.title, fontWeight = FontWeight.Bold)
                        if (h.details.isNotBlank()) Text(h.details)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.markHabit(h.id, date, 1) }) { Text("✅") }
                            Button(onClick = { vm.markHabit(h.id, date, -1) }) { Text("❌") }
                            Text("Статус: ${when (mark?.status) { 1 -> "выполнено"; -1 -> "пропущено"; else -> "нет" }}")
                        }
                    }
                }
            }
        }
    }
}
