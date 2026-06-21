package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.OrbitGeminiManager
import com.example.data.NoteEntity
import com.example.data.OrbitRepository
import com.example.data.TaskEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class OrbitViewModel(private val repository: OrbitRepository) : ViewModel() {

    // --- User Preferences ---
    val userName: StateFlow<String?> = repository.getUserNameFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Navigation & States ---
    private val _currentTab = MutableStateFlow<OrbitTab>(OrbitTab.Tasks)
    val currentTab: StateFlow<OrbitTab> = _currentTab.asStateFlow()

    private val _taskTabMode = MutableStateFlow<TaskTabMode>(TaskTabMode.MyList)
    val taskTabMode: StateFlow<TaskTabMode> = _taskTabMode.asStateFlow()

    // --- Selected Calendar Date ---
    private val _selectedDate = MutableStateFlow<String>(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // --- Loading States ---
    private val _isOrganizing = MutableStateFlow(false)
    val isOrganizing: StateFlow<Boolean> = _isOrganizing.asStateFlow()

    // --- Room Data Flows ---
    val activeTasks: StateFlow<List<TaskEntity>> = repository.allActiveTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val binTasks: StateFlow<List<TaskEntity>> = repository.binTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotes: StateFlow<List<NoteEntity>> = repository.allActiveNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedNotes: StateFlow<List<NoteEntity>> = repository.deletedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dynamic Tasks for Selected Date ---
    val calendarTasks: StateFlow<List<TaskEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getAllTasksForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- App Usage & Streak ---
    private val _usageDates = MutableStateFlow<Set<String>>(emptySet())
    val usageDates: StateFlow<Set<String>> = _usageDates.asStateFlow()

    private val _streakCount = MutableStateFlow<Int>(0)
    val streakCount: StateFlow<Int> = _streakCount.asStateFlow()

    private val _confettiEvent = MutableStateFlow<Boolean>(false)
    val confettiEvent: StateFlow<Boolean> = _confettiEvent.asStateFlow()

    fun triggerConfetti() {
        _confettiEvent.value = true
    }

    fun dismissConfetti() {
        _confettiEvent.value = false
    }

    // --- Rabbit Companion (INFINITE) Messages ---
    private val _rabbitMessage = MutableStateFlow<String>("Hello, welcome to Orbit!")
    val rabbitMessage: StateFlow<String> = _rabbitMessage.asStateFlow()

    init {
        // Record usage and calculate streak
        recordAppUsageAndCalculateStreak()

        // Trigger 48-hour database cleanup on scope startup
        viewModelScope.launch {
            repository.cleanupCompletedAndDeleted()
        }

        // Welcome message on start
        viewModelScope.launch {
            userName.collect { name ->
                if (name != null) {
                    setRabbitMessage("Welcome back, $name. I'm Infinite!")
                } else {
                    setRabbitMessage("Hello there space traveler! What should I call you?")
                }
            }
        }
    }

    fun setTab(tab: OrbitTab) {
        _currentTab.value = tab
        // Context-aware rabbit messages on tab change
        when (tab) {
            OrbitTab.Tasks -> {
                if (taskTabMode.value == TaskTabMode.BrainDump) {
                    setRabbitMessage("Need somewhere to put that thought? Try Brain Dump.")
                } else {
                    triggerRandomEncouragement()
                }
            }
            OrbitTab.Write -> setRabbitMessage("I'll keep your notes safe. Type your thoughts whenever you're ready.")
            OrbitTab.Calendar -> setRabbitMessage("Let's look ahead. A clean, quiet monthly planner for you.")
            OrbitTab.Bin -> setRabbitMessage("This is our cosmic recycling space. Completed tasks fly away after 48 hours.")
        }
    }

    fun setTaskTabMode(mode: TaskTabMode) {
        _taskTabMode.value = mode
        if (mode == TaskTabMode.BrainDump) {
            setRabbitMessage("Need somewhere to put that thought? Try Brain Dump.")
        } else {
            setRabbitMessage("Your personal list. No pressure, do what you can.")
        }
    }

    fun selectDate(dateString: String) {
        _selectedDate.value = dateString
        setRabbitMessage("Looking at plans for $dateString. Simple and quiet.")
    }

    // --- User Profile ---
    fun registerUser(name: String) {
        viewModelScope.launch {
            repository.saveUserName(name)
            setRabbitMessage("Wonderful to meet you, $name. I am Infinite. Let's explore together!")
        }
    }

    // --- Task CRUD ---
    fun createTask(name: String, note: String?, dueDate: String? = null) {
        viewModelScope.launch {
            val task = TaskEntity(
                name = name,
                note = note,
                dueDate = dueDate
            )
            repository.insertTask(task)
            setRabbitMessage("I saved your task: '$name'. No rush to finish it!")
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = true,
                completedTime = System.currentTimeMillis()
            )
            repository.updateTask(updated)
            
            // Trigger confetti popup
            triggerConfetti()
            
            // Check completed count to trigger smart congrats
            val completedToday = activeTasks.value.count { it.isCompleted } + 1
            if (completedToday > 2) {
                setRabbitMessage("Nice work today, ${userName.value ?: ""}. You've already finished a few things!")
            } else {
                setRabbitMessage("You finished that task! I'll put it in the Bin section.")
            }
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = !task.isCompleted,
                completedTime = if (!task.isCompleted) System.currentTimeMillis() else null
            )
            repository.updateTask(updated)
            if (updated.isCompleted) {
                // Trigger confetti popup
                triggerConfetti()
                setRabbitMessage("Task marked completed. It is safe inRecently Finished.")
            } else {
                setRabbitMessage("Restored task to active list.")
            }
        }
    }

    fun softDeleteTask(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(
                isDeleted = true,
                deletedTime = System.currentTimeMillis()
            )
            repository.updateTask(updated)
            setRabbitMessage("Moved task to the Bin. It'll stay for a while.")
        }
    }

    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = false,
                completedTime = null,
                isDeleted = false,
                deletedTime = null
            )
            repository.updateTask(updated)
            setRabbitMessage("Restored task: '${task.name}'")
        }
    }

    fun permanentlyDeleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            setRabbitMessage("Permanently cleared task.")
        }
    }

    // --- Organizer / Brain Dump (AI Magic) ---
    fun organizeRawParagraph(paragraph: String) {
        if (paragraph.isBlank()) return
        _isOrganizing.value = true
        setRabbitMessage("Infinite is flying through the galaxy to untangle your busy thoughts...")
        
        viewModelScope.launch {
            try {
                val parsedTasks = OrbitGeminiManager.organizeBrainDump(paragraph)
                parsedTasks.forEach { parsed ->
                    val task = TaskEntity(
                        name = parsed.name,
                        note = parsed.note,
                        dueDate = null
                    )
                    repository.insertTask(task)
                }
                _isOrganizing.value = false
                _taskTabMode.value = TaskTabMode.MyList
                setRabbitMessage("Magic! I've organized your brain dump into ${parsedTasks.size} tasks in Tasks tab!")
            } catch (e: Exception) {
                _isOrganizing.value = false
                setRabbitMessage("That was a heavy cluster of stars. I did my best to separate them manually!")
            }
        }
    }

    // --- Note CRUD ---
    fun createNote(title: String, content: String, imageUrl: String? = null, links: String? = null) {
        viewModelScope.launch {
            val note = NoteEntity(
                title = title,
                content = content,
                imageUrl = imageUrl,
                links = links
            )
            repository.insertNote(note)
            setRabbitMessage("I'll keep your new note safe in our cosmic journal.")
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.insertNote(note)
            setRabbitMessage("I updated your entry successfully.")
        }
    }

    fun softDeleteNote(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(
                isDeleted = true,
                deletedTime = System.currentTimeMillis()
            )
            repository.insertNote(updated)
            setRabbitMessage("Note moved to the Bin.")
        }
    }

    fun restoreNote(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(
                isDeleted = false,
                deletedTime = null
            )
            repository.insertNote(updated)
            setRabbitMessage("Restored your note to the journal!")
        }
    }

    fun permanentlyDeleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            setRabbitMessage("Permanently cleared note.")
        }
    }

    // --- Helper Messages ---
    fun setRabbitMessage(message: String) {
        _rabbitMessage.value = message
    }

    fun triggerRandomEncouragement() {
        val expressions = listOf(
            "Take your time, there is absolutely no pressure here.",
            "I trust you, you can do better! I'm always believing in you.",
            "There are billions of stars, and each shines in their own quiet pace.",
            "Did you know? Infinite is very happy when you just open Orbit to breathe.",
            "Are we going to do that chemistry assignment today? Infinite is waiting...",
            "Your thoughts are like nebulae—colorful, beautiful, and sometimes a bit floating.",
            "Need anywhere to put that thought? Try Brain Dump.",
            "I'm always here watching your cozy tasks grow..."
        )
        setRabbitMessage(expressions.random())
    }

    private fun recordAppUsageAndCalculateStreak() {
        viewModelScope.launch {
            val todayStr = getTodayDateString()
            val rawDates = repository.getPreferenceValueDirect("key_usage_dates") ?: ""
            val datesSet = rawDates.split(" ")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableSet()

            // Add today
            datesSet.add(todayStr)

            // Save updated usage dates list
            val updatedRawDates = datesSet.joinToString(" ")
            repository.savePreference("key_usage_dates", updatedRawDates)

            // Update flows
            _usageDates.value = datesSet

            // Calculate streak
            val streak = calculateStreak(datesSet, todayStr)
            _streakCount.value = streak

            repository.savePreference("key_streak_count", streak.toString())
        }
    }

    private fun calculateStreak(usageDates: Set<String>, todayStr: String): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = try {
            sdf.parse(todayStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        val todayCal = Calendar.getInstance().apply {
            time = todayDate
        }

        var streakCount = 0
        val checkCal = Calendar.getInstance().apply {
            time = todayCal.time
        }
        var checkDateStr = sdf.format(checkCal.time)

        if (usageDates.contains(checkDateStr)) {
            // Streak is alive ending today
            while (usageDates.contains(checkDateStr)) {
                streakCount++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
                checkDateStr = sdf.format(checkCal.time)
            }
        } else {
            // Not used today yet. Check if it was used yesterday
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            checkDateStr = sdf.format(checkCal.time)
            if (usageDates.contains(checkDateStr)) {
                // Streak is alive ending yesterday
                while (usageDates.contains(checkDateStr)) {
                    streakCount++
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                    checkDateStr = sdf.format(checkCal.time)
                }
            } else {
                streakCount = 0
            }
        }
        return streakCount
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}

class OrbitViewModelFactory(private val repository: OrbitRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrbitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrbitViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

enum class OrbitTab {
    Tasks, Write, Calendar, Bin
}

enum class TaskTabMode {
    MyList, BrainDump
}
