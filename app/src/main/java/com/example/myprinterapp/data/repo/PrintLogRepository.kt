package com.example.myprinterapp.data.repo

import com.example.myprinterapp.data.db.PrintLogDao
import com.example.myprinterapp.data.db.PrintLogEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrintLogRepository @Inject constructor(
    private val printLogDao: PrintLogDao
) {
    /**
     * Получить все записи журнала
     */
    fun getAllLogs(): Flow<List<PrintLogEntry>> = printLogDao.getAllLogs()

    /**
     * Получить записи журнала с фильтрацией
     */
    fun getFilteredLogs(
        startDate: Long? = null,
        endDate: Long? = null,
        operationType: String? = null
    ): Flow<List<PrintLogEntry>> {
        return when {
            startDate != null && endDate != null && operationType != null -> {
                printLogDao.getLogsByDateRangeAndType(startDate, endDate, operationType)
            }
            startDate != null && endDate != null -> {
                printLogDao.getLogsByDateRange(startDate, endDate)
            }
            operationType != null -> {
                printLogDao.getLogsByType(operationType)
            }
            else -> getAllLogs()
        }
    }

    /**
     * Добавить запись в журнал
     */
    suspend fun addLog(entry: PrintLogEntry) {
        printLogDao.insertLog(entry)
    }

    /**
     * Добавить несколько записей
     */
    suspend fun addLogs(entries: List<PrintLogEntry>) {
        printLogDao.insertLogs(entries)
    }

    /**
     * Удалить запись
     */
    suspend fun deleteLog(entry: PrintLogEntry) {
        printLogDao.deleteLog(entry)
    }

    /**
     * Удалить все записи
     */
    suspend fun deleteAllLogs() {
        printLogDao.deleteAllLogs()
    }

    /**
     * Получить запись по ID
     */
    suspend fun getLogById(id: Long): PrintLogEntry? {
        return printLogDao.getLogById(id)
    }

    /**
     * Обновить запись
     */
    suspend fun updateLog(entry: PrintLogEntry) {
        printLogDao.updateLog(entry)
    }

    /**
     * Получить количество записей
     */
    suspend fun getLogsCount(): Int {
        return printLogDao.getLogsCount()
    }

    /**
     * Получить последние N записей
     */
    fun getRecentLogs(limit: Int): Flow<List<PrintLogEntry>> {
        return printLogDao.getRecentLogs(limit)
    }

    /**
     * Поиск по тексту
     */
    fun searchLogs(query: String): Flow<List<PrintLogEntry>> {
        return printLogDao.searchLogs("%$query%")
    }
}