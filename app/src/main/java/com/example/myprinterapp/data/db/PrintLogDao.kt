package com.example.myprinterapp.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintLogDao {
    @Query("SELECT * FROM print_log ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<PrintLogEntry>>

    @Query("SELECT * FROM print_log WHERE id = :id")
    suspend fun getLogById(id: Long): PrintLogEntry?

    @Query("SELECT * FROM print_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<PrintLogEntry>>

    @Query("SELECT * FROM print_log WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getLogsByDateRange(startDate: Long, endDate: Long): Flow<List<PrintLogEntry>>

    @Query("SELECT * FROM print_log WHERE operationType = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: String): Flow<List<PrintLogEntry>>

    @Query("SELECT * FROM print_log WHERE timestamp BETWEEN :startDate AND :endDate AND operationType = :type ORDER BY timestamp DESC")
    fun getLogsByDateRangeAndType(startDate: Long, endDate: Long, type: String): Flow<List<PrintLogEntry>>

    @Query("SELECT * FROM print_log WHERE partNumber LIKE :query OR partName LIKE :query OR orderNumber LIKE :query ORDER BY timestamp DESC")
    fun searchLogs(query: String): Flow<List<PrintLogEntry>>

    @Query("SELECT COUNT(*) FROM print_log")
    suspend fun getLogsCount(): Int

    @Insert
    suspend fun insertLog(log: PrintLogEntry)

    @Insert
    suspend fun insertLogs(logs: List<PrintLogEntry>)

    @Update
    suspend fun updateLog(log: PrintLogEntry)

    @Delete
    suspend fun deleteLog(log: PrintLogEntry)

    @Query("DELETE FROM print_log")
    suspend fun deleteAllLogs()
}