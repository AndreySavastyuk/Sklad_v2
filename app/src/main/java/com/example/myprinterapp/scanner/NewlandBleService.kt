package com.example.myprinterapp.scanner

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.nlscan.ble.NlsBleManager
import com.nlscan.ble.NlsBleDevice
import com.nlscan.ble.NlsBleDefaultEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис для работы с Newland BLE сканерами
 * Использует официальный SDK производителя
 */
@Singleton
class NewlandBleService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "NewlandBleService"
    }

    private val bleManager: NlsBleManager = NlsBleManager.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main)

    // Состояние подключения
    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    // Подключенное устройство
    private val _connectedDevice = MutableStateFlow<NlsBleDevice?>(null)
    val connectedDevice: StateFlow<NlsBleDevice?> = _connectedDevice.asStateFlow()

    // Последний отсканированный код
    private val _lastScannedData = MutableStateFlow<ScannedData?>(null)
    val lastScannedData: StateFlow<ScannedData?> = _lastScannedData.asStateFlow()

    // QR-код для сопряжения
    private val _pairingQrBitmap = MutableStateFlow<Bitmap?>(null)
    val pairingQrBitmap: StateFlow<Bitmap?> = _pairingQrBitmap.asStateFlow()

    // Активно ли сканирование
    private val _isScanningActive = MutableStateFlow(false)
    val isScanningActive: StateFlow<Boolean> = _isScanningActive.asStateFlow()

    // Observer для событий BLE
    private val bleObserver = object : NlsBleDefaultEventObserver() {
        override fun onConnectionStateChanged(device: NlsBleDevice) {
            Log.d(TAG, "Connection state changed: ${device.connectionState} for ${device.address}")

            when (device.connectionState) {
                NlsBleManager.CONNECTION_STATE_CONNECTED -> {
                    Log.i(TAG, "Scanner connected: ${device.address}")
                    _connectedDevice.value = device
                    _connectionState.value = BleConnectionState.CONNECTED

                    // Автоматически запускаем сканирование при подключении
                    startScanning()
                }

                NlsBleManager.CONNECTION_STATE_DISCONNECTED -> {
                    Log.i(TAG, "Scanner disconnected")
                    _connectedDevice.value = null
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    _isScanningActive.value = false
                }

                NlsBleManager.CONNECTION_STATE_CONNECTING -> {
                    Log.d(TAG, "Connecting to scanner...")
                    _connectionState.value = BleConnectionState.CONNECTING
                }

                else -> {
                    Log.d(TAG, "Unknown connection state: ${device.connectionState}")
                }
            }
        }

        override fun onScanDataReceived(data: String) {
            Log.d(TAG, "Scan data received: $data")

            // Создаем объект с данными сканирования
            val scannedData = ScannedData(
                data = data,
                timestamp = System.currentTimeMillis(),
                deviceAddress = _connectedDevice.value?.address ?: "Unknown"
            )

            _lastScannedData.value = scannedData

            // Логируем для отладки
            if (data.any { it in '\u0400'..'\u04FF' }) {
                Log.d(TAG, "Cyrillic detected in scanned data: $data")
            }
        }

        override fun onError(errorCode: Int, errorMsg: String?) {
            Log.e(TAG, "BLE Error: code=$errorCode, msg=$errorMsg")
            _connectionState.value = BleConnectionState.ERROR
        }
    }

    init {
        // Регистрируем observer при создании сервиса
        bleManager.registerBleEventObserver(bleObserver)
    }

    /**
     * Генерирует QR-код для сопряжения и запускает автоподключение
     */
    fun startPairing() {
        scope.launch {
            Log.d(TAG, "Starting pairing process...")
            _connectionState.value = BleConnectionState.PAIRING

            // Генерируем QR-код для сопряжения
            bleManager.generateConnectCodeBitmap { bitmap ->
                if (bitmap != null) {
                    Log.d(TAG, "Pairing QR code generated successfully")
                    _pairingQrBitmap.value = bitmap

                    // Запускаем режим "fine scan to connect"
                    bleManager.startFineScanToConnect()
                } else {
                    Log.e(TAG, "Failed to generate pairing QR code")
                    _connectionState.value = BleConnectionState.ERROR
                }
            }
        }
    }

    /**
     * Останавливает процесс сопряжения
     */
    fun stopPairing() {
        Log.d(TAG, "Stopping pairing process...")
        bleManager.stopFineScanToConnect()
        _pairingQrBitmap.value = null

        if (_connectionState.value == BleConnectionState.PAIRING) {
            _connectionState.value = BleConnectionState.DISCONNECTED
        }
    }

    /**
     * Запускает сканирование (прием данных от сканера)
     */
    fun startScanning() {
        if (_connectionState.value == BleConnectionState.CONNECTED) {
            Log.d(TAG, "Starting scanning...")
            bleManager.startScan()
            _isScanningActive.value = true
        } else {
            Log.w(TAG, "Cannot start scanning - scanner not connected")
        }
    }

    /**
     * Останавливает сканирование
     */
    fun stopScanning() {
        Log.d(TAG, "Stopping scanning...")
        bleManager.stopScan()
        _isScanningActive.value = false
    }

    /**
     * Отключает текущее устройство
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting...")
        stopScanning()
        bleManager.disconnect(_connectedDevice.value)
        _connectedDevice.value = null
        _connectionState.value = BleConnectionState.DISCONNECTED
    }

    /**
     * Очищает последние отсканированные данные
     */
    fun clearLastScannedData() {
        _lastScannedData.value = null
    }

    /**
     * Проверяет, подключены ли уже какие-то устройства
     */
    fun checkConnectedDevices() {
        scope.launch {
            Log.d(TAG, "Checking for connected devices...")
            val connectedDevices = bleManager.connectedDevices

            if (connectedDevices.isNotEmpty()) {
                val device = connectedDevices.first()
                Log.d(TAG, "Found connected device: ${device.address}")
                _connectedDevice.value = device
                _connectionState.value = BleConnectionState.CONNECTED
                startScanning()
            }
        }
    }

    /**
     * Получает информацию о подключенном устройстве
     */
    fun getDeviceInfo(): DeviceInfo? {
        return _connectedDevice.value?.let { device ->
            DeviceInfo(
                name = device.name ?: "Unknown Scanner",
                address = device.address,
                isConnected = device.connectionState == NlsBleManager.CONNECTION_STATE_CONNECTED
            )
        }
    }

    /**
     * Очистка ресурсов
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up BLE service...")
        stopScanning()
        stopPairing()
        disconnect()
        bleManager.unregisterBleEventObserver(bleObserver)
    }
}

/**
 * Состояния подключения BLE
 */
enum class BleConnectionState {
    DISCONNECTED,
    PAIRING,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Данные сканирования
 */
data class ScannedData(
    val data: String,
    val timestamp: Long,
    val deviceAddress: String
)

/**
 * Информация об устройстве
 */
data class DeviceInfo(
    val name: String,
    val address: String,
    val isConnected: Boolean
)