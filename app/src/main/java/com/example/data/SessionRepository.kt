package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class SessionRepository(private val focusSessionDao: FocusSessionDao) {
    val allSessions: Flow<List<FocusSession>> = focusSessionDao.getAllSessions()

    fun getTodaySessions(): Flow<List<FocusSession>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return focusSessionDao.getSessionsSince(calendar.timeInMillis)
    }

    suspend fun insert(session: FocusSession) {
        focusSessionDao.insertSession(session)
    }

    suspend fun delete(session: FocusSession) {
        focusSessionDao.deleteSession(session)
    }

    suspend fun deleteById(id: Int) {
        focusSessionDao.deleteSessionById(id)
    }

    suspend fun clearAll() {
        focusSessionDao.clearAll()
    }
}
