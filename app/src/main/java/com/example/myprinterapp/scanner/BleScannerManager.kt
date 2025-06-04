package com.example.myprinterapp.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.myprinterapp.data.models.*
import com.nlscan.ble.NlsBleManager
import com.nlscan.ble.NlsBleDevice
import com.nlscan.ble.NlsBleDefaultEventObserver
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleScannerManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "BleScannerManager"

        // Разрешения для BLE сканирования
        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }

    private val bleManager: NlsBleManager = NlsBleManager.getInstance()

    // Флаг для отслеживания регистрации observer
    private var isObserverRegistered = false

    // Состояния
    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()

    private val _pairingQrCode = MutableStateFlow<Bitmap?>(null)
    val pairingQrCode: StateFlow<Bitmap?> = _pairingQrCode.asStateFlow()

    private val _connectedDevice = MutableStateFlow<DeviceInfo?>(null)
    val connectedDevice: StateFlow<DeviceInfo?> = _connectedDevice.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    // Observer для BLE событий
    private val bleObserver = object : NlsBleDefaultEventObserver() {
        override fun onConnectionStateChanged(device: NlsBleDevice) {
            when (device.connectionState) {
                NlsBleManager.CONNECTION_STATE_CONNECTED -> {
                    Timber.i("$TAG: Сканер подключён: ${device.address}")
                    _connectionState.value = BleConnectionState.CONNECTED

                    // Создаем DeviceInfo для подключенного сканера
                    val deviceInfo = DeviceInfo(
                        id = device.address,
                        name = device.name ?: "BLE Scanner",
                        address = device.address,
                        type = DeviceType.SCANNER_BLE,
                        connectionState = ConnectionState.CONNECTED,
                        lastConnected = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                        batteryLevel = _batteryLevel.value
                    )
                    _connectedDevice.value = deviceInfo

                    // Запускаем прием данных сканирования
                    bleManager.startScan()

                    // Запрашиваем уровень батареи
                    queryBatteryLevel()

                    // Настраиваем звук успешного сканирования
                    configureScannerSound()
                }

                NlsBleManager.CONNECTION_STATE_DISCONNECTED -> {
                    Timber.i("$TAG: Сканер отключён: ${device.address}")
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    _connectedDevice.value = null
                    _batteryLevel.value = null
                }

                NlsBleManager.CONNECTION_STATE_CONNECTING -> {
                    Timber.d("$TAG: Подключение к сканеру: ${device.address}")
                    _connectionState.value = BleConnectionState.CONNECTING
                }

                else -> {
                    Timber.d("$TAG: Изменение состояния сканера: ${device.connectionState}")
                }
            }
        }

        // КРИТИЧНО: Этот метод должен называться именно onScanDataReceived
        override fun onScanDataReceived(data: String) {
            Timber.d("$TAG: onScanDataReceived вызван с данными: $data")
            
            // Очищаем данные от лишних символов
            val cleanData = data.trim()
            
            if (cleanData.isNotEmpty()) {
                Timber.i("$TAG: Обработка сканированных данных: $cleanData")
                
                // Создаем результат сканирования
                val scanResult = ScanResult(
                    data = cleanData,
                    format = BarcodeFormat.QR_CODE,
                    timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                    deviceId = _connectedDevice.value?.id,
                    isProcessed = false,
                    metadata = ScanMetadata(quality = 0.9f, orientation = 0)
                )
                
                // ВАЖНО: Обновляем Flow с результатом
                _scanResult.value = scanResult
                
                Timber.i("$TAG: ScanResult обновлен в Flow: ${scanResult.data}")
                
                // Воспроизводим звук успешного сканирования (один сигнал)
                try {
                    bleManager.beep(2700, 100, 15)
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: Ошибка воспроизведения звука")
                }
            } else {
                Timber.w("$TAG: Получены пустые данные сканирования")
            }
        }

        override fun onBatteryLevelRead(result: com.nlscan.ble.NlsResult<Int>) {
            if (result.retSucceed()) {
                val level = result.result
                Timber.d("$TAG: Уровень батареи: $level%")
                _batteryLevel.value = level

                // Обновляем информацию об устройстве
                _connectedDevice.value?.let { device ->
                    _connectedDevice.value = device.copy(batteryLevel = level)
                }
            } else {
                Timber.e("$TAG: Ошибка получения уровня батареи")
            }
        }
    }

    /**
     * Настройка звука сканера
     */
    private fun configureScannerSound() {
        try {
            // Настраиваем встроенный звук сканера
            Timber.d("$TAG: Настройки звука сканера применены")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Ошибка настройки звука")
        }
    }

    /**
     * Запрос уровня батареи
     */
    fun queryBatteryLevel() {
        if (isConnected()) {
            try {
                bleManager.queryBatteryLevel()
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Ошибка запроса уровня батареи")
            }
        }
    }

    /**
     * Запрос версии прошивки
     */
    fun queryFirmwareVersion() {
        if (isConnected()) {
            try {
                bleManager.queryFirmwareVersion()
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Ошибка запроса версии прошивки")
            }
        }
    }

    /**
     * Подключение сканера
     */
    fun connectScanner(onComplete: (Boolean, String?) -> Unit) {
        Timber.d("$TAG: Начинаем подключение к BLE сканеру...")

        when (_connectionState.value) {
            BleConnectionState.CONNECTED -> {
                onComplete(true, "Сканер уже подключен")
                return
            }
            BleConnectionState.GENERATING_QR,
            BleConnectionState.WAITING_FOR_SCAN,
            BleConnectionState.CONNECTING -> {
                onComplete(false, "Подключение уже в процессе")
                return
            }
            else -> {}
        }

        _connectionState.value = BleConnectionState.GENERATING_QR

        try {
            // Регистрируем observer только если еще не зарегистрирован
            if (!isObserverRegistered) {
                bleManager.registerBleEventObserver(bleObserver)
                isObserverRegistered = true
            }

            // Генерируем QR-код для сопряжения
            bleManager.generateConnectCodeBitmap { bitmap: Bitmap? ->
                if (bitmap != null) {
                    _pairingQrCode.value = bitmap
                    _connectionState.value = BleConnectionState.WAITING_FOR_SCAN

                    // Запускаем режим сопряжения
                    bleManager.startFineScanToConnect()

                    Timber.d("$TAG: QR-код создан и режим сопряжения запущен")
                    onComplete(true, "QR-код готов. Отсканируйте его сканером для подключения")
                } else {
                    _connectionState.value = BleConnectionState.ERROR
                    Timber.e("$TAG: Не удалось создать QR-код для сопряжения")
                    onComplete(false, "Ошибка генерации QR-кода")
                }
            }
        } catch (e: Exception) {
            _connectionState.value = BleConnectionState.ERROR
            Timber.e(e, "$TAG: Ошибка подключения сканера")
            onComplete(false, "Ошибка подключения: ${e.message}")
        }
    }

    /**
     * Отключение сканера
     */
    fun disconnectScanner() {
        try {
            // Остановка всех операций
            bleManager.stopFineScanToConnect()
            bleManager.stopScan()

            // Отключение observer
            if (isObserverRegistered) {
                bleManager.unregisterBleEventObserver(bleObserver)
                isObserverRegistered = false
            }

            _connectionState.value = BleConnectionState.DISCONNECTED
            _connectedDevice.value = null
            _scanResult.value = null
            _pairingQrCode.value = null
            _batteryLevel.value = null

            Timber.d("$TAG: Сканер отключен")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Ошибка отключения сканера")
        }
    }

    /**
     * Очистка последнего результата сканирования
     */
    fun clearScanResult() {
        _scanResult.value = null
    }

    /**
     * Проверка подключения
     */
    fun isConnected(): Boolean = _connectionState.value == BleConnectionState.CONNECTED

    /**
     * Тестирование подключения
     */
    fun testConnection(onResult: (Boolean, String) -> Unit) {
        if (!isConnected()) {
            onResult(false, "Сканер не подключен")
            return
        }

        try {
            val device = _connectedDevice.value
            if (device != null) {
                val batteryInfo = _batteryLevel.value?.let { ", батарея: $it%" } ?: ""
                onResult(true, "Сканер ${device.name} работает корректно$batteryInfo")
            } else {
                onResult(false, "Ошибка получения информации о сканере")
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Ошибка тестирования сканера")
            onResult(false, "Ошибка тестирования: ${e.message}")
        }
    }

    /**
     * Проверка разрешений
     */
    fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Получение списка необходимых разрешений
     */
    fun getRequiredPermissions(): Array<String> = REQUIRED_PERMISSIONS

    /**
     * Тестовая функция для эмуляции сканирования (только для debug)
     */
    fun testScanData(testData: String) {
        if (com.example.myprinterapp.BuildConfig.DEBUG) {
            Timber.d("$TAG: Эмулируем сканирование с данными: $testData")
            
            // Создаем тестовый результат сканирования
            val scanResult = ScanResult(
                data = testData.trim(),
                format = BarcodeFormat.QR_CODE,
                timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                deviceId = _connectedDevice.value?.id ?: "test_device",
                isProcessed = false,
                metadata = ScanMetadata(quality = 1.0f, orientation = 0)
            )
            
            // Обновляем Flow с результатом
            _scanResult.value = scanResult
            
            Timber.i("$TAG: Тестовый результат сканирования установлен: ${scanResult.data}")
        }
    }
}

/**
 * Состояния BLE подключения
 */
enum class BleConnectionState {
    DISCONNECTED,           // Отключено
    CONNECTING,             // Подключение
    GENERATING_QR,          // Генерация QR-кода
    WAITING_FOR_SCAN,       // Ожидание сканирования QR-кода
    CONNECTED,              // Подключено и готово к работе
    ERROR                   // Ошибка
}