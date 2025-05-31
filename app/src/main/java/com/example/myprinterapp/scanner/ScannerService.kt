package com.example.myprinterapp.scanner

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// Импорты из Newland SDK (добавить в build.gradle)
import com.newland.nlsdk.*
import com.newland.nlsdk.scanner.*
import com.newland.nlsdk.bluetooth.*

@Singleton
class NewlandScannerService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "NewlandScannerService"
    }

    // Newland SDK компоненты
    private var scannerManager: ScannerManager? = null
    private var bluetoothManager: BluetoothManager? = null
    private var connectedScanner: Scanner? = null

    // Состояния
    private val _connectionState = MutableStateFlow(ScannerConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ScannerConnectionState> = _connectionState

    private val _lastScannedData = MutableStateFlow<ScanData?>(null)
    val lastScannedData: StateFlow<ScanData?> = _lastScannedData

    private val _availableScanners = MutableStateFlow<List<ScannerInfo>>(emptyList())
    val availableScanners: StateFlow<List<ScannerInfo>> = _availableScanners

    init {
        initializeNewlandSDK()
    }

    /**
     * Инициализация Newland SDK
     */
    private fun initializeNewlandSDK() {
        try {
            // Инициализация SDK
            NLSDKManager.getInstance().init(context)

            // Создание менеджера сканеров
            scannerManager = ScannerManager.getInstance()
            bluetoothManager = BluetoothManager.getInstance()

            // Настройка слушателей
            setupScannerListeners()

            Log.d(TAG, "Newland SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Newland SDK", e)
        }
    }

    /**
     * Настройка слушателей SDK
     */
    private fun setupScannerListeners() {
        // Слушатель подключения
        bluetoothManager?.setConnectionListener(object : ConnectionListener {
            override fun onConnected(scanner: Scanner) {
                Log.d(TAG, "Scanner connected: ${scanner.deviceInfo.name}")
                connectedScanner = scanner
                _connectionState.value = ScannerConnectionState.CONNECTED

                // Настраиваем сканер для кириллицы
                configureForCyrillic(scanner)
            }

            override fun onDisconnected(scanner: Scanner) {
                Log.d(TAG, "Scanner disconnected")
                connectedScanner = null
                _connectionState.value = ScannerConnectionState.DISCONNECTED
            }

            override fun onConnectionFailed(error: ConnectionError) {
                Log.e(TAG, "Connection failed: ${error.message}")
                _connectionState.value = ScannerConnectionState.DISCONNECTED
            }
        })

        // Слушатель сканирования
        scannerManager?.setScanListener(object : ScanListener {
            override fun onScanResult(scanData: ScanData) {
                processScanResult(scanData)
            }

            override fun onScanError(error: ScanError) {
                Log.e(TAG, "Scan error: ${error.message}")
            }
        })
    }

    /**
     * Настройка сканера для поддержки кириллицы
     */
    private fun configureForCyrillic(scanner: Scanner) {
        try {
            val config = ScannerConfig().apply {
                // Основные настройки для кириллицы
                characterSet = CharacterSet.UTF8
                dataFormat = DataFormat.RAW_WITH_HEADER

                // Включаем передачу метаданных
                includeSymbologyInfo = true
                includeAimId = true
                includeCodeId = true

                // Настройки для HID режима
                hidMode = HidMode.KEYBOARD_EMULATION
                hidKeyboardLayout = KeyboardLayout.RUSSIAN

                // Префиксы/суффиксы
                prefix = ""
                suffix = ScannerConfig.CARRIAGE_RETURN

                // Включаем все типы кодов
                enableAllSymbologies()

                // Особые настройки для QR
                qrCodeConfig = QRCodeConfig().apply {
                    enabled = true
                    inverseReading = true
                    mirrorReading = true
                }
            }

            scanner.applyConfiguration(config)
            Log.d(TAG, "Scanner configured for Cyrillic support")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure scanner", e)
        }
    }

    /**
     * Поиск доступных сканеров
     */
    fun searchForScanners() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _connectionState.value = ScannerConnectionState.SEARCHING

                bluetoothManager?.searchDevices(object : SearchListener {
                    override fun onDeviceFound(scanners: List<ScannerInfo>) {
                        _availableScanners.value = scanners.filter {
                            it.deviceType == DeviceType.HR32_BT
                        }
                        Log.d(TAG, "Found ${scanners.size} scanners")
                    }

                    override fun onSearchComplete() {
                        if (_availableScanners.value.isEmpty()) {
                            _connectionState.value = ScannerConnectionState.DISCONNECTED
                        }
                    }

                    override fun onSearchError(error: SearchError) {
                        Log.e(TAG, "Search error: ${error.message}")
                        _connectionState.value = ScannerConnectionState.DISCONNECTED
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                _connectionState.value = ScannerConnectionState.DISCONNECTED
            }
        }
    }

    /**
     * Подключение к сканеру
     */
    fun connectToScanner(scannerInfo: ScannerInfo) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _connectionState.value = ScannerConnectionState.CONNECTING
                bluetoothManager?.connect(scannerInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                _connectionState.value = ScannerConnectionState.DISCONNECTED
            }
        }
    }

    /**
     * Отключение от сканера
     */
    fun disconnect() {
        try {
            connectedScanner?.let { scanner ->
                bluetoothManager?.disconnect(scanner)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect failed", e)
        }
    }

    /**
     * Обработка результата сканирования
     */
    private fun processScanResult(scanData: ScanData) {
        try {
            Log.d(TAG, "Raw scan data: ${scanData.rawData}")
            Log.d(TAG, "Symbology: ${scanData.symbology}")
            Log.d(TAG, "Character set: ${scanData.characterSet}")

            // Декодируем данные с учетом кодировки
            val decodedData = when (scanData.characterSet) {
                CharacterSet.UTF8 -> {
                    // Данные уже в UTF-8
                    String(scanData.rawData, Charsets.UTF_8)
                }
                CharacterSet.WINDOWS_1251 -> {
                    // Конвертируем из Windows-1251
                    String(scanData.rawData, charset("Windows-1251"))
                }
                CharacterSet.ISO_8859_1 -> {
                    // Попробуем интерпретировать как UTF-8
                    try {
                        String(scanData.rawData, Charsets.UTF_8)
                    } catch (e: Exception) {
                        String(scanData.rawData, Charsets.ISO_8859_1)
                    }
                }
                else -> {
                    // Автоопределение кодировки
                    detectAndDecodeString(scanData.rawData)
                }
            }

            // Создаем обогащенные данные сканирования
            val enrichedScanData = EnhancedScanData(
                data = decodedData,
                rawData = scanData.rawData,
                symbology = scanData.symbology,
                aimId = scanData.aimId,
                codeId = scanData.codeId,
                timestamp = System.currentTimeMillis(),
                quality = scanData.quality,
                characterSet = scanData.characterSet
            )

            _lastScannedData.value = enrichedScanData
            Log.d(TAG, "Processed scan result: $decodedData")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process scan result", e)
        }
    }

    /**
     * Автоопределение и декодирование строки
     */
    private fun detectAndDecodeString(rawData: ByteArray): String {
        // Пробуем разные кодировки
        val encodings = listOf(
            Charsets.UTF_8,
            charset("Windows-1251"),
            Charsets.ISO_8859_1
        )

        for (encoding in encodings) {
            try {
                val decoded = String(rawData, encoding)
                // Проверяем наличие кириллицы
                if (decoded.any { it in '\u0400'..'\u04FF' }) {
                    return decoded
                }
            } catch (e: Exception) {
                continue
            }
        }

        // Если ничего не подошло, возвращаем UTF-8
        return String(rawData, Charsets.UTF_8)
    }

    /**
     * Получение расширенной информации о сканере
     */
    fun getScannerInfo(): ScannerDetailedInfo? {
        return connectedScanner?.let { scanner ->
            ScannerDetailedInfo(
                name = scanner.deviceInfo.name,
                address = scanner.deviceInfo.address,
                model = scanner.deviceInfo.model,
                firmwareVersion = scanner.deviceInfo.firmwareVersion,
                serialNumber = scanner.deviceInfo.serialNumber,
                batteryLevel = scanner.getBatteryLevel(),
                supportedSymbologies = scanner.getSupportedSymbologies(),
                currentConfig = scanner.getCurrentConfiguration()
            )
        }
    }

    /**
     * Настройка специфичных параметров для разных типов QR
     */
    fun configureForQRType(qrType: QRType) {
        connectedScanner?.let { scanner ->
            val config = when (qrType) {
                QRType.CYRILLIC_HEAVY -> ScannerConfig().apply {
                    characterSet = CharacterSet.UTF8
                    dataFormat = DataFormat.RAW_WITH_HEADER
                    hidKeyboardLayout = KeyboardLayout.RUSSIAN
                    qrCodeConfig = QRCodeConfig().apply {
                        errorCorrection = ErrorCorrectionLevel.HIGH
                        inverseReading = true
                        mirrorReading = true
                    }
                }
                QRType.MIXED_CONTENT -> ScannerConfig().apply {
                    characterSet = CharacterSet.AUTO_DETECT
                    dataFormat = DataFormat.HEX_WITH_PREFIX
                    prefix = "\\x"
                }
                QRType.STANDARD_LATIN -> ScannerConfig().apply {
                    characterSet = CharacterSet.ASCII
                    dataFormat = DataFormat.PLAIN
                }
            }

            try {
                scanner.applyConfiguration(config)
                Log.d(TAG, "Configured for QR type: $qrType")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to configure for QR type", e)
            }
        }
    }

    fun cleanup() {
        try {
            disconnect()
            scannerManager?.cleanup()
            bluetoothManager?.cleanup()
            NLSDKManager.getInstance().cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
        }
    }
}