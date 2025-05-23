package com.example.myprinterapp.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.data.db.PrintLogEntry
import com.example.myprinterapp.data.repo.PrintLogRepository
import com.example.myprinterapp.printer.ConnectionState
import com.example.myprinterapp.printer.LabelData
import com.example.myprinterapp.printer.PrinterService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class PrintLogViewModel @Inject constructor(
    private val repo: PrintLogRepository,
    private val printerService: PrinterService
) : ViewModel() {

    val entries = repo.logFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val printerConnectionState: StateFlow<ConnectionState> = printerService.connectionState

    fun reprintLabel(entry: PrintLogEntry, newQuantity: Int, newCellCode: String) {
        viewModelScope.launch {
            // Парсим QR для получения дополнительных данных
            val parts = entry.qrData.split('=')
            val partName = if (parts.size >= 4) parts[3] else "Деталь"
            val routeCardNumber = if (parts.size >= 1) parts[0] else ""

            // Определяем дату приемки
            val acceptanceDate = entry.dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

            val labelData = LabelData(
                partNumber = entry.partNumber,
                description = "$partName (Повтор)",
                orderNumber = entry.orderNumber ?: "",
                location = newCellCode,
                quantity = newQuantity,
                qrData = entry.qrData,
                labelType = "Повторная печать",
                acceptanceDate = acceptanceDate
            )

            printerService.printLabel(labelData)
                .onSuccess {
                    // TODO: Показать сообщение об успешной печати
                }
                .onFailure { error ->
                    // TODO: Показать сообщение об ошибке
                }
        }
    }

    fun clearLog() {
        viewModelScope.launch {
            repo.clear()
        }
    }
}