package com.example.myprinterapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.printer.LabelData
import com.example.myprinterapp.printer.PrinterService
import com.example.myprinterapp.printer.ConnectionState
import com.example.myprinterapp.data.db.PrintLogEntry
import com.example.myprinterapp.data.repo.PrintLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AcceptViewModel @Inject constructor(
    private val printerService: PrinterService,
    private val printLogRepository: PrintLogRepository
) : ViewModel() {

    // Состояние полей ввода
    private val _scannedValue = MutableStateFlow<String?>(null)
    val scannedValue: StateFlow<String?> = _scannedValue

    private val _quantity = MutableStateFlow("")
    val quantity: StateFlow<String> = _quantity.asStateFlow()

    private val _cellCode = MutableStateFlow("")
    val cellCode: StateFlow<String> = _cellCode.asStateFlow()

    // Состояние UI
    private val _uiState = MutableStateFlow<AcceptUiState>(AcceptUiState.Idle)
    val uiState: StateFlow<AcceptUiState> = _uiState.asStateFlow()

    // Состояние принтера
    val printerConnectionState: StateFlow<ConnectionState> = printerService.connectionState

    // История последних операций
    private val _lastOperations = MutableStateFlow<List<AcceptanceRecord>>(emptyList())
    val lastOperations: StateFlow<List<AcceptanceRecord>> = _lastOperations.asStateFlow()

    // Показывать диалог редактирования
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    // Запись для редактирования
    private val _editingRecord = MutableStateFlow<AcceptanceRecord?>(null)
    val editingRecord: StateFlow<AcceptanceRecord?> = _editingRecord.asStateFlow()

    init {
        // Загружаем последние операции при инициализации
        loadLastOperations()
    }

    /**
     * Обработка отсканированного штрих-кода
     */
    fun onBarcodeDetected(code: String) {
        _scannedValue.value = code
        _uiState.value = AcceptUiState.Idle
    }

    /**
     * Изменение количества
     */
    fun onQuantityChange(new: String) {
        if (new.all { it.isDigit() } || new.isEmpty()) {
            _quantity.value = new
        }
    }

    /**
     * Изменение кода ячейки
     */
    fun onCellCodeChange(new: String) {
        val filtered = new.filter { it.isLetterOrDigit() }.take(4).uppercase()
        _cellCode.value = filtered
    }

    /**
     * Печать этикетки
     */
    fun onPrintLabel() {
        val scannedData = _scannedValue.value
        val qty = _quantity.value
        val cell = _cellCode.value

        // Валидация
        if (scannedData == null) {
            _uiState.value = AcceptUiState.Error("Сначала отсканируйте QR-код")
            return
        }

        if (qty.isEmpty() || qty.toIntOrNull() == null || qty.toInt() <= 0) {
            _uiState.value = AcceptUiState.Error("Введите корректное количество")
            return
        }

        if (cell.isEmpty()) {
            _uiState.value = AcceptUiState.Error("Введите код ячейки")
            return
        }

        // Проверка подключения принтера
        if (printerConnectionState.value != ConnectionState.CONNECTED) {
            _uiState.value = AcceptUiState.Error("Принтер не подключен. Перейдите в настройки")
            return
        }

        viewModelScope.launch {
            _uiState.value = AcceptUiState.Printing

            try {
                // Парсим QR-код
                val parts = scannedData.split('=')
                if (parts.size < 4) {
                    _uiState.value = AcceptUiState.Error("Неверный формат QR-кода")
                    return@launch
                }

                // Добавляем дату приемки
                val acceptanceDate = LocalDateTime.now()
                val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

                val labelData = LabelData(
                    partNumber = parts[2],
                    description = parts[3],
                    orderNumber = parts[1],
                    location = cell,
                    quantity = qty.toInt(),
                    qrData = scannedData,
                    labelType = "Приемка",
                    acceptanceDate = acceptanceDate.format(dateFormatter)
                )

                printerService.printLabel(labelData)
                    .onSuccess {
                        _uiState.value = AcceptUiState.Success("Этикетка напечатана")

                        // Сохраняем запись о приемке
                        val record = AcceptanceRecord(
                            id = System.currentTimeMillis().toString(),
                            partNumber = parts[2],
                            partName = parts[3],
                            orderNumber = parts[1],
                            quantity = qty.toInt(),
                            cellCode = cell,
                            qrData = scannedData,
                            acceptedAt = acceptanceDate,
                            routeCardNumber = parts[0]
                        )
                        addToHistory(record)

                        // Очищаем поля после успешной печати
                        resetInputFields()
                    }
                    .onFailure { error ->
                        _uiState.value = AcceptUiState.Error(
                            "Ошибка печати: ${error.message ?: "Неизвестная ошибка"}"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = AcceptUiState.Error(
                    "Ошибка: ${e.message ?: "Неизвестная ошибка"}"
                )
            }
        }
    }

    /**
     * Открытие последней операции для редактирования
     */
    fun openLastOperation() {
        _lastOperations.value.firstOrNull()?.let { record ->
            _editingRecord.value = record
            _showEditDialog.value = true
        }
    }

    /**
     * Обновление записи после редактирования
     */
    fun updateRecord(recordId: String, newQuantity: Int, newCellCode: String) {
        viewModelScope.launch {
            val record = _lastOperations.value.find { it.id == recordId } ?: return@launch

            // Обновляем запись
            val updatedRecord = record.copy(
                quantity = newQuantity,
                cellCode = newCellCode,
                editedAt = LocalDateTime.now()
            )

            // Обновляем историю
            _lastOperations.value = _lastOperations.value.map {
                if (it.id == recordId) updatedRecord else it
            }

            // Печатаем новую этикетку
            val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            val labelData = LabelData(
                partNumber = record.partNumber,
                description = "${record.partName} (Изменено)",
                orderNumber = record.orderNumber,
                location = newCellCode,
                quantity = newQuantity,
                qrData = record.qrData,
                labelType = "Приемка (изм.)",
                acceptanceDate = record.acceptedAt.format(dateFormatter)
            )

            printerService.printLabel(labelData)
                .onSuccess {
                    _uiState.value = AcceptUiState.Success("Исправленная этикетка напечатана")
                    _showEditDialog.value = false
                }
                .onFailure { error ->
                    _uiState.value = AcceptUiState.Error("Ошибка печати: ${error.message}")
                }
        }
    }

    /**
     * Закрытие диалога редактирования
     */
    fun closeEditDialog() {
        _showEditDialog.value = false
        _editingRecord.value = null
    }

    /**
     * Сброс полей ввода
     */
    fun resetInputFields() {
        _quantity.value = ""
        _cellCode.value = ""
        _scannedValue.value = null
    }

    /**
     * Очистка сообщений об ошибках/успехе
     */
    fun clearMessage() {
        if (_uiState.value !is AcceptUiState.Printing) {
            _uiState.value = AcceptUiState.Idle
        }
    }

    /**
     * Добавление записи в историю
     */
    private fun addToHistory(record: AcceptanceRecord) {
        val currentHistory = _lastOperations.value.toMutableList()
        currentHistory.add(0, record) // Добавляем в начало
        // Оставляем только последние 10 записей
        _lastOperations.value = currentHistory.take(10)
    }

    /**
     * Загрузка последних операций
     */
    private fun loadLastOperations() {
        viewModelScope.launch {
            // TODO: Загрузить из базы данных
            // Пока используем пустой список
            _lastOperations.value = emptyList()
        }
    }
}

/**
 * Состояния UI экрана приемки
 */
sealed class AcceptUiState {
    object Idle : AcceptUiState()
    object Printing : AcceptUiState()
    data class Success(val message: String) : AcceptUiState()
    data class Error(val message: String) : AcceptUiState()
}

/**
 * Запись о приемке товара
 */
data class AcceptanceRecord(
    val id: String,
    val partNumber: String,
    val partName: String,
    val orderNumber: String,
    val quantity: Int,
    val cellCode: String,
    val qrData: String,
    val acceptedAt: LocalDateTime,
    val routeCardNumber: String,
    val editedAt: LocalDateTime? = null
)