package com.example.myprinterapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.printer.LabelData
import com.example.myprinterapp.printer.PrinterService
import com.example.myprinterapp.printer.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AcceptViewModel @Inject constructor(
    private val printerService: PrinterService
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
        // Фильтруем только буквы и цифры, максимум 4 символа
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

                val labelData = LabelData(
                    partNumber = parts[2],
                    description = "${parts[3]} x$qty", // Добавляем количество к описанию
                    orderNumber = parts[1],
                    location = cell,
                    quantity = qty.toInt(),
                    qrData = scannedData,
                    labelType = "Приемка"
                )

                printerService.printLabel(labelData)
                    .onSuccess {
                        _uiState.value = AcceptUiState.Success("Этикетка напечатана")
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