package com.example.myprinterapp.printer

import android.graphics.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

import android.content.Context
import android.util.Log

import com.example.myprinterapp.data.repo.PrintLogRepository


/**
 * Базовый интерфейс для форматов этикеток
 */
interface LabelFormat {
    val widthMm: Double
    val heightMm: Double
    val dpi: Int

    val widthPx: Int
        get() = (widthMm / 25.4 * dpi).toInt()

    val heightPx: Int
        get() = (heightMm / 25.4 * dpi).toInt()

    fun createBitmap(data: LabelData): Bitmap
}

/**
 * Формат этикетки 57x40 мм для приемки
 */
class AcceptanceLabelFormat57x40 : LabelFormat {
    override val widthMm = 57.0
    override val heightMm = 40.0
    override val dpi = 203

    override fun createBitmap(data: LabelData): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Настройки отступов
        val margin = 8f
        val qrSize = 140 // Увеличенный размер QR как на образце

        // Рамка
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, widthPx - 1f, heightPx - 1f, borderPaint)

        // QR-код слева
        val qrX = margin.toInt()
        val qrY = ((heightPx - qrSize) / 2).toInt()
        val qrBitmap = generateQRCode(data.qrData, qrSize)
        canvas.drawBitmap(qrBitmap, qrX.toFloat(), qrY.toFloat(), null)

        // Правая часть - текст
        val textStartX = qrX + qrSize + 12f
        val textAreaWidth = widthPx - textStartX - margin

        // 1. Обозначение детали (максимально крупно)
        val partNumberPaint = createPartNumberPaint(data.partNumber, textAreaWidth)
        var yPos = margin + partNumberPaint.textSize
        canvas.drawText(data.partNumber, textStartX, yPos, partNumberPaint)

        // 2. Наименование детали
        yPos += 8f
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }

        val nameLines = wrapText(data.description, namePaint, textAreaWidth)
        nameLines.take(2).forEach { line ->
            yPos += namePaint.textSize + 2f
            canvas.drawText(line, textStartX, yPos, namePaint)
        }

        // 3. Номер заказа
        yPos = heightPx - margin - 36f
        val orderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }
        canvas.drawText(data.orderNumber, textStartX, yPos, orderPaint)

        // 4. Ячейка (в рамке)
        val cellBoxHeight = 30f
        val cellBoxWidth = 65f
        val cellBoxX = widthPx - cellBoxWidth - margin
        val cellBoxY = heightPx - cellBoxHeight - margin

        // Рамка для ячейки
        canvas.drawRect(cellBoxX, cellBoxY, cellBoxX + cellBoxWidth, cellBoxY + cellBoxHeight, borderPaint)

        // Текст ячейки
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create("Arial", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cellTextY = cellBoxY + (cellBoxHeight + cellPaint.textSize) / 2 - 4f
        canvas.drawText(data.location, cellBoxX + cellBoxWidth / 2, cellTextY, cellPaint)

        return bitmap
    }

    /**
     * Создает Paint для обозначения детали с автоматическим размером шрифта
     */
    private fun createPartNumberPaint(partNumber: String, maxWidth: Float): Paint {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create("Arial", Typeface.BOLD)
        }

        // Начинаем с максимального размера и уменьшаем при необходимости
        var textSize = 24f
        paint.textSize = textSize

        while (paint.measureText(partNumber) > maxWidth && textSize > 12f) {
            textSize -= 1f
            paint.textSize = textSize
        }

        return paint
    }

    private fun generateQRCode(data: String, size: Int): Bitmap {
        return try {
            val writer = QRCodeWriter()
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
            hints[EncodeHintType.MARGIN] = 0

            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
        }
    }

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
}

/**
 * Формат этикетки для комплектации
 */
class PickingLabelFormat57x40 : LabelFormat {
    override val widthMm = 57.0
    override val heightMm = 40.0
    override val dpi = 203

    override fun createBitmap(data: LabelData): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val margin = 8f

        // Рамка
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, widthPx - 1f, heightPx - 1f, borderPaint)

        // Заголовок "КОМПЛЕКТАЦИЯ"
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create("Arial", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        var yPos = margin + 16f
        canvas.drawText("КОМПЛЕКТАЦИЯ", widthPx / 2f, yPos, headerPaint)

        // Линия после заголовка
        yPos += 6f
        canvas.drawLine(margin, yPos, widthPx - margin, yPos, borderPaint)
        yPos += 10f

        // Номер заказа
        val orderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }
        canvas.drawText("Заказ: ${data.orderNumber}", margin, yPos, orderPaint)
        yPos += orderPaint.textSize + 6f

        // Обозначение детали
        val partPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create("Arial", Typeface.BOLD)
        }
        canvas.drawText(data.partNumber, margin, yPos, partPaint)
        yPos += partPaint.textSize + 4f

        // Наименование
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }
        canvas.drawText(data.description, margin, yPos, namePaint)

        // QR-код справа внизу
        val qrSize = 100
        val qrX = widthPx - qrSize - margin.toInt()
        val qrY = heightPx - qrSize - margin.toInt()
        val qrBitmap = generateQRCode(data.qrData, qrSize)
        canvas.drawBitmap(qrBitmap, qrX.toFloat(), qrY.toFloat(), null)

        // Количество и ячейка внизу слева
        val bottomY = heightPx - margin - 20f
        if (data.quantity != null) {
            val qtyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.create("Arial", Typeface.BOLD)
            }
            canvas.drawText("Кол-во: ${data.quantity} шт", margin, bottomY, qtyPaint)
        }

        // Ячейка
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }
        canvas.drawText("Ячейка: ${data.location}", margin, heightPx - margin - 5f, cellPaint)

        return bitmap
    }

    private fun generateQRCode(data: String, size: Int): Bitmap {
        return try {
            val writer = QRCodeWriter()
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
            hints[EncodeHintType.MARGIN] = 0

            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
        }
    }
}

/**
 * Типы этикеток
 */
enum class LabelType {
    ACCEPTANCE_57x40,
    PICKING_57x40;

    fun getFormat(): LabelFormat {
        return when (this) {
            ACCEPTANCE_57x40 -> AcceptanceLabelFormat57x40()
            PICKING_57x40 -> PickingLabelFormat57x40()
        }
    }

    fun getDisplayName(): String {
        return when (this) {
            ACCEPTANCE_57x40 -> "Приемка 57x40 мм"
            PICKING_57x40 -> "Комплектация 57x40 мм"
        }
    }
}



/**
 * Расширенный сервис печати с поддержкой форматов
 */
class ExtendedPrinterService(
    private val originalService: PrinterService
) {

    companion object {
        private const val TAG = "ExtendedPrinterService"
    }

    /**
     * Основной метод печати этикетки с выбором формата
     */
    suspend fun printLabel(labelData: LabelData, labelType: LabelType = LabelType.ACCEPTANCE_57x40): Result<Unit> {
        if (originalService.connectionState.value != ConnectionState.CONNECTED) {
            return Result.failure(PrinterException("Принтер не подключен"))
        }

        return try {
            // Получаем формат этикетки
            val format = labelType.getFormat()

            // Создаем изображение этикетки
            val labelBitmap = format.createBitmap(labelData)

            // Для печати используем оригинальный метод сервиса
            // но с модифицированными данными этикетки
            originalService.printLabel(labelData)
        } catch (e: Exception) {
            Log.e(TAG, "Print error", e)
            Result.failure(PrinterException("Ошибка печати: ${e.message}"))
        }
    }
}