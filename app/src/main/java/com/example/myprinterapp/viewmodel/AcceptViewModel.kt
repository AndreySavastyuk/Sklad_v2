package com.example.myprinterapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.data.models.*
import com.example.myprinterapp.data.repo.PrintLogRepository
import com.example.myprinterapp.data.db.PrintLogEntry
import com.example.myprinterapp.domain.usecase.PrintLabelUseCase
import com.example.myprinterapp.domain.usecase.GetPrintHistoryUseCase
import com.example.myprinterapp.scanner.BleScannerManager
import com.example.myprinterapp.scanner.BleConnectionState
import com.example.myprinterapp.ui.validateAcceptanceQrMask
import com.example.myprinterapp.ui.settings.SettingsViewModel
import com.example.myprinterapp.ui.settings.SettingsUiState
import com.example.myprinterapp.printer.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import timber.log.Timber

// Состояния для совместимости
sealed class AcceptUiState {
    object Idle : AcceptUiState()
    object Printing : AcceptUiState()
    data class Success(val message: String) : AcceptUiState()
    data class Error(val message: String) : AcceptUiState()
}

enum class ScannerState { CONNECTED, DISCONNECTED }

data class AcceptanceRecord(
    val id: String,
    val timestamp: Long,
    val qrData: String,
    val quantity: Int,
    val cellCode: String,
    val partNumber: String,
    val partName: String,
    val orderNumber: String
)

@HiltViewModel
class AcceptViewModel @Inject constructor(
    private val printLabelUseCase: PrintLabelUseCase,
    private val getPrintHistoryUseCase: GetPrintHistoryUseCase,
    private val printLogRepository: PrintLogRepository,
    private val bleScannerManager: BleScannerManager,
    private val printerService: com.example.myprinterapp.printer.PrinterService
) : ViewModel() {

    companion object {
        private const val TAG = "AcceptViewModel"
    }

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

    // Диалоги для ввода данных
    private val _showQuantityDialog = MutableStateFlow(false)
    val showQuantityDialog: StateFlow<Boolean> = _showQuantityDialog.asStateFlow()

    private val _showCellCodeDialog = MutableStateFlow(false)
    val showCellCodeDialog: StateFlow<Boolean> = _showCellCodeDialog.asStateFlow()

    // Реальное состояние принтера из PrinterService
    val printerConnectionState: StateFlow<ConnectionState> = printerService.connectionState

    // Простое состояние сканера на основе BLE состояния
    private val _scannerConnectionState = MutableStateFlow(ScannerState.DISCONNECTED)
    val scannerConnectionState: StateFlow<ScannerState> = _scannerConnectionState.asStateFlow()

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

        // Наблюдение за состоянием BLE сканера
        viewModelScope.launch {
            bleScannerManager.connectionState.collect { bleState ->
                _scannerConnectionState.value = when (bleState) {
                    BleConnectionState.CONNECTED -> ScannerState.CONNECTED
                    else -> ScannerState.DISCONNECTED
                }
            }
        }

        // ИСПРАВЛЕНИЕ: Добавляем подписку на результаты сканирования от BLE сканера
        viewModelScope.launch {
            bleScannerManager.scanResult.collect { scanResult ->
                scanResult?.let { result ->
                    Timber.d("$TAG: Получен результат сканирования от BLE: ${result.data}")
                    onBarcodeDetected(result.data)
                }
            }
        }

        // Тестируем UTF-8 QR генерацию (только в debug режиме)
        if (com.example.myprinterapp.BuildConfig.DEBUG) {
            testQrGeneration()
        }
    }

    /**
     * Обработка сканированного штрих-кода/QR
     */
    fun onBarcodeDetected(code: String) {
        Timber.d("$TAG: Получен QR/штрих-код: $code")
        
        // Проверяем маску для приемки
        if (validateAcceptanceQrMask(code)) {
            _scannedValue.value = code
            // После успешного сканирования открываем диалог количества
            _showQuantityDialog.value = true
        } else {
            _uiState.value = AcceptUiState.Error("QR-код не соответствует маске для приемки")
        }
        
        // Обновление состояния сканера на основе BLE
        bleScannerManager.clearScanResult()
    }

    /**
     * Подтверждение количества и переход к вводу ячейки
     */
    fun onQuantityConfirmed(quantity: Int) {
        _quantity.value = quantity.toString()
        _showQuantityDialog.value = false
        _showCellCodeDialog.value = true
    }

    /**
     * Подтверждение ячейки и завершение ввода
     */
    fun onCellCodeConfirmed(cellCode: String) {
        _cellCode.value = cellCode
        _showCellCodeDialog.value = false
    }

    /**
     * Отмена диалога количества
     */
    fun onQuantityDialogDismissed() {
        _showQuantityDialog.value = false
    }

    /**
     * Отмена диалога ячейки
     */
    fun onCellCodeDialogDismissed() {
        _showCellCodeDialog.value = false
    }

    /**
     * Тестирование генерации QR-кодов с кириллицей
     */
    fun testQrGeneration() {
        viewModelScope.launch {
            val testCases = listOf(
                "тест=2024/001=ДЕТАЛЬ-123=Тестовая деталь",
                "приемка=2024/002=КОМПОНЕНТ-456=Компонент системы управления",
                "заказ=2024/003=ИЗДЕЛИЕ-789=Изделие №1 специального назначения",
                "test=2024/004=ЧАСТЬ-000=Mixed текст with кириллицей",
                "маршрут=2024/005=УЗЕЛ-321=Сборочный узел двигателя"
            )

            Log.d("QRTest", "=== Starting UTF-8 QR Generation Test ===")

            testCases.forEach { qrData ->
                try {
                    Log.d("QRTest", "--- Testing QR generation ---")
                    Log.d("QRTest", "Original data: $qrData")

                    // Проверяем UTF-8 кодировку
                    val utf8Bytes = qrData.toByteArray(Charsets.UTF_8)
                    val restored = String(utf8Bytes, Charsets.UTF_8)

                    Log.d("QRTest", "UTF-8 bytes (${utf8Bytes.size}): " +
                            utf8Bytes.joinToString(" ") { "%02X".format(it) })
                    Log.d("QRTest", "Restored: $restored")
                    Log.d("QRTest", "UTF-8 valid: ${qrData == restored}")

                    // Проверяем символы
                    val cyrillicChars = mutableListOf<Char>()
                    qrData.forEach { char ->
                        if (char.code > 127) {
                            if (char in '\u0400'..'\u04FF') {
                                cyrillicChars.add(char)
                            }
                            Log.d("QRTest", "Non-ASCII char: '$char' (U+${char.code.toString(16).uppercase()})")
                        }
                    }

                    if (cyrillicChars.isNotEmpty()) {
                        Log.d("QRTest", "Cyrillic characters found: ${cyrillicChars.joinToString()}")
                    }

                } catch (e: Exception) {
                    Log.e("QRTest", "Error testing QR: $qrData", e)
                }
            }

            Log.d("QRTest", "=== UTF-8 QR Generation Test Completed ===")
        }
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
        _cellCode.value = new
    }

    /**
     * Сброс полей ввода
     */
    fun onResetInputFields() {
        _scannedValue.value = null
        _quantity.value = ""
        _cellCode.value = ""
        _uiState.value = AcceptUiState.Idle
    }

    /**
     * Очистка сообщений UI
     */
    fun onClearMessage() {
        if (_uiState.value is AcceptUiState.Success || _uiState.value is AcceptUiState.Error) {
            _uiState.value = AcceptUiState.Idle
        }
    }

    /**
     * Печать этикетки - теперь с UseCase
     */
    fun onPrintLabel() {
        val qr = _scannedValue.value
        val qty = _quantity.value
        val cell = _cellCode.value

        if (qr.isNullOrBlank() || qty.isBlank() || cell.isBlank()) {
            _uiState.value = AcceptUiState.Error("Заполните все поля")
            return
        }

        val qtyInt = qty.toIntOrNull()
        if (qtyInt == null || qtyInt <= 0) {
            _uiState.value = AcceptUiState.Error("Введите корректное количество")
            return
        }

        viewModelScope.launch {
            _uiState.value = AcceptUiState.Printing

            try {
                // Парсим QR данные
                val qrParts = qr.split('=')
                if (qrParts.size < 4) {
                    _uiState.value = AcceptUiState.Error("Неверный формат QR-кода")
                    return@launch
                }

                val routeCardNumber = qrParts.getOrNull(0) ?: ""
                val orderNumber = qrParts.getOrNull(1) ?: ""
                val partNumber = qrParts.getOrNull(2) ?: ""
                val partName = qrParts.getOrNull(3) ?: ""

                // Создаем LabelData
                val labelData = LabelData(
                    type = LabelType.STANDARD,
                    routeCardNumber = routeCardNumber,
                    partNumber = partNumber,
                    partName = partName,
                    orderNumber = orderNumber,
                    quantity = qtyInt,
                    cellCode = cell,
                    date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                    qrData = "$routeCardNumber=$orderNumber=$partNumber=$qtyInt=$cell"
                )

                // Используем UseCase для печати
                val result = printLabelUseCase(PrintLabelUseCase.Params(labelData))
                
                when (result) {
                    is Result.Success -> {
                        _uiState.value = AcceptUiState.Success("Этикетка успешно напечатана")
                        // Очищаем поля после успешной печати
                        onResetInputFields()
                        // Обновляем историю операций
                        loadLastOperations()
                    }
                    is Result.Error -> {
                        _uiState.value = AcceptUiState.Error("Ошибка печати: ${result.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e("AcceptViewModel", "Print error", e)
                _uiState.value = AcceptUiState.Error("Ошибка печати: ${e.message}")
            }
        }
    }

    /**
     * Загрузка последних операций
     */
    private fun loadLastOperations() {
        viewModelScope.launch {
            try {
                getPrintHistoryUseCase().collect { logEntries ->
                    _lastOperations.value = logEntries.take(10).map { entry ->
                        AcceptanceRecord(
                            id = entry.id.toString(),
                            timestamp = entry.timestamp.toEpochSecond() * 1000,
                            qrData = entry.qrData,
                            quantity = entry.quantity,
                            cellCode = entry.location,
                            partNumber = entry.partNumber,
                            partName = entry.partName,
                            orderNumber = entry.orderNumber ?: ""
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AcceptViewModel", "Error loading operations", e)
            }
        }
    }

    /**
     * Открытие последней операции для редактирования
     */
    fun openLastOperation() {
        val operations = _lastOperations.value
        if (operations.isNotEmpty()) {
            _editingRecord.value = operations.first()
            _showEditDialog.value = true
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
     * Обновление записи
     */
    fun updateRecord(id: String, quantity: Int, cellCode: String) {
        viewModelScope.launch {
            try {
                // TODO: Реализовать обновление записи в базе
                Log.d("AcceptViewModel", "Updating record $id: qty=$quantity, cell=$cellCode")
                closeEditDialog()
                loadLastOperations()
            } catch (e: Exception) {
                Log.e("AcceptViewModel", "Error updating record", e)
            }
        }
    }
}