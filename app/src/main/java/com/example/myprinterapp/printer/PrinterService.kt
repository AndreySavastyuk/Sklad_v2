package com.example.myprinterapp.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.example.myprinterapp.data.db.PrintLogEntry
import com.example.myprinterapp.data.repo.PrintLogRepository
import net.posprinter.TSPLConst
import net.posprinter.TSPLPrinter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
     * Подключение к принтеру по MAC-адресу
     */
    suspend fun connect(macAddress: String): Result<Unit> {
        return try {
            _connectionState.value = ConnectionState.CONNECTING
            Log.d(TAG, "Attempting to connect to printer with MAC: $macAddress") // Добавленный лог

            val connected = connectionManager.connect(macAddress)
            if (connected) {
                tsplPrinter = TSPLPrinter(connectionManager.getConnection()!!)
                _connectionState.value = ConnectionState.CONNECTED
                Log.i(TAG, "Successfully connected to printer: $macAddress") // Добавленный лог
                Result.success(Unit)
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.w(TAG, "Failed to connect to printer: $macAddress. connectionManager.connect returned false.") // Добавленный лог
                Result.failure(PrinterException("Не удалось подключиться к принтеру"))
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            Log.e(TAG, "Connection error to $macAddress", e) // Улучшенный лог ошибки
            Result.failure(PrinterException("Ошибка подключения: ${e.message}")) // Можно добавить сообщение из исключения
        }
    }

    /**
     * Отключение от принтера
     */
    fun disconnect() {
        connectionManager.getConnection()?.close()
        tsplPrinter = null
        _connectionState.value = ConnectionState.DISCONNECTED
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
            Result.failure(e)
        }
    }

    /**
     * Создание изображения этикетки по макету
     */
    private fun createLabelBitmap(data: LabelData): Bitmap {
        val bitmap = Bitmap.createBitmap(LABEL_WIDTH_PX, LABEL_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Белый фон
        canvas.drawColor(Color.WHITE)

        // Настройки отступов
        val margin = 12f
        val lineSpacing = 4f

        // Шрифты
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create("Arial", Typeface.BOLD)
        }

        val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }

        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }

        var yPos = margin + 24f

        // 1. Номер детали (жирным)
        canvas.drawText("Part: ${data.partNumber}", margin, yPos, titlePaint)
        yPos += titlePaint.textSize + lineSpacing

        // 2. Название детали
        val descLines = wrapText(data.description, normalPaint, LABEL_WIDTH_PX - margin * 2 - 140f)
        descLines.forEach { line ->
            canvas.drawText(line, margin, yPos, normalPaint)
            yPos += normalPaint.textSize + lineSpacing
        }

        // 3. Номер заказа
        yPos += lineSpacing
        canvas.drawText("Order: ${data.orderNumber}", margin, yPos, normalPaint)
        yPos += normalPaint.textSize + lineSpacing * 2

        // 4. Ячейка хранения (внизу слева)
        val bottomY = LABEL_HEIGHT_PX - margin - 10f
        canvas.drawText("Loc: ${data.location}", margin, bottomY, smallPaint)

        // 5. QR-код (справа)
        val qrSize = 120
        val qrX = LABEL_WIDTH_PX - qrSize - margin.toInt()
        val qrY = margin.toInt()

        val qrBitmap = generateQRCode(data.qrData, qrSize)
        canvas.drawBitmap(qrBitmap, qrX.toFloat(), qrY.toFloat(), null)

        // 6. Количество (под QR если есть)
        if (data.quantity != null && data.quantity > 0) {
            val qtyText = "Qty: ${data.quantity}"
            val qtyX = qrX + (qrSize - smallPaint.measureText(qtyText)) / 2
            val qtyY = qrY + qrSize + 16f
            canvas.drawText(qtyText, qtyX, qtyY, smallPaint)
        }

        return bitmap
    }

    /**
     * Генерация QR-кода с использованием ZXing
     */
    private fun generateQRCode(data: String, size: Int): Bitmap {
        return try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(
                data,
                com.google.zxing.BarcodeFormat.QR_CODE,
                size,
                size
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
            // Возвращаем пустой битмап в случае ошибки
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

        return lines
    }

    /**
     * Отправка изображения на принтер
     */
    private fun printBitmap(bitmap: Bitmap) {
        tsplPrinter?.apply {
            try {
                // Очистка буфера
                cls()

                // Установка размера этикетки
                sizeMm(LABEL_WIDTH_MM, LABEL_HEIGHT_MM)

                // Установка зазора между этикетками
                gapMm(2.0, 0.0)

                // Скорость печати
                speed(2.0)

                // Плотность печати
                density(8)

                // Опорная точка
                reference(0, 0)

                // Отправка изображения
                bitmap(0, 0, 0, bitmap.width, bitmap)

                // Печать одной копии
                print(1)

                Log.d(TAG, "Label printed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Print bitmap error", e)
                throw PrinterException("Ошибка печати: ${e.message}")
            }
        } ?: throw PrinterException("Принтер не инициализирован")
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
     * Обработка данных из QR-кода для печати
     */
    fun printFromScannedQR(qrData: String, quantity: String, cellCode: String): Result<Unit> {
        return try {
            // Парсим QR: id=order=part=description
            val parts = qrData.split('=')
            if (parts.size < 4) {
                return Result.failure(PrinterException("Неверный формат QR-кода"))
            }

            val labelData = LabelData(
                partNumber = parts[2],
                description = "${parts[3]} x$quantity",
                orderNumber = parts[1],
                location = cellCode,
                quantity = quantity.toIntOrNull() ?: 0,
                qrData = qrData,
                labelType = "Приемка"
            )

            CoroutineScope(Dispatchers.IO).launch {
                printLabel(labelData)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
    val labelType: String = "Общая"
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
 * Исключение для ошибок принтера
 */
class PrinterException(message: String) : Exception(message)

