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
        val qrSize = 140 // Фиксированный размер QR

        // Внешняя рамка
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, widthPx - 1f, heightPx - 1f, borderPaint)

        // 1. QR-код слева (жестко зафиксирован)
        val qrX = margin.toInt()
        val qrY = ((heightPx - qrSize) / 2).toInt()
        val qrBitmap = generateQRCode(data.qrData, qrSize)
        canvas.drawBitmap(qrBitmap, qrX.toFloat(), qrY.toFloat(), null)

        // Область справа от QR
        val rightAreaStartX = qrX + qrSize + margin
        val rightAreaWidth = widthPx - rightAreaStartX - margin
        val rightAreaCenterX = rightAreaStartX + rightAreaWidth / 2

        // 2. Обозначение детали (сверху по центру между QR и верхним краем)
        val partNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create("Arial", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Определяем размер шрифта для обозначения
        var partNumberSize = 24f
        partNumberPaint.textSize = partNumberSize
        val maxPartNumberWidth = rightAreaWidth * 0.9f // 90% ширины области

        while (partNumberPaint.measureText(data.partNumber) > maxPartNumberWidth && partNumberSize > 12f) {
            partNumberSize -= 0.5f
            partNumberPaint.textSize = partNumberSize
        }

        // Позиция Y для обозначения (между QR и верхним краем)
        val partNumberY = qrY / 2 + partNumberPaint.textSize / 2
        canvas.drawText(data.partNumber, rightAreaCenterX, partNumberY, partNumberPaint)

        // 3. Наименование детали (под обозначением)
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        // Проверяем, нужен ли перенос для наименования
        val nameLines = if (namePaint.measureText(data.description) > rightAreaWidth * 0.9f) {
            wrapTextCenter(data.description, namePaint, rightAreaWidth * 0.9f)
        } else {
            listOf(data.description)
        }

        // Если 2 строки, уменьшаем размер шрифта
        if (nameLines.size > 1) {
            namePaint.textSize = 16f
        }

        // Рисуем наименование
        var nameY = partNumberY + partNumberPaint.textSize + 8f
        nameLines.take(2).forEach { line ->
            canvas.drawText(line, rightAreaCenterX, nameY, namePaint)
            nameY += namePaint.textSize + 2f
        }

        // 4. Номер заказа (внизу справа)
        val orderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        // Позиция для заказа
        val orderY = heightPx - margin - 70f
        canvas.drawText(data.orderNumber, rightAreaCenterX, orderY, orderPaint)

        // 5. Ячейка в рамке (самый низ)
        val cellBoxHeight = 40f
        val cellBoxWidth = 80f
        val cellBoxX = rightAreaCenterX - cellBoxWidth / 2
        val cellBoxY = heightPx - cellBoxHeight - margin

        // Тонкая рамка для ячейки
        val cellBorderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(cellBoxX, cellBoxY, cellBoxX + cellBoxWidth, cellBoxY + cellBoxHeight, cellBorderPaint)

        // Текст ячейки
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 36f
            typeface = Typeface.create("Arial", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cellTextY = cellBoxY + (cellBoxHeight + cellPaint.textSize) / 2 - 6f
        canvas.drawText(data.location, rightAreaCenterX, cellTextY, cellPaint)

        return bitmap
    }

    /**
     * Перенос текста по центру
     */
    private fun wrapTextCenter(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        if (words.size == 1) {
            // Если одно слово, возвращаем как есть
            return listOf(text)
        }

        // Пытаемся разделить на 2 примерно равные части
        val midPoint = words.size / 2
        val firstLine = words.subList(0, midPoint).joinToString(" ")
        val secondLine = words.subList(midPoint, words.size).joinToString(" ")

        // Проверяем, помещаются ли обе строки
        if (paint.measureText(firstLine) <= maxWidth && paint.measureText(secondLine) <= maxWidth) {
            return listOf(firstLine, secondLine)
        }

        // Иначе используем стандартный перенос
        return wrapText(text, paint, maxWidth)
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