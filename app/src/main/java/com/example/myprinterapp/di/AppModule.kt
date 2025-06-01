package com.example.myprinterapp.di

import android.content.Context
import androidx.room.Room
import com.example.myprinterapp.data.db.AppDatabase
import com.example.myprinterapp.data.repo.*
import com.example.myprinterapp.domain.usecase.*
import com.example.myprinterapp.printer.PrinterManager
import com.example.myprinterapp.printer.PrinterManagerImpl
import com.example.myprinterapp.printer.PrinterService
import com.example.myprinterapp.data.PrinterSettings
import com.example.myprinterapp.scanner.ScannerManager
import com.example.myprinterapp.scanner.ScannerManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.example.myprinterapp.data.db.PrintLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "printer_app_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun providePrintLogDao(database: AppDatabase) = database.printLogDao()

    @Provides
    @Singleton
    fun providePrintLogRepository(database: AppDatabase): PrintLogRepository {
        return PrintLogRepositoryImpl(database.printLogDao())
    }

    @Provides
    @Singleton
    fun provideScanHistoryRepository(@ApplicationContext context: Context): ScanHistoryRepository {
        return ScanHistoryRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }

    // Настройки принтера
    @Provides
    @Singleton
    fun providePrinterSettings(@ApplicationContext context: Context): PrinterSettings {
        return PrinterSettings(context)
    }

    // Новый PrinterService с поддержкой POSConnect SDK
    @Provides
    @Singleton
    fun providePrinterService(
        @ApplicationContext context: Context,
        printLogRepository: PrintLogRepository
    ): PrinterService {
        return PrinterService(context, printLogRepository)
    }

    // Диспетчеры корутин
    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @MainDispatcher
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @DefaultDispatcher
    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    // Менеджеры устройств
    @Provides
    @Singleton
    fun providePrinterManager(@ApplicationContext context: Context): PrinterManager {
        return PrinterManagerImpl(context)
    }

    @Provides
    @Singleton
    fun provideScannerManager(@ApplicationContext context: Context): ScannerManager {
        return ScannerManagerImpl(context)
    }

    // QR парсер
    @Provides
    @Singleton
    fun provideQrCodeParser(): QrCodeParser {
        return QrCodeParserImpl()
    }

    // Use Cases
    @Provides
    @Singleton
    fun provideProcessScanResultUseCase(
        scanHistoryRepository: ScanHistoryRepository,
        qrParser: QrCodeParser,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): ProcessScanResultUseCase {
        return ProcessScanResultUseCase(scanHistoryRepository, qrParser, dispatcher)
    }

    @Provides
    @Singleton
    fun providePrintLabelUseCase(
        printerManager: PrinterManager,
        printLogRepository: PrintLogRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): PrintLabelUseCase {
        return PrintLabelUseCase(printerManager, printLogRepository, dispatcher)
    }

    @Provides
    @Singleton
    fun provideGetPrintHistoryUseCase(
        repository: PrintLogRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): GetPrintHistoryUseCase {
        return GetPrintHistoryUseCase(repository, dispatcher)
    }

    @Provides
    @Singleton
    fun provideGetScanHistoryUseCase(
        repository: ScanHistoryRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): GetScanHistoryUseCase {
        return GetScanHistoryUseCase(repository, dispatcher)
    }

    @Provides
    @Singleton
    fun provideConnectPrinterUseCase(
        printerManager: PrinterManager,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): ConnectPrinterUseCase {
        return ConnectPrinterUseCase(printerManager, dispatcher)
    }

    @Provides
    @Singleton
    fun provideConnectScannerUseCase(
        scannerManager: ScannerManager,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): ConnectScannerUseCase {
        return ConnectScannerUseCase(scannerManager, dispatcher)
    }

    @Provides
    @Singleton
    fun provideGetAvailablePrintersUseCase(
        printerManager: PrinterManager,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): GetAvailablePrintersUseCase {
        return GetAvailablePrintersUseCase(printerManager, dispatcher)
    }

    @Provides
    @Singleton
    fun provideGetAvailableScannersUseCase(
        scannerManager: ScannerManager,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): GetAvailableScannersUseCase {
        return GetAvailableScannersUseCase(scannerManager, dispatcher)
    }

    @Provides
    @Singleton
    fun provideReprintLabelUseCase(
        printLabelUseCase: PrintLabelUseCase,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): ReprintLabelUseCase {
        return ReprintLabelUseCase(printLabelUseCase, dispatcher)
    }
}

// Квалификаторы для диспетчеров
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

// Реализации репозиториев
class ScanHistoryRepositoryImpl(context: Context) : ScanHistoryRepository {
    override suspend fun saveScanResult(scanResult: com.example.myprinterapp.data.models.ScanResult) {
        // TODO: Реализовать сохранение в локальную БД
    }

    override fun getScanHistory(): kotlinx.coroutines.flow.Flow<List<com.example.myprinterapp.data.models.ScanResult>> {
        // TODO: Реализовать получение истории из БД
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }

    override suspend fun clearHistory() {
        // TODO: Реализовать очистку истории
    }
}

class SettingsRepositoryImpl(context: Context) : SettingsRepository {
    override suspend fun getAppSettings(): com.example.myprinterapp.data.models.AppSettings {
        // TODO: Реализовать получение настроек из DataStore
        return com.example.myprinterapp.data.models.AppSettings(
            printerSettings = com.example.myprinterapp.data.models.PrinterSettings(),
            scannerSettings = com.example.myprinterapp.data.models.ScannerSettings(),
            uiSettings = com.example.myprinterapp.data.models.UiSettings(),
            networkSettings = com.example.myprinterapp.data.models.NetworkSettings()
        )
    }

    override suspend fun updateAppSettings(settings: com.example.myprinterapp.data.models.AppSettings) {
        // TODO: Реализовать сохранение настроек в DataStore
    }

    override suspend fun getPrinterSettings(): com.example.myprinterapp.data.models.PrinterSettings {
        return com.example.myprinterapp.data.models.PrinterSettings()
    }

    override suspend fun updatePrinterSettings(settings: com.example.myprinterapp.data.models.PrinterSettings) {
        // TODO: Реализовать обновление настроек принтера
    }

    override suspend fun getScannerSettings(): com.example.myprinterapp.data.models.ScannerSettings {
        return com.example.myprinterapp.data.models.ScannerSettings()
    }

    override suspend fun updateScannerSettings(settings: com.example.myprinterapp.data.models.ScannerSettings) {
        // TODO: Реализовать обновление настроек сканера
    }
}

class PrintLogRepositoryImpl(
    private val dao: com.example.myprinterapp.data.db.PrintLogDao
) : PrintLogRepository {
    override suspend fun addPrintLog(entry: PrintLogEntry) {
        dao.insertLog(entry)
    }

    override fun logFlow(): Flow<List<PrintLogEntry>> {
        return dao.getAllLogs()
    }

    override suspend fun getAllLogs(): List<PrintLogEntry> {
        return dao.getAllLogsSync()
    }

    override suspend fun deleteLog(entry: PrintLogEntry) {
        dao.deleteLog(entry)
    }

    override suspend fun deleteAllLogs() {
        dao.deleteAllLogs()
    }
}

class QrCodeParserImpl : QrCodeParser {
    override suspend fun parseForAcceptance(data: String): ParsedData {
        return try {
            val parts = data.split('=')
            if (parts.size >= 4) {
                ParsedData.AcceptanceData(
                    routeCardNumber = parts[0],
                    partNumber = parts[1],
                    partName = parts[2],
                    orderNumber = parts.getOrNull(3) ?: "",
                    quantity = parts.getOrNull(4)?.toIntOrNull()
                )
            } else {
                ParsedData.Error("Неверный формат QR-кода для приемки", data)
            }
        } catch (e: Exception) {
            ParsedData.Error("Ошибка парсинга: ${e.message}", data)
        }
    }

    override suspend fun parseForPicking(data: String): ParsedData {
        // TODO: Реализовать парсинг для комплектации
        return ParsedData.GeneralData(data, "picking")
    }

    override suspend fun parseGeneral(data: String): ParsedData {
        return ParsedData.GeneralData(data, "general")
    }
}
