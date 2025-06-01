package com.example.myprinterapp.scanner

import com.example.myprinterapp.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import android.util.Log

/**
 * Интерфейс для управления сканерами
 */
interface ScannerManager {
    /**
     * Подключение к устройству
     */
    suspend fun connectDevice(deviceId: String, deviceType: DeviceType): DeviceInfo
    
    /**
     * Получение списка доступных устройств
     */
    fun getAvailableDevices(): Flow<List<DeviceInfo>>
    
    /**
     * Получение устройства по умолчанию
     */
    suspend fun getDefaultDevice(): DeviceInfo?
    
    /**
     * Отключение от устройства
     */
    suspend fun disconnectDevice(deviceId: String)
    
    /**
     * Проверка состояния подключения
     */
    suspend fun isConnected(deviceId: String): Boolean
    
    /**
     * Получение последнего отсканированного результата
     */
    fun getLastScanResult(): Flow<ScanResult?>
    
    /**
     * Начать сканирование
     */
    suspend fun startScanning(): Result<Unit>
    
    /**
     * Остановить сканирование
     */
    suspend fun stopScanning()
    
    /**
     * Получить состояние сканирования
     */
    fun getScanningState(): Flow<ScanningState>
}

enum class ScanningState {
    IDLE,
    SCANNING,
    PAUSED,
    ERROR
}

/**
 * Улучшенная реализация ScannerManager
 */
class ScannerManagerImpl(
    private val context: android.content.Context
) : ScannerManager {
    
    private val connectedDevices = mutableMapOf<String, DeviceInfo>()
    private var defaultDevice: DeviceInfo? = null
    
    private val _lastScanResult = MutableStateFlow<ScanResult?>(null)
    private val _scanningState = MutableStateFlow(ScanningState.IDLE)
    
    init {
        // Имитируем автоподключение к устройству по умолчанию
        setupDefaultDevice()
    }
    
    private fun setupDefaultDevice() {
        val defaultScanner = DeviceInfo(
            id = "default_scanner",
            name = "Встроенная камера",
            address = "internal_camera",
            type = DeviceType.SCANNER_BLE,
            connectionState = ConnectionState.CONNECTED,
            lastConnected = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            batteryLevel = null
        )
        
        connectedDevices["default_scanner"] = defaultScanner
        defaultDevice = defaultScanner
        
        Log.d("ScannerManager", "Настроено устройство по умолчанию: ${defaultScanner.name}")
    }
    
    override suspend fun connectDevice(deviceId: String, deviceType: DeviceType): DeviceInfo {
        Log.d("ScannerManager", "Подключение к сканеру: $deviceId")
        
        // Имитируем процесс подключения
        kotlinx.coroutines.delay(2000)
        
        val deviceName = when (deviceType) {
            DeviceType.SCANNER_BLE -> "BLE Scanner"
            DeviceType.SCANNER_BLUETOOTH -> "BT Scanner"
            else -> "Unknown Scanner"
        }
        
        val device = DeviceInfo(
            id = deviceId,
            name = "$deviceName - ${deviceId.takeLast(4)}",
            address = generateMockAddress(deviceType),
            type = deviceType,
            connectionState = ConnectionState.CONNECTED,
            lastConnected = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            batteryLevel = if (deviceType == DeviceType.SCANNER_BLE) (60..100).random() else null
        )
        
        connectedDevices[deviceId] = device
        
        // Устанавливаем как устройство по умолчанию если это первое подключенное внешнее устройство
        if (defaultDevice?.id == "default_scanner") {
            defaultDevice = device
        }
        
        Log.i("ScannerManager", "Успешно подключено к ${device.name}")
        return device
    }
    
    override fun getAvailableDevices(): Flow<List<DeviceInfo>> {
        Log.d("ScannerManager", "Поиск доступных сканеров")
        
        // Возвращаем список доступных устройств включая подключенные
        val availableDevices = listOf(
            DeviceInfo(
                id = "mock_ble_scanner_001",
                name = "Newland BLE Scanner #1",
                address = "NL:BL:E0:01:23:45",
                type = DeviceType.SCANNER_BLE,
                connectionState = ConnectionState.DISCONNECTED,
                batteryLevel = 85
            ),
            DeviceInfo(
                id = "mock_bt_scanner_001",
                name = "Bluetooth Scanner #1",
                address = "AA:BB:CC:DD:EE:F1",
                type = DeviceType.SCANNER_BLUETOOTH,
                connectionState = ConnectionState.DISCONNECTED,
                batteryLevel = 65
            ),
            DeviceInfo(
                id = "mock_ble_scanner_002",
                name = "OnSemi BLE Scanner #2",
                address = "OS:BL:E0:02:34:56",
                type = DeviceType.SCANNER_BLE,
                connectionState = ConnectionState.DISCONNECTED,
                batteryLevel = 92
            )
        ).plus(connectedDevices.values.toList())
        
        return flowOf(availableDevices.distinctBy { it.id })
    }
    
    override suspend fun getDefaultDevice(): DeviceInfo? {
        return defaultDevice
    }
    
    override suspend fun disconnectDevice(deviceId: String) {
        val device = connectedDevices.remove(deviceId)
        if (device != null) {
            Log.d("ScannerManager", "Отключение сканера: ${device.name}")
            
            // Если это было устройство по умолчанию, возвращаемся к встроенной камере
            if (defaultDevice?.id == deviceId) {
                setupDefaultDevice()
            }
        }
    }
    
    override suspend fun isConnected(deviceId: String): Boolean {
        return connectedDevices.containsKey(deviceId)
    }
    
    override fun getLastScanResult(): Flow<ScanResult?> {
        return _lastScanResult.asStateFlow()
    }
    
    override suspend fun startScanning(): Result<Unit> {
        return try {
            Log.d("ScannerManager", "Начало сканирования")
            _scanningState.value = ScanningState.SCANNING
            
            // Проверяем подключение
            val device = defaultDevice
            if (device == null) {
                _scanningState.value = ScanningState.ERROR
                return Result.Error("Нет подключенного сканера")
            }
            
            // Имитируем процесс сканирования (в реальном приложении здесь был бы запуск камеры или BLE сканера)
            Log.i("ScannerManager", "Сканирование запущено на устройстве: ${device.name}")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            Log.e("ScannerManager", "Ошибка запуска сканирования", e)
            _scanningState.value = ScanningState.ERROR
            Result.Error("Ошибка запуска сканирования: ${e.message}", e)
        }
    }
    
    override suspend fun stopScanning() {
        Log.d("ScannerManager", "Остановка сканирования")
        _scanningState.value = ScanningState.IDLE
    }
    
    override fun getScanningState(): Flow<ScanningState> {
        return _scanningState.asStateFlow()
    }
    
    /**
     * Симуляция получения результата сканирования (для тестирования)
     * В реальном приложении это вызывалось бы из callback'ов камеры или BLE устройства
     */
    fun simulateScanResult(data: String, format: BarcodeFormat = BarcodeFormat.QR_CODE) {
        val scanResult = ScanResult(
            data = data,
            format = format,
            deviceId = defaultDevice?.id,
            isProcessed = false,
            metadata = ScanMetadata(
                quality = 0.9f,
                orientation = 0
            )
        )
        
        _lastScanResult.value = scanResult
        _scanningState.value = ScanningState.IDLE
        
        Log.i("ScannerManager", "Получен результат сканирования: $data")
    }
    
    private fun generateMockAddress(deviceType: DeviceType): String {
        return when (deviceType) {
            DeviceType.SCANNER_BLE -> {
                // BLE MAC-адрес
                (1..6).map { 
                    (0..255).random().toString(16).padStart(2, '0').uppercase() 
                }.joinToString(":")
            }
            DeviceType.SCANNER_BLUETOOTH -> {
                // Bluetooth MAC-адрес
                (1..6).map { 
                    (0..255).random().toString(16).padStart(2, '0').uppercase() 
                }.joinToString(":")
            }
            else -> "unknown_address"
        }
    }
} 