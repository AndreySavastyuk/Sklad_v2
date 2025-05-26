package com.example.myprinterapp.ui.scanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.scanner.BluetoothScannerService
import com.example.myprinterapp.scanner.ScannerDecoderService
import com.example.myprinterapp.scanner.ScannerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ScannerConfigViewModel @Inject constructor(
    private val scannerService: BluetoothScannerService,
    private val decoderService: ScannerDecoderService
) : ViewModel() {

    companion object {
        private const val TAG = "ScannerConfigVM"
        private const val PREFS_KEY_SCANNER_MODE = "scanner_mode"
    }

    // Состояние подключения
    val scannerState: StateFlow<ScannerState> = scannerService.scannerState

    // Подключенное устройство
    private val _connectedDevice = MutableStateFlow<String?>(null)
    val connectedDevice: StateFlow<String?> = _connectedDevice.asStateFlow()

    // Текущий режим сканера
    private val _currentScannerMode = MutableStateFlow(ScannerMode.HID_STANDARD)
    val currentScannerMode: StateFlow<ScannerMode> = _currentScannerMode.asStateFlow()

    // Результаты тестирования
    private val _testResults = MutableStateFlow<List<TestResult>>(emptyList())
    val testResults: StateFlow<List<TestResult>> = _testResults.asStateFlow()

    // Конфигурационные QR-коды
    private val _configQrCodes = MutableStateFlow<Map<String, String>>(emptyMap())
    val configQrCodes: StateFlow<Map<String, String>> = _configQrCodes.asStateFlow()

    init {
        // Загружаем сохраненный режим
        loadSavedMode()

        // Подписываемся на изменения состояния сканера
        viewModelScope.launch {
            scannerService.connectedScanner.collect { device ->
                _connectedDevice.value = device?.let {
                    try {
                        "${it.name} (${it.address})"
                    } catch (e: SecurityException) {
                        it.address
                    }
                }
            }
        }

        // Загружаем конфигурационные QR-коды
        loadConfigQrCodes()
    }

    /**
     * Загрузка сохраненного режима сканера
     */
    private fun loadSavedMode() {
        // TODO: Загрузить из SharedPreferences
        _currentScannerMode.value = ScannerMode.HID_STANDARD
    }

    /**
     * Загрузка QR-кодов для настройки
     */
    private fun loadConfigQrCodes() {
        _configQrCodes.value = mapOf(
            // Основные команды
            "Enter Setup" to "\\u0016\\u004D\\u000D",
            "Exit Setup" to "\\u0016\\u0054\\u000D",

            // Режимы
            "Bluetooth HID Mode" to "\\u0016\\u0012\\u0000\\u0001",
            "SPP Mode" to "\\u0016\\u0012\\u0000\\u0002",

            // Форматы данных
            "Standard Format" to "\\u0016\\u0030\\u0000",
            "AIM ID Enable" to "\\u0016\\u0031\\u0001",
            "AIM ID Disable" to "\\u0016\\u0031\\u0000",
            "Code ID Enable" to "\\u0016\\u0032\\u0001",
            "Code ID Disable" to "\\u0016\\u0032\\u0000",
            "Hex String Format" to "\\u0016\\u0033\\u0001",

            // Префиксы/суффиксы
            "Add Prefix \\x" to "\\u0016\\u0040\\u005C\\u0078",
            "Clear Prefix" to "\\u0016\\u0040",
            "Add Suffix CR" to "\\u0016\\u0041\\u000D",
            "Clear Suffix" to "\\u0016\\u0041",

            // Кодировки
            "Character Set UTF-8" to "\\u0016\\u0050\\u0008",
            "Character Set Windows-1251" to "\\u0016\\u0050\\u0033",

            // Звуковые сигналы
            "Beep On" to "\\u0016\\u0034\\u0001",
            "Beep Off" to "\\u0016\\u0034\\u0000",

            // Дополнительные настройки
            "Upper Case On" to "\\u0016\\u0035\\u0001",
            "Upper Case Off" to "\\u0016\\u0035\\u0000",
            "Add Enter" to "\\u0016\\u0036\\u0001",
            "Remove Enter" to "\\u0016\\u0036\\u0000"
        )
    }

    /**
     * Обновление подключения
     */
    fun refreshConnection() {
        viewModelScope.launch {
            Log.d(TAG, "Refreshing scanner connection...")
            scannerService.forceCheckConnection()
        }
    }

    /**
     * Применение режима сканера
     */
    fun applyScannerMode(mode: ScannerMode) {
        viewModelScope.launch {
            Log.d(TAG, "Applying scanner mode: ${mode.displayName}")
            _currentScannerMode.value = mode

            // TODO: Сохранить в SharedPreferences

            // Показываем уведомление
            addTestResult(
                TestResult(
                    timestamp = getCurrentTimestamp(),
                    mode = mode,
                    input = "",
                    output = "Режим изменен на: ${mode.displayName}",
                    success = true,
                    details = "Требуется перенастройка сканера согласно инструкции"
                )
            )
        }
    }

    /**
     * Тестирование ввода со сканера
     */
    fun testScannerInput(input: String) {
        viewModelScope.launch {
            Log.d(TAG, "Testing scanner input: $input")

            val currentMode = _currentScannerMode.value

            try {
                // Определяем формат входных данных
                val format = detectInputFormat(input)

                // Декодируем в зависимости от режима
                val decoded = when (currentMode) {
                    ScannerMode.HID_STANDARD -> decoderService.decode(input)
                    ScannerMode.HID_WITH_AIM_ID -> decodeWithAimId(input)
                    ScannerMode.HID_WITH_CODE_ID -> decodeWithCodeId(input)
                    ScannerMode.HID_HEX_MODE -> decodeHexMode(input)
                    ScannerMode.SPP_MODE -> decodeSppMode(input)
                }

                // Добавляем результат
                addTestResult(
                    TestResult(
                        timestamp = getCurrentTimestamp(),
                        mode = currentMode,
                        input = input,
                        output = decoded.data,
                        success = true,
                        details = "Формат: ${format}, Тип кода: ${decoded.codeType}"
                    )
                )

                // Проверяем на кириллицу
                if (decoded.data.any { it in '\u0400'..'\u04FF' }) {
                    Log.d(TAG, "Cyrillic detected in output: ${decoded.data}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error testing input", e)
                addTestResult(
                    TestResult(
                        timestamp = getCurrentTimestamp(),
                        mode = currentMode,
                        input = input,
                        output = "Ошибка декодирования: ${e.message}",
                        success = false,
                        details = null
                    )
                )
            }
        }
    }

    /**
     * Определение формата входных данных
     */
    private fun detectInputFormat(input: String): String {
        return when {
            input.startsWith("]") -> "AIM ID"
            input.contains('\u001D') -> "Code ID с GS"
            input.contains("\\x") -> "HEX"
            input.contains("%") -> "URL Encoded"
            input.length > 0 && !input[0].isLetterOrDigit() -> "Code ID"
            else -> "Обычный текст"
        }
    }

    /**
     * Декодирование с AIM ID
     */
    private fun decodeWithAimId(input: String): DecodedData {
        val hidPosData = decoderService.decodeHidPosData(input)
        return if (hidPosData != null) {
            DecodedData(
                data = hidPosData.data,
                codeType = hidPosData.symbology,
                rawBytes = hidPosData.rawBytes
            )
        } else {
            DecodedData(
                data = input,
                codeType = "Unknown",
                rawBytes = input.toByteArray()
            )
        }
    }

    /**
     * Декодирование с Code ID
     */
    private fun decodeWithCodeId(input: String): DecodedData {
        if (input.length > 1) {
            val codeId = input[0]
            val data = input.substring(1)
            val codeType = getCodeTypeFromId(codeId)

            return DecodedData(
                data = decoderService.decode(data),
                codeType = codeType,
                rawBytes = data.toByteArray()
            )
        }
        return DecodedData(input, "Unknown", input.toByteArray())
    }

    /**
     * Декодирование HEX режима
     */
    private fun decodeHexMode(input: String): DecodedData {
        val decoded = decoderService.decode(input)
        return DecodedData(
            data = decoded,
            codeType = "QR Code", // В HEX режиме тип кода не передается
            rawBytes = input.toByteArray()
        )
    }

    /**
     * Декодирование SPP режима
     */
    private fun decodeSppMode(input: String): DecodedData {
        // В SPP режиме данные приходят как есть в UTF-8
        return DecodedData(
            data = input,
            codeType = "QR Code",
            rawBytes = input.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Определение типа кода по ID
     */
    private fun getCodeTypeFromId(id: Char): String {
        return when (id.lowercaseChar()) {
            'q' -> "QR Code"
            'j' -> "Code 128"
            'd' -> "EAN-13"
            'b' -> "Code 39"
            'u' -> "Data Matrix"
            'e' -> "EAN-8"
            'c' -> "UPC-A"
            'p' -> "PDF417"
            else -> "Unknown ($id)"
        }
    }

    /**
     * Добавление результата тестирования
     */
    private fun addTestResult(result: TestResult) {
        val currentResults = _testResults.value.toMutableList()
        currentResults.add(0, result) // Добавляем в начало

        // Оставляем только последние 10 результатов
        if (currentResults.size > 10) {
            _testResults.value = currentResults.take(10)
        } else {
            _testResults.value = currentResults
        }
    }

    /**
     * Очистка результатов тестирования
     */
    fun clearTestResults() {
        _testResults.value = emptyList()
    }

    /**
     * Генерация тестовых QR-кодов
     */
    fun generateTestQrCodes(): List<TestQrCode> {
        return listOf(
            TestQrCode(
                name = "Обычный текст (латиница)",
                data = "test=2024/001=PART-123=Test Part",
                description = "Базовый QR-код с латиницей"
            ),
            TestQrCode(
                name = "Текст с кириллицей",
                data = "тест=2024/001=ДЕТАЛЬ-123=Тестовая деталь",
                description = "QR-код с русскими символами"
            ),
            TestQrCode(
                name = "Смешанный текст",
                data = "order=2024/001=PART-123=Деталь тестовая",
                description = "Латиница + кириллица"
            ),
            TestQrCode(
                name = "Специальные символы",
                data = "test=2024/001=PART#123=Detail@Test!",
                description = "Со спецсимволами"
            ),
            TestQrCode(
                name = "Длинный текст",
                data = "МК-2024-0012345=ЗАК/2024/00123=ДЕТАЛЬ.СЛОЖНАЯ.НАЗВАНИЕ-12345=Очень длинное название детали с подробным описанием",
                description = "Максимальная длина"
            )
        )
    }

    /**
     * Получение текущей временной метки
     */
    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    /**
     * Экспорт результатов тестирования
     */
    fun exportTestResults(): String {
        val results = _testResults.value
        if (results.isEmpty()) return "Нет результатов для экспорта"

        val sb = StringBuilder()
        sb.appendLine("=== Результаты тестирования сканера HR32-BT ===")
        sb.appendLine("Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
        sb.appendLine("Текущий режим: ${_currentScannerMode.value.displayName}")
        sb.appendLine()

        results.forEach { result ->
            sb.appendLine("--- ${result.timestamp} ---")
            sb.appendLine("Режим: ${result.mode.displayName}")
            sb.appendLine("Вход: ${result.input}")
            sb.appendLine("Выход: ${result.output}")
            sb.appendLine("Статус: ${if (result.success) "Успешно" else "Ошибка"}")
            result.details?.let { sb.appendLine("Детали: $it") }
            sb.appendLine()
        }

        return sb.toString()
    }
}

// Вспомогательные модели данных
data class DecodedData(
    val data: String,
    val codeType: String,
    val rawBytes: ByteArray?
)

data class TestQrCode(
    val name: String,
    val data: String,
    val description: String
)