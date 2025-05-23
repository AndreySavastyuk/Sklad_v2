package com.example.myprinterapp.printer

import android.content.Context
import android.graphics.*
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.example.myprinterapp.data.db.PrintLogEntry
import com.example.myprinterapp.data.repo.PrintLogRepository
import net.posprinter.TSPLConst
import net.posprinter.TSPLPrinter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис для работы с термопринтером.
 * Поддерживает печать этикеток различных форматов с QR-кодом и текстом.
 */
@Singleton
class PrinterService @Inject constructor(
    private val context: Context,
    private val printRepo: PrintLogRepository
) {
    companion object {
        private const val TAG = "PrinterService"
        // Таймаут подключения
        private const val CONNECTION_TIMEOUT_MS = 10000L
    }

    private val connectionManager = PrinterConnection(context)
    private var tsplPrinter: TSPLPrinter? = null

    // Состояние подключения
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // Состояние печати
    private val _printingState = MutableStateFlow(PrintingState.IDLE)
    val printingState: StateFlow<PrintingState> = _printingState

    init {
        connectionManager.init()
    }

    /**
     * Подключение к принтеру по MAC-адресу с таймаутом
     */
    suspend fun connect(macAddress: String): Result<Unit> {
        return try {
            _connectionState.value = ConnectionState.CONNECTING
            Log.d(TAG, "Attempting to connect to printer with MAC: $macAddress")

            val connected = withTimeout(CONNECTION_TIMEOUT_MS) {
                connectionManager.connectAsync(macAddress)
            }

            if (connected) {
                val deviceConnection = connectionManager.getConnection()
                if (deviceConnection != null) {
                    tsplPrinter = TSPLPrinter(deviceConnection)
                    _connectionState.value = ConnectionState.CONNECTED
                    Log.i(TAG, "Successfully connected to printer: $macAddress")

                    // Тестовый звуковой сигнал для подтверждения подключения
                    try {
                        tsplPrinter?.sound(2, 100)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to play connection sound", e)
                    }

                    Result.success(Unit)
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    Log.e(TAG, "Connection object is null after successful connect")
                    Result.failure(PrinterException("Ошибка инициализации принтера"))
                }
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.w(TAG, "Failed to connect to printer: $macAddress")
                Result.failure(PrinterException("Не удалось подключиться к принтеру"))
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            _connectionState.value = ConnectionState.DISCONNECTED
            Log.e(TAG, "Connection timeout for $macAddress")
            Result.failure(PrinterException("Превышено время ожидания подключения"))
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            Log.e(TAG, "Connection error to $macAddress", e)
            Result.failure(PrinterException("Ошибка подключения: ${e.message}"))
        }
    }

    /**
     * Отключение от принтера
     */
    fun disconnect() {
        try {
            connectionManager.close()
            tsplPrinter = null
            _connectionState.value = ConnectionState.DISCONNECTED
            Log.d(TAG, "Printer disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }

    /**
     * Основной метод печати этикетки с поддержкой форматов
     */
    suspend fun printLabel(labelData: LabelData, labelType: LabelType = LabelType.ACCEPTANCE_57x40): Result<Unit> {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return Result.failure(PrinterException("Принтер не подключен"))
        }

        return try {
            _printingState.value = PrintingState.PRINTING

            // Получаем формат
            val format = labelType.getFormat()

            // Создаем изображение этикетки
            val labelBitmap = format.createBitmap(labelData)

            // Отправляем на печать
            printBitmap(labelBitmap, format)

            // Логируем операцию
            logPrintOperation(labelData)

            _printingState.value = PrintingState.IDLE
            Result.success(Unit)
        } catch (e: Exception) {
            _printingState.value = PrintingState.ERROR
            Log.e(TAG, "Print error", e)
            Result.failure(PrinterException("Ошибка печати: ${e.message}"))
        }
    }

    /**
     * Устаревший метод для обратной совместимости
     */
    suspend fun printLabel(labelData: LabelData): Result<Unit> {
        return printLabel(labelData, LabelType.ACCEPTANCE_57x40)
    }

    /**
     * Отправка изображения на принтер используя TSPL команды
     */
    private fun printBitmap(bitmap: Bitmap, format: LabelFormat) {
        val printer = tsplPrinter ?: throw PrinterException("Принтер не инициализирован")

        try {
            Log.d(TAG, "Starting print job")

            // Настройки принтера
            printer.cls()
            printer.sizeMm(format.widthMm, format.heightMm)
            printer.gapMm(2.0, 0.0)
            printer.speed(2.0)
            printer.density(8)
            printer.direction(TSPLConst.DIRECTION_FORWARD)
            printer.reference(0, 0)
            printer.cls()

            // Отправка изображения
            printer.bitmap(0, 0, TSPLConst.BMP_MODE_OVERWRITE, bitmap.width, bitmap)

            // Печать
            printer.print(1)
            printer.sound(1, 50)

            Log.d(TAG, "Print job sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Print bitmap error", e)
            throw PrinterException("Ошибка печати: ${e.message}")
        }
    }

    /**
     * Логирование операции печати
     */
    private suspend fun logPrintOperation(data: LabelData) {
        val logEntry = PrintLogEntry(
            dateTime = OffsetDateTime.now(),
            labelType = data.labelType,
            partNumber = data.partNumber,
            orderNumber = data.orderNumber,
            quantity = data.quantity,
            cellCode = data.location,
            qrData = data.qrData
        )

        printRepo.add(logEntry)
    }

    /**
     * Проверка состояния принтера
     */
    suspend fun checkPrinterStatus(): Result<PrinterStatus> {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return Result.failure(PrinterException("Принтер не подключен"))
        }

        return Result.success(PrinterStatus.READY)
    }
}

/**
 * Модель данных для этикетки
 */
data class LabelData(
    val partNumber: String,
    val description: String,
    val orderNumber: String,
    val location: String,
    val quantity: Int? = null,
    val qrData: String,
    val labelType: String = "Общая",
    val acceptanceDate: String? = null // Дата приемки
)

/**
 * Состояния подключения принтера
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/**
 * Состояния процесса печати
 */
enum class PrintingState {
    IDLE,
    PRINTING,
    ERROR
}

/**
 * Статус принтера
 */
enum class PrinterStatus {
    READY,
    BUSY,
    ERROR,
    PAPER_OUT,
    COVER_OPEN
}

/**
 * Исключение для ошибок принтера
 */
class PrinterException(message: String) : Exception(message)