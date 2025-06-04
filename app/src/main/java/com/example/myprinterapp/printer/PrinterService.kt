package com.example.myprinterapp.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.myprinterapp.data.db.PrintLogEntry
import com.example.myprinterapp.data.repo.PrintLogRepository
import com.example.myprinterapp.data.models.LabelData
import net.posprinter.TSPLConst
import net.posprinter.TSPLPrinter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис для работы с термопринтером.
 * Поддерживает печать этикеток различных форматов с QR-кодом и текстом.
 */
@Singleton
class PrinterService @Inject constructor(
    context: Context,
    private val printRepo: PrintLogRepository
) {
    companion object {
        // Таймаут подключения
        private const val CONNECTION_TIMEOUT_MS = 10000L
        // MAC адрес принтера по умолчанию
        const val DEFAULT_PRINTER_MAC = "10:23:81:5B:DA:29"
    }

    private val connectionManager = PrinterConnection(context)
    private var tsplPrinter: TSPLPrinter? = null

    // Состояние подключения
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // Состояние печати (приватное, т.к. используется только внутри класса)
    private val _printingState = MutableStateFlow(PrintingState.IDLE)

    init {
        connectionManager.init()
    }

    /**
     * Подключение к принтеру по MAC-адресу (асинхронное)
     */
    fun connect(macAddress: String): kotlin.Result<Unit> {
        return try {
            _connectionState.value = ConnectionState.CONNECTING
            Timber.d("Attempting to connect to printer with MAC: $macAddress")

            // Используем асинхронное подключение с колбэками
            connectionManager.connectAsync(
                macAddress = macAddress,
                onSuccess = {
                    // Получаем принтер из connection manager
                    tsplPrinter = connectionManager.getPrinter()
                    _connectionState.value = ConnectionState.CONNECTED
                    Timber.i("Successfully connected to printer: $macAddress")

                    // Тестовый звуковой сигнал для подтверждения подключения
                    try {
                        tsplPrinter?.sound(2, 100)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to play connection sound")
                    }
                },
                onFailure = { errorMessage ->
                    _connectionState.value = ConnectionState.DISCONNECTED
                    Timber.e("Connection failed: $errorMessage")
                }
            )

            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            Timber.e(e, "Connection error to $macAddress")
            kotlin.Result.failure(PrinterException("Ошибка подключения: ${e.message}"))
        }
    }

    /**
     * Отключение от принтера
     */
    fun disconnect() {
        try {
            connectionManager.disconnect()
            tsplPrinter = null
            _connectionState.value = ConnectionState.DISCONNECTED
            Timber.d("Printer disconnected")
        } catch (e: Exception) {
            Timber.e(e, "Error during disconnect")
        }
    }

    /**
     * Основной метод печати этикетки с поддержкой форматов
     */
    suspend fun printLabel(labelData: LabelData, labelType: com.example.myprinterapp.data.models.LabelType = com.example.myprinterapp.data.models.LabelType.STANDARD): kotlin.Result<Unit> {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return kotlin.Result.failure(PrinterException("Принтер не подключен"))
        }

        return try {
            _printingState.value = PrintingState.PRINTING

            // Получаем формат для данного типа этикетки
            val format = getLabelFormat(labelType)

            // Создаем изображение этикетки
            val labelBitmap = format.createBitmap(labelData)

            // Отправляем на печать
            printBitmap(labelBitmap, format)

            // Логируем операцию
            logPrintOperation(labelData)

            _printingState.value = PrintingState.IDLE
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            _printingState.value = PrintingState.ERROR
            Timber.e(e, "Print error")
            kotlin.Result.failure(PrinterException("Ошибка печати: ${e.message}"))
        }
    }

    /**
     * Устаревший метод для обратной совместимости
     */
    suspend fun printLabel(labelData: LabelData): kotlin.Result<Unit> {
        return printLabel(labelData, com.example.myprinterapp.data.models.LabelType.STANDARD)
    }

    /**
     * Получает формат этикетки на основе типа
     */
    private fun getLabelFormat(labelType: com.example.myprinterapp.data.models.LabelType): LabelFormat {
        return when (labelType) {
            com.example.myprinterapp.data.models.LabelType.STANDARD -> AcceptanceLabelFormat57x40()
            com.example.myprinterapp.data.models.LabelType.SMALL -> StandardLabelFormat(40.0, 30.0)
            com.example.myprinterapp.data.models.LabelType.LARGE -> StandardLabelFormat(80.0, 50.0)
            com.example.myprinterapp.data.models.LabelType.CUSTOM -> StandardLabelFormat(60.0, 45.0)
        }
    }

    /**
     * Отправка изображения на принтер используя TSPL команды
     */
    private fun printBitmap(bitmap: Bitmap, format: LabelFormat) {
        val printer = tsplPrinter ?: throw PrinterException("Принтер не инициализирован")

        try {
            Timber.d("Starting print job")

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

            Timber.d("Print job sent successfully")
        } catch (e: Exception) {
            Timber.e(e, "Print bitmap error")
            throw PrinterException("Ошибка печати: ${e.message}")
        }
    }

    /**
     * Логирование операции печати
     */
    private suspend fun logPrintOperation(data: LabelData) {
        val logEntry = PrintLogEntry(
            timestamp = OffsetDateTime.now(),
            operationType = data.labelType ?: "Общая",
            partNumber = data.partNumber,
            partName = data.description ?: data.partName,
            quantity = data.quantity,
            location = data.location ?: data.cellCode,
            orderNumber = data.orderNumber,
            qrData = data.qrData,
            printerStatus = "SUCCESS"
        )

        try {
            printRepo.addPrintLog(logEntry)
        } catch (e: Exception) {
            Timber.e(e, "Failed to log print operation")
        }
    }

    /**
     * Тестирование соединения с принтером
     */
    fun testConnection(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        connectionManager.testConnection(
            onSuccess = {
                Timber.d("Printer connection test successful")
                onSuccess()
            },
            onFailure = { errorMessage ->
                Timber.e("Printer connection test failed: $errorMessage")
                onFailure(errorMessage)
            }
        )
    }
}

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
 * Исключение для ошибок принтера
 */
class PrinterException(message: String) : Exception(message)

/**
 * Стандартная реализация формата этикетки
 */
class StandardLabelFormat(
    override val widthMm: Double,
    override val heightMm: Double,
    override val dpi: Int = 203
) : LabelFormat {
    override fun createBitmap(data: LabelData): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Рисуем базовую этикетку с рамкой
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, widthPx - 1f, heightPx - 1f, borderPaint)

        // Рисуем основную информацию
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
        }

        // Рисуем номер детали
        canvas.drawText("Номер: ${data.partNumber}", 10f, 30f, textPaint)

        // Рисуем название
        val namePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
        }
        canvas.drawText(data.partName.take(20), 10f, 60f, namePaint)

        // Рисуем номер заказа
        canvas.drawText("Заказ: ${data.orderNumber}", 10f, 90f, namePaint)

        return bitmap
    }
}
