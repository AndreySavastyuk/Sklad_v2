package com.example.myprinterapp.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.data.repo.PrintLogRepository
import com.example.myprinterapp.data.db.PrintLogEntry
import com.example.myprinterapp.printer.PrinterService
import com.example.myprinterapp.printer.LabelData
import com.example.myprinterapp.printer.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class PrintLogViewModel @Inject constructor(
    private val repo: PrintLogRepository,
    private val printerService: PrinterService
) : ViewModel() {

    // Состояние записей журнала
    val entries = repo.logFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // Состояние подключения принтера
    val printerConnectionState: StateFlow<ConnectionState> = printerService.connectionState

    /**
     * Повторная печать этикетки с возможностью изменения данных
     */
    fun reprintLabel(
        originalEntry: PrintLogEntry,
        newQuantity: Int,
        newCellCode: String
    ) {
        viewModelScope.launch {
            try {
                // Извлекаем данные из QR-кода оригинальной записи
                val qrParts = originalEntry.qrData.split('=')
                if (qrParts.size >= 4) {
                    // Добавляем дату повторной печати
                    val currentDate = LocalDateTime.now()
                    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

                    val labelData = LabelData(
                        partNumber = originalEntry.partNumber,
                        description = "${qrParts[3]} (Переиздание)",
                        orderNumber = originalEntry.orderNumber ?: qrParts[1],
                        location = newCellCode,
                        quantity = newQuantity,
                        qrData = originalEntry.qrData,
                        labelType = "${originalEntry.labelType} (переиздание)",
                        acceptanceDate = currentDate.format(dateFormatter)
                    )

                    // Печатаем этикетку
                    printerService.printLabel(labelData)
                        .onSuccess {
                            // Логируем операцию переиздания
                            val newEntry = PrintLogEntry(
                                dateTime = currentDate.atOffset(java.time.ZoneOffset.UTC),
                                labelType = "${originalEntry.labelType} (переиздание)",
                                partNumber = originalEntry.partNumber,
                                orderNumber = originalEntry.orderNumber,
                                quantity = newQuantity,
                                cellCode = newCellCode,
                                qrData = originalEntry.qrData
                            )
                            repo.add(newEntry)
                        }
                        .onFailure { error ->
                            // TODO: Показать сообщение об ошибке
                            println("Error reprinting label: ${error.message}")
                        }
                }
            } catch (e: Exception) {
                // TODO: Показать сообщение об ошибке
                println("Error in reprintLabel: ${e.message}")
            }
        }
    }

    /**
     * Очистка журнала операций
     */
    fun clearLog() {
        viewModelScope.launch {
            try {
                repo.clear()
            } catch (e: Exception) {
                // TODO: Показать сообщение об ошибке
                println("Error clearing log: ${e.message}")
            }
        }
    }

    /**
     * Экспорт журнала в текстовый формат
     */
    fun exportLogToText(): String {
        val currentEntries = entries.value
        if (currentEntries.isEmpty()) return "Журнал операций пуст"

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val sb = StringBuilder()

        sb.appendLine("ЖУРНАЛ ОПЕРАЦИЙ СКЛАДА")
        sb.appendLine("Экспортирован: ${LocalDateTime.now().format(formatter)}")
        sb.appendLine("Всего записей: ${currentEntries.size}")
        sb.appendLine()
        sb.appendLine("=".repeat(50))
        sb.appendLine()

        // Группируем по дням
        val groupedByDate = currentEntries.groupBy {
            it.dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }

        groupedByDate.forEach { (date, dayEntries) ->
            sb.appendLine("ДАТА: $date (${dayEntries.size} операций)")
            sb.appendLine("-".repeat(30))

            dayEntries.forEach { entry ->
                sb.appendLine("${entry.dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))} | ${entry.labelType}")
                sb.appendLine("  Артикул: ${entry.partNumber}")
                entry.orderNumber?.let { sb.appendLine("  Заказ: $it") }
                entry.quantity?.let { sb.appendLine("  Количество: $it шт") }
                entry.cellCode?.let { sb.appendLine("  Ячейка: $it") }
                sb.appendLine()
            }
            sb.appendLine()
        }

        return sb.toString()
    }

    /**
     * Получение статистики операций
     */
    fun getOperationStats(): OperationStats {
        val currentEntries = entries.value
        val today = LocalDateTime.now().toLocalDate()

        val todayEntries = currentEntries.filter {
            it.dateTime.toLocalDate() == today
        }

        val byType = currentEntries.groupBy { it.labelType }
        val totalQuantity = currentEntries.mapNotNull { it.quantity }.sum()

        return OperationStats(
            totalOperations = currentEntries.size,
            todayOperations = todayEntries.size,
            operationsByType = byType.mapValues { it.value.size },
            totalQuantityProcessed = totalQuantity,
            lastOperationTime = currentEntries.firstOrNull()?.dateTime
        )
    }
}

/**
 * Статистика операций
 */
data class OperationStats(
    val totalOperations: Int,
    val todayOperations: Int,
    val operationsByType: Map<String, Int>,
    val totalQuantityProcessed: Int,
    val lastOperationTime: java.time.OffsetDateTime?
)