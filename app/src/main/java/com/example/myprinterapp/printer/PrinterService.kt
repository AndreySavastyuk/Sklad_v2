package com.example.myprinterapp.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import net.posprinter.TSPLConst
import net.posprinter.TSPLPrinter

class PrinterService(private val context: Context) {
    companion object {
        private const val PRINTER_MAC = "10:23:81:5B:DA:29"
    }

    private val connectionManager = PrinterConnection(context)
    private var tsplPrinter: TSPLPrinter? = null

    init {
        connectionManager.init()
    }

    /**
     * Подключается к принтеру.
     */
    fun connect(): Boolean {
        val ok = connectionManager.connect(PRINTER_MAC)
        if (ok) tsplPrinter = TSPLPrinter(connectionManager.getConnection()!!)
        return ok
    }

    /**
     * Печать этикетки 57×40 мм.
     */
    private fun printFullLabel(
        partNumber: String,
        description: String,
        orderNumber: String,
        location: String,
        qrData: String
    ) {
        tsplPrinter?.apply {
            try {
                cls()
                sizeMm(56.0, 38.0)
                gapMm(2.0, 0.0)
                speed(2.0)
                density(8)
                reference(0, 0)
                codePage("CP1251")

                // Рендер текста
                drawTextBitmap("Part: $partNumber", 10, 10, 32f)
                drawTextBitmap(description, 10, 50, 28f)
                drawTextBitmap("Order: $orderNumber", 10, 85, 28f)

                // QR
                qrcode(300, 10, 5, 0, qrData)

                drawTextBitmap("Loc: $location", 210, 180, 24f)

                print(1)
                Log.d("PrinterService", "printFullLabel: OK")
            } catch (e: Exception) {
                Log.e("PrinterService", "printFullLabel failed", e)
            }
        } ?: Log.e("PrinterService", "TSPLPrinter is null")
    }

    /**
     * Обработка результата сканирования QR + печать
     */
    fun printFromScanned(qrData: String, quantity: String, cellCode: String) {
        // Парсим QR: id=order=part=description
        val parts = qrData.split('=')
        val id    = parts.getOrNull(0) ?: ""
        val order = parts.getOrNull(1) ?: ""
        val part  = parts.getOrNull(2) ?: ""
        val desc  = parts.getOrNull(3) ?: ""

        // Вставляем quantity в описание
        val fullDesc = "$desc  x$quantity"
        printFullLabel(
            partNumber  = part,
            description = fullDesc,
            orderNumber = order,
            location    = cellCode,
            qrData      = qrData
        )
    }

    /**
     * Рисует текст в Bitmap и отправляет на принтер.
     */
    private fun TSPLPrinter.drawTextBitmap(text: String, x: Int, y: Int, textSize: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSize
        }
        val bounds = Rect().also { paint.getTextBounds(text, 0, text.length, it) }
        val bmp = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawText(text, -bounds.left.toFloat(), -bounds.top.toFloat(), paint)
        bitmap(x, y, 0, bmp.width,bmp)
    }

    /**
     * Сохраняет факт комплектации в журнал.
     */
    fun confirmPick(partCode: String, quantity: Int) {
        Log.d("PrinterService", "confirmPick: part=$partCode qty=$quantity")
        // TODO: реализация сохранения в журнал
    }
}