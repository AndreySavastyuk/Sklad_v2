package com.example.myprinterapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.printer.LabelData
import com.example.myprinterapp.printer.LabelType
import com.example.myprinterapp.printer.PrinterService
import com.example.myprinterapp.printer.ConnectionState
import com.example.myprinterapp.scanner.BluetoothScannerService
import com.example.myprinterapp.scanner.ScannerState
import com.example.myprinterapp.data.repo.PrintLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AcceptViewModel @Inject constructor(
    private val printerService: PrinterService,
    val scannerService: BluetoothScannerService,
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

    // Состояние сканера
    val scannerConnectionState: StateFlow<ScannerState> = scannerService.scannerState

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

        // Подписываемся на изменения от сканера
        observeScannerInput()

        // Принудительно проверяем подключение сканера
        viewModelScope.launch {
            delay(1000) // Даем время на инициализацию
            scannerService.forceCheckConnection()
        }

        // Тестируем UTF-8 QR генерацию (только в debug режиме)
        if (com.example.myprinterapp.BuildConfig.DEBUG) {
            testQrGeneration()
        }
    }

    /**
     * Наблюдение за вводом от сканера
     */
    private fun observeScannerInput() {
        viewModelScope.launch {
            scannerService.lastScannedCode.collect { code ->
                code?.let {
                    onBarcodeDetected(it)
                    scannerService.clearLastScannedCode()
                }
            }
        }
    }

    /**
     * Обработка отсканированного штрих-кода
     */
    fun onBarcodeDetected(code: String) {
        _scannedValue.value = code
        _uiState.value = AcceptUiState.Idle
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

                    // Создаем тестовые данные этикетки
                    val labelData = LabelData(
                        partNumber = "TEST-UTF8-${System.currentTimeMillis() % 1000}",
                        description = "UTF-8 Test Label",
                        orderNumber = "2024/TEST",
                        location = "A1",
                        quantity = 1,
                        qrData = qrData,
                        labelType = "Тест UTF-8",
                        acceptanceDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    )

                    Log.d("QRTest", "Label data prepared successfully for: ${labelData.partNumber}")

                } catch (e: Exception) {
                    Log.e("QRTest", "Error testing QR: $qrData", e)
                }
            }

            Log.d("QRTest", "=== UTF-8 QR Generation Test Completed ===")
        }
    }

    /**
     * Проверка корректности QR-данных перед печатью
     */
    private fun validateQrDataBeforePrint(qrData: String): Boolean {
        return try {
            // Проверяем, что данные можно корректно закодировать в UTF-8
            val utf8Bytes = qrData.toByteArray(Charsets.UTF_8)
            val restored = String(utf8Bytes, Charsets.UTF_8)

            val isValid = qrData == restored && qrData.isNotBlank()

            if (!isValid) {
                Log.w("QRValidation", "Invalid QR data detected: '$qrData'")
            } else {
                Log.d("QRValidation", "QR data validation passed: '$qrData'")

                // Дополнительно логируем информацию о кириллице
                val hasCyrillic = qrData.any { it in '\u0400'..'\u04FF' }
                if (hasCyrillic) {
                    Log.d("QRValidation", "Cyrillic characters detected in QR data")
                }
            }

            isValid
        } catch (e: Exception) {
            Log.e("QRValidation", "QR validation failed", e)
            false
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
        val filtered = new.filter { it.isLetterOrDigit() }.take(4).uppercase()
        _cellCode.value = filtered
    }

    /**
     * Печать этикетки с улучшенной UTF-8 валидацией
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

        // Проверяем UTF-8 валидность QR-данных
        if (!validateQrDataBeforePrint(scannedData)) {
            _uiState.value = AcceptUiState.Error("QR-код содержит некорректные символы")
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

                // Log для отладки UTF-8
                Log.d("AcceptViewModel", "=== QR Parts UTF-8 Analysis ===")
                parts.forEachIndexed { index, part ->
                    val utf8Bytes = part.toByteArray(Charsets.UTF_8)
                    Log.d("AcceptViewModel", "Part $index: '$part'")
                    Log.d("AcceptViewModel", "  UTF-8 bytes: ${utf8Bytes.contentToString()}")
                    Log.d("AcceptViewModel", "  Has cyrillic: ${part.any { it in '\u0400'..'\u04FF' }}")
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

                Log.d("AcceptViewModel", "=== Label Data for Printing ===")
                Log.d("AcceptViewModel", "QR Data: ${labelData.qrData}")
                Log.d("AcceptViewModel", "Part Number: ${labelData.partNumber}")
                Log.d("AcceptViewModel", "Description: ${labelData.description}")
                Log.d("AcceptViewModel", "Order Number: ${labelData.orderNumber}")

                // Используем основной сервис с форматом для приемки
                printerService.printLabel(labelData, LabelType.ACCEPTANCE_57x40)
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

            Log.d("AcceptViewModel", "Reprinting label with updated data:")
            Log.d("AcceptViewModel", "QR: ${labelData.qrData}")
            Log.d("AcceptViewModel", "Quantity: $newQuantity, Cell: $newCellCode")

            printerService.printLabel(labelData, LabelType.ACCEPTANCE_57x40)
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

    /**
     * Получение инструкций по настройке сканера
     */
    fun getScannerSetupInstructions(): String {
        return scannerService.getSetupInstructions()
    }

    /**
     * Экспорт результатов тестирования UTF-8
     */
    fun exportQrTestResults(): String {
        val sb = StringBuilder()
        sb.appendLine("=== UTF-8 QR Generation Test Results ===")
        sb.appendLine("Timestamp: ${LocalDateTime.now()}")
        sb.appendLine()

        val testCases = listOf(
            "тест=2024/001=ДЕТАЛЬ-123=Тестовая деталь",
            "приемка=2024/002=КОМПОНЕНТ-456=Компонент системы управления",
            "test=2024/004=ЧАСТЬ-000=Mixed текст with кириллицей"
        )

        testCases.forEach { testCase ->
            sb.appendLine("Test Case: $testCase")
            val utf8Bytes = testCase.toByteArray(Charsets.UTF_8)
            sb.appendLine("UTF-8 Bytes: ${utf8Bytes.contentToString()}")
            sb.appendLine("Byte Count: ${utf8Bytes.size}")
            sb.appendLine("Has Cyrillic: ${testCase.any { it in '\u0400'..'\u04FF' }}")
            sb.appendLine()
        }

        return sb.toString()
    }

    override fun onCleared() {
        super.onCleared()
        scannerService.cleanup()
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
