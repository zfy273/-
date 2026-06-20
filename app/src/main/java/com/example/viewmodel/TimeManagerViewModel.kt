package com.example.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FocusSession
import com.example.data.SessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TimeManagerViewModel(private val repository: SessionRepository) : ViewModel() {

    // --- Database State ---
    val allSessions: StateFlow<List<FocusSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todaySessions: StateFlow<List<FocusSession>> = repository.getTodaySessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Interactive Timer State ---
    var totalDurationSeconds by mutableStateOf(1500) // Default: 25 minutes
        private set

    var secondsRemaining by mutableStateOf(1500)
        private set

    var isTimerRunning by mutableStateOf(false)
        private set

    var isTimerPaused by mutableStateOf(false)
        private set

    var selectedCategory by mutableStateOf("工作") // Default: 工作 (Work)
        private set

    var currentNote by mutableStateOf("")
        private set

    // Celebration/Finish State
    var showCelebrationDialog by mutableStateOf(false)
    var lastCompletedSessionDuration by mutableStateOf(0)
        private set

    private var timerJob: Job? = null

    // Predefined Categories
    val categories = listOf("工作", "学习", "运动", "创意", "生活", "备忘")

    // --- Timer Actions ---
    fun selectCategory(category: String) {
        selectedCategory = category
    }

    fun updateNote(note: String) {
        currentNote = note
    }

    fun setTimerDuration(minutes: Int) {
        if (isTimerRunning) return
        val seconds = minutes * 60
        totalDurationSeconds = seconds
        secondsRemaining = seconds
        isTimerPaused = false
    }

    fun startTimer() {
        if (isTimerRunning) return
        isTimerRunning = true
        isTimerPaused = false
        
        timerJob = viewModelScope.launch {
            while (secondsRemaining > 0) {
                delay(1000L)
                secondsRemaining--
            }
            onTimerComplete()
        }
    }

    fun pauseTimer() {
        if (!isTimerRunning) return
        isTimerRunning = false
        isTimerPaused = true
        timerJob?.cancel()
    }

    fun resumeTimer() {
        if (isTimerRunning) return
        startTimer()
    }

    fun resetTimer() {
        timerJob?.cancel()
        isTimerRunning = false
        isTimerPaused = false
        secondsRemaining = totalDurationSeconds
    }

    private fun onTimerComplete() {
        timerJob?.cancel()
        isTimerRunning = false
        isTimerPaused = false
        
        val duration = totalDurationSeconds
        val category = selectedCategory
        val note = currentNote

        val session = FocusSession(
            category = category,
            durationSeconds = duration,
            note = note.ifBlank { "完成了一次专注" }
        )

        viewModelScope.launch {
            repository.insert(session)
            lastCompletedSessionDuration = duration
            showCelebrationDialog = true
            // reset note & timer
            currentNote = ""
            secondsRemaining = totalDurationSeconds
        }
    }

    // --- Manual Session Entry ---
    fun logManualSession(category: String, minutes: Int, note: String) {
        if (minutes <= 0) return
        val session = FocusSession(
            category = category,
            durationSeconds = minutes * 60,
            note = note.ifBlank { "手动记录专注时间" }
        )
        viewModelScope.launch {
            repository.insert(session)
        }
    }

    // --- Data Management Actions ---
    fun deleteSession(session: FocusSession) {
        viewModelScope.launch {
            repository.delete(session)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // --- Google Schedule Layout Planners ---
    fun logPlannedSession(category: String, durationMinutes: Int, note: String, dateOffsetDays: Int, hourOfDay: Int) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, dateOffsetDays)
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val plannedSession = FocusSession(
            category = category,
            durationSeconds = -durationMinutes * 60, // Negative duration flags a planned/scheduled item
            note = note.ifBlank { "待完成计划" },
            timestamp = calendar.timeInMillis
        )
        viewModelScope.launch {
            repository.insert(plannedSession)
        }
    }

    fun completePlannedSession(session: FocusSession) {
        val completedSession = session.copy(
            durationSeconds = kotlin.math.abs(session.durationSeconds), // turn positive representing completion
            timestamp = System.currentTimeMillis() // tag completion as today
        )
        viewModelScope.launch {
            repository.insert(completedSession)
            lastCompletedSessionDuration = completedSession.durationSeconds
            showCelebrationDialog = true
        }
    }

    fun populateDemoPlans() {
        viewModelScope.launch {
            // Check if there are already sessions
            val currentList = allSessions.value
            if (currentList.isEmpty()) {
                // Prepopulate 3 lovely plans for today to give Google Schedule style immediate live content
                logPlannedSession(
                    category = "工作",
                    durationMinutes = 45,
                    note = "UI 设计与原型探索",
                    dateOffsetDays = 0,
                    hourOfDay = 9
                )
                logPlannedSession(
                    category = "运动",
                    durationMinutes = 30,
                    note = "每日有氧激活拉伸",
                    dateOffsetDays = 0,
                    hourOfDay = 14
                )
                logPlannedSession(
                    category = "学习",
                    durationMinutes = 40,
                    note = "深入阅读一个技术章节",
                    dateOffsetDays = 0,
                    hourOfDay = 19
                )
                logPlannedSession(
                    category = "生活",
                    durationMinutes = 15,
                    note = "冲煮一杯精品手冲咖啡",
                    dateOffsetDays = 0,
                    hourOfDay = 16
                )
            }
        }
    }
}

// Factory class to instantiate ViewModel with dependency
class TimeManagerViewModelFactory(private val repository: SessionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimeManagerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimeManagerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
