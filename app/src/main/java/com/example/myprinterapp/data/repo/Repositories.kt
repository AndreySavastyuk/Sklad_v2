package com.example.myprinterapp.data.repo

import com.example.myprinterapp.data.models.*
import com.example.myprinterapp.data.db.PrintLogEntry
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для работы с историей сканирования
 */
interface ScanHistoryRepository {
    suspend fun saveScanResult(scanResult: ScanResult)
    fun getScanHistory(): Flow<List<ScanResult>>
    suspend fun clearHistory()
}

/**
 * Репозиторий для работы с настройками приложения
 */
interface SettingsRepository {
    suspend fun getAppSettings(): AppSettings
    suspend fun updateAppSettings(settings: AppSettings)
    suspend fun getPrinterSettings(): PrinterSettings
    suspend fun updatePrinterSettings(settings: PrinterSettings)
    suspend fun getScannerSettings(): ScannerSettings
    suspend fun updateScannerSettings(settings: ScannerSettings)
}

/**
 * Репозиторий для работы с журналом печати (совместимость со старым кодом)
 */
interface PrintLogRepository {
    suspend fun addPrintLog(entry: PrintLogEntry)
    fun logFlow(): Flow<List<PrintLogEntry>>
    suspend fun getAllLogs(): List<PrintLogEntry>
    suspend fun deleteLog(entry: PrintLogEntry)
    suspend fun deleteAllLogs()
} 