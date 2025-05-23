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
 * Поддерживает печать этикеток 57x40 мм с QR-кодом и текстом.
 */
@Singleton
class PrinterService @Inject constructor(
    private val context: Context,
    private val printRepo: PrintLogRepository
) {
    companion object {
        private const val TAG = "PrinterService"
        // Размеры этикетки в мм
        private const val LABEL_WIDTH_MM = 57.0
        private const val LABEL_HEIGHT_MM = 40.0
        // DPI принтера (типичное значение для термопринтеров)
        private const val PRINTER_DPI = 203
        // Размеры в пикселях
        private const val LABEL_WIDTH_PX = (LABEL_WIDTH_MM / 25.4 * PRINTER_DPI).toInt()  // ~456px
        private const val LABEL_HEIGHT_PX = (LABEL_HEIGHT_MM / 25.4 * PRINTER_DPI).toInt() // ~320px
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
     * Основной метод печати этикетки
     */
    suspend fun printLabel(labelData: LabelData): Result<Unit> {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return Result.failure(PrinterException("Принтер не подключен"))
        }

        return try {
            _printingState.value = PrintingState.PRINTING

            // Создаем изображение этикетки
            val labelBitmap = createLabelBitmap(labelData)

            // Отправляем на печать
            printBitmap(labelBitmap)

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
     * Создание изображения этикетки по новому макету
     */
    private fun createLabelBitmap(data: LabelData): Bitmap {
        val bitmap = Bitmap.createBitmap(LABEL_WIDTH_PX, LABEL_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Белый фон
        canvas.drawColor(Color.WHITE)

        // Настройки отступов
        val margin = 10f
        val lineSpacing = 3f

        // Шрифты (приближенные к макету)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create("Arial", Typeface.BOLD)
        }

        val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }

        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create("Arial", Typeface.BOLD)
        }

        // Рамка вокруг этикетки
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, LABEL_WIDTH_PX - 1f, LABEL_HEIGHT_PX - 1f, borderPaint)

        var yPos = margin + 16f

        // 1. Заголовок "ПРИЕМКА" по центру
        val headerText = "ПРИЕМКА"
        val headerWidth = headerPaint.measureText(headerText)
        canvas.drawText(headerText, (LABEL_WIDTH_PX - headerWidth) / 2, yPos, headerPaint)
        yPos += headerPaint.textSize + lineSpacing + 2

        // Горизонтальная линия после заголовка
        canvas.drawLine(margin, yPos, LABEL_WIDTH_PX - margin, yPos, borderPaint)
        yPos += 8f

        // 2. Номер заказа и маршрутки
        canvas.drawText("Заказ: ${data.orderNumber}", margin, yPos, normalPaint)
        yPos += normalPaint.textSize + lineSpacing

        // Извлекаем номер маршрутки из QR кода
        val routeCardNumber = data.qrData.split('=').firstOrNull() ?: ""
        canvas.drawText("М/К: $routeCardNumber", margin, yPos, normalPaint)
        yPos += normalPaint.textSize + lineSpacing + 4

        // 3. Номер детали (крупным шрифтом)
        canvas.drawText("Деталь: ${data.partNumber}", margin, yPos, boldPaint)
        yPos += boldPaint.textSize + lineSpacing

        // 4. Название детали (с переносом если длинное)
        val descLines = wrapText(data.description, normalPaint, LABEL_WIDTH_PX - margin * 2 - 140f)
        descLines.forEach { line ->
            canvas.drawText(line, margin, yPos, normalPaint)
            yPos += normalPaint.textSize + lineSpacing
        }

        // 5. QR-код справа
        val qrSize = 110
        val qrX = LABEL_WIDTH_PX - qrSize - margin.toInt()
        val qrY = 65

        // Генерируем QR с поддержкой кириллицы
        val qrBitmap = generateQRCodeWithCyrillic(data.qrData, qrSize)
        canvas.drawBitmap(qrBitmap, qrX.toFloat(), qrY.toFloat(), null)

        // 6. Количество (справа от названия, под QR)
        if (data.quantity != null && data.quantity > 0) {
            val qtyText = "Кол-во: ${data.quantity} шт"
            val qtyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.create("Arial", Typeface.BOLD)
            }
            val qtyX = qrX + (qrSize - qtyPaint.measureText(qtyText)) / 2
            val qtyY = qrY + qrSize + 12f
            canvas.drawText(qtyText, qtyX, qtyY, qtyPaint)
        }

        // 7. Внизу: ячейка и дата приемки
        val bottomY1 = LABEL_HEIGHT_PX - margin - 20f
        val bottomY2 = LABEL_HEIGHT_PX - margin - 5f

        // Ячейка слева
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create("Arial", Typeface.BOLD)
        }
        canvas.drawText("Ячейка: ${data.location}", margin, bottomY1, cellPaint)

        // Дата приемки справа
        if (data.acceptanceDate != null) {
            val dateText = "Дата: ${data.acceptanceDate}"
            val dateWidth = smallPaint.measureText(dateText)
            canvas.drawText(dateText, LABEL_WIDTH_PX - dateWidth - margin, bottomY2, smallPaint)
        }

        return bitmap
    }

    /**
     * Генерация QR-кода с поддержкой кириллицы
     */
    private fun generateQRCodeWithCyrillic(data: String, size: Int): Bitmap {
        return try {
            val writer = QRCodeWriter()
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
            hints[EncodeHintType.MARGIN] = 1
            
            val bitMatrix = writer.encode(
                data,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error generating QR code", e)
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
        }
    }

    /**
     * Перенос текста на новые строки
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines.take(2) // Максимум 2 строки для названия
    }

    /**
     * Отправка изображения на принтер используя TSPL команды
     */
    private fun printBitmap(bitmap: Bitmap) {
        val printer = tsplPrinter ?: throw PrinterException("Принтер не инициализирован")
        
        try {
            Log.d(TAG, "Starting print job")
            
            // Настройки принтера
            printer.cls()
            printer.sizeMm(LABEL_WIDTH_MM, LABEL_HEIGHT_MM)
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
