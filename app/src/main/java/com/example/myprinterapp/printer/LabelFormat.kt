package com.example.myprinterapp.printer

import android.graphics.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import android.util.Log
import com.example.myprinterapp.data.models.LabelData
import com.example.myprinterapp.printer.LabelFormat

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

        val margin = 8f

        // Рамка этикетки
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(1f, 1f, widthPx - 1f, heightPx - 1f, borderPaint)

        // QR-код с улучшенной UTF-8 поддержкой
        val qrX = 16f
        val qrY = 95f
        val qrSize = 200

        try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )

            val bitMatrix = QRCodeWriter().encode(data.qrData, BarcodeFormat.QR_CODE, qrSize, qrSize, hints)
            val qrBitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888)

            for (x in 0 until qrSize) {
                for (y in 0 until qrSize) {
                    qrBitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            canvas.drawBitmap(qrBitmap, qrX, qrY, null)
        } catch (e: Exception) {
            Log.e("AcceptanceLabelFormat", "QR code generation error", e)
        }

        // Текстовая информация
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val smallTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
        }

        // Заголовок "ПРИЕМКА"
        textPaint.textSize = 30f
        canvas.drawText("ПРИЕМКА", widthPx/2 - 70f, margin + 25f, textPaint)

        // Артикул
        textPaint.textSize = 24f
        canvas.drawText("Арт: ${data.partNumber}", margin + 10f, margin + 55f, textPaint)

        // Наименование (с ограничением длины)
        val description = if ((data.partName ?: "").length > 20)
            (data.partName ?: "").substring(0, 20) + "..."
        else
            data.partName ?: ""
        smallTextPaint.textSize = 18f
        canvas.drawText(description, margin + 10f, margin + 80f, smallTextPaint)

        // Заказ справа от QR-кода
        smallTextPaint.textSize = 22f
        canvas.drawText("Заказ:", qrX + qrSize + 10f, qrY + 35f, smallTextPaint)
        canvas.drawText(data.orderNumber, qrX + qrSize + 10f, qrY + 60f, smallTextPaint)

        // Локация (если есть)
        canvas.drawText("Локация:", qrX + qrSize + 10f, qrY + 95f, smallTextPaint)
        val location = data.location ?: data.cellCode
        canvas.drawText(location, qrX + qrSize + 10f, qrY + 120f, smallTextPaint)

        // Дата (используем поле date вместо acceptanceDate)
        smallTextPaint.textSize = 18f
        canvas.drawText("Дата: ${data.date}", 16f, heightPx - margin - 10f, smallTextPaint)

        return bitmap
    }

    /**
     * Валидация QR-данных для обеспечения корректной UTF-8 кодировки
     */
    private fun validateQrData(data: String): String {
        return try {
            // Явно конвертируем в UTF-8 и обратно для проверки
            val utf8Bytes = data.toByteArray(Charsets.UTF_8)
            val restored = String(utf8Bytes, Charsets.UTF_8)

            Log.d("QRValidation", "Original: $data")
            Log.d("QRValidation", "UTF-8 bytes: ${utf8Bytes.contentToString()}")
            Log.d("QRValidation", "Restored: $restored")
            Log.d("QRValidation", "UTF-8 valid: ${data == restored}")

            // Убираем потенциально проблемные управляющие символы
            val cleaned = restored.replace("\r", "").replace("\n", "")

            cleaned
        } catch (e: Exception) {
            Log.e("QRValidation", "QR data validation failed", e)
            // Убираем невалидные символы как fallback
            data.filter { char ->
                char.code <= 0xFFFF && // Базовая многоязычная плоскость Unicode
                        (!char.isISOControl() || char == '\t')
            }
        }
    }

    /**
     * Улучшенная генерация QR-кода с полной поддержкой UTF-8
     */
    private fun generateQRCode(data: String, size: Int): Bitmap {
        return try {
            // Явно обеспечиваем UTF-8 кодировку
            val utf8Data = String(data.toByteArray(Charsets.UTF_8), Charsets.UTF_8)

            val writer = QRCodeWriter()
            val hints = HashMap<EncodeHintType, Any>().apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1)
            }

            Log.d("QRGeneration", "Generating QR with UTF-8 data: $utf8Data")
            Log.d("QRGeneration", "Data length: ${utf8Data.length} chars")
            Log.d("QRGeneration", "UTF-8 bytes: ${utf8Data.toByteArray(Charsets.UTF_8).contentToString()}")

            // Проверяем наличие кириллицы
            val hasCyrillic = utf8Data.any { it in '\u0400'..'\u04FF' }
            if (hasCyrillic) {
                Log.d("QRGeneration", "Cyrillic characters detected in QR data")
            }

            val bitMatrix = writer.encode(utf8Data, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            Log.d("QRGeneration", "QR code generated successfully: ${size}x${size}")
            bitmap

        } catch (e: Exception) {
            Log.e("QRGeneration", "Failed to generate QR code with data: $data", e)
            // Создаем пустой белый квадрат в случае ошибки
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
        }
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
        canvas.drawText(data.partName, margin, yPos, namePaint)

        // QR-код справа внизу с UTF-8 поддержкой
        val qrSize = 100
        val qrX = widthPx - qrSize - margin.toInt()
        val qrY = heightPx - qrSize - margin.toInt()

        // Валидируем QR-данные
        val validatedQrData = validateQrData(data.qrData)
        val qrBitmap = generateQRCode(validatedQrData, qrSize)
        canvas.drawBitmap(qrBitmap, qrX.toFloat(), qrY.toFloat(), null)

        // Количество и ячейка внизу слева
        val bottomY = heightPx - margin - 20f
        val qtyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create("Arial", Typeface.BOLD)
        }
        canvas.drawText("Кол-во: ${data.quantity} шт", margin, bottomY, qtyPaint)

        // Ячейка
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create("Arial", Typeface.NORMAL)
        }
        canvas.drawText("Ячейка: ${data.cellCode}", margin, heightPx - margin - 5f, cellPaint)

        return bitmap
    }

    /**
     * Валидация QR-данных для комплектации
     */
    private fun validateQrData(data: String): String {
        return try {
            val utf8Bytes = data.toByteArray(Charsets.UTF_8)
            val restored = String(utf8Bytes, Charsets.UTF_8)

            Log.d("PickingQR", "Validating QR data: $data")
            Log.d("PickingQR", "UTF-8 valid: ${data == restored}")

            restored.replace("\r", "").replace("\n", "")
        } catch (e: Exception) {
            Log.e("PickingQR", "QR validation failed", e)
            data.filter { !it.isISOControl() }
        }
    }

    /**
     * Генерация QR-кода для комплектации с UTF-8
     */
    private fun generateQRCode(data: String, size: Int): Bitmap {
        return try {
            val utf8Data = String(data.toByteArray(Charsets.UTF_8), Charsets.UTF_8)

            val writer = QRCodeWriter()
            val hints = HashMap<EncodeHintType, Any>().apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 0)
            }

            Log.d("PickingQR", "Generating picking QR: $utf8Data")

            val bitMatrix = writer.encode(utf8Data, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            bitmap
        } catch (e: Exception) {
            Log.e("PickingQR", "Failed to generate picking QR", e)
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

