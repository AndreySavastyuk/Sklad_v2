package com.example.myprinterapp.data.repo

import com.example.myprinterapp.data.db.PrintLogDao
import com.example.myprinterapp.data.db.PrintLogEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrintLogRepository @Inject constructor(
    private val dao: PrintLogDao
) {
    fun logFlow(): Flow<List<PrintLogEntry>> = dao.getAll()

    suspend fun add(entry: PrintLogEntry) = dao.insert(entry)

    suspend fun clear() = dao.clear()
}