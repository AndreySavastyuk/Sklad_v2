package com.example.myprinterapp.scanner

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.nlscan.ble.NlsBleManager
import com.nlscan.ble.NlsBleDevice
import com.nlscan.ble.NlsBleDefaultEventObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис для работы со сканерами Newland через официальный BLE SDK
 */
@Singleton
class NewlandBleService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "NewlandBleService"
    }

    private val bleManager: NlsBleManager = NlsBleManager.getInstance()

    // Состояние подключения
    private val _connectionState = MutableStateFlow(NewlandConnectionState.DISCONNECTED)
    val connectionState: StateFlow<NewlandConnectionState> = _connectionState.asStateFlow()

    // Подключенное устройство
    private val _connectedDevice = MutableStateFlow<NewlandDeviceInfo?>(null)
    val connectedDevice: StateFlow<NewlandDeviceInfo?> = _connectedDevice.asStateFlow()

    // Последние отсканированные данные
    private val _lastScannedData = MutableStateFlow<ScannedData?>(null)
    val lastScannedData: StateFlow<ScannedData?> = _lastScannedData.asStateFlow()

    // QR код для сопряжения
    private val _pairingQrBitmap = MutableStateFlow<Bitmap?>(null)
    val pairingQrBitmap: StateFlow<Bitmap?> = _pairingQrBitmap.asStateFlow()

    // Статус генерации QR
    private val _qrGenerationState = MutableStateFlow(QrGenerationState.IDLE)
    val qrGenerationState: StateFlow<QrGenerationState> = _qrGenerationState.asStateFlow()

    // Observer для событий BLE
    private val bleObserver = object : NlsBleDefaultEventObserver() {
        override fun onConnectionStateChanged(device: NlsBleDevice) {
            Log.d(TAG, "Connection state changed: ${device.connectionState}")

            when (device.connectionState) {
                NlsBleManager.CONNECTION_STATE_CONNECTED -> {
                    Log.i(TAG, "Scanner connected: ${device.address}")
                    _connectionState.value = NewlandConnectionState.CONNECTED
                    _connectedDevice.value = NewlandDeviceInfo(
                        address = device.address,
                        name = device.name ?: "Newland Scanner"
                    )
                    // Автоматически начинаем прием данных
                    startScanning()
                }

                NlsBleManager.CONNECTION_STATE_DISCONNECTED -> {
                    Log.i(TAG, "Scanner disconnected")
                    _connectionState.value = NewlandConnectionState.DISCONNECTED
                    _connectedDevice.value = null
                }

                NlsBleManager.CONNECTION_STATE_CONNECTING -> {
                    Log.d(TAG, "Scanner connecting...")
                    _connectionState.value = NewlandConnectionState.CONNECTING
                }

                else -> {
                    Log.w(TAG, "Unknown connection state: ${device.connectionState}")
                }
            }
        }

        override fun onScanDataReceived(data: String) {
            Log.d(TAG, "Scan data received: $data")

            // Данные приходят уже в UTF-8, включая кириллицу
            _lastScannedData.value = ScannedData(
                rawData = data,
                decodedData = data, // Уже декодировано SDK
                timestamp = System.currentTimeMillis(),
                format = "QR_CODE" // SDK не всегда передает формат
            )
        }

        override fun onError(errorCode: Int, errorMessage: String?) {
            Log.e(TAG, "BLE Error: $errorCode - $errorMessage")
            _connectionState.value = NewlandConnectionState.ERROR
        }
    }

    init {
        // Регистрируем observer при создании сервиса
        bleManager.registerBleEventObserver(bleObserver)
    }

    /**
     * Генерация QR-кода для сопряжения сканера
     */
    fun generatePairingQr() {
        Log.d(TAG, "Generating pairing QR code...")
        _qrGenerationState.value = QrGenerationState.GENERATING

        bleManager.generateConnectCodeBitmap { bitmap ->
            if (bitmap != null) {
                Log.i(TAG, "QR code generated successfully")
                _pairingQrBitmap.value = bitmap
                _qrGenerationState.value = QrGenerationState.READY

                // Запускаем режим ожидания подключения
                startFineScanToConnect()
            } else {
                Log.e(TAG, "Failed to generate QR code")
                _qrGenerationState.value = QrGenerationState.ERROR
            }
        }
    }

    /**
     * Запуск режима "Fine Scan to Connect"
     * После сканирования QR-кода сканер автоматически подключится
     */
    private fun startFineScanToConnect() {
        Log.d(TAG, "Starting Fine Scan to Connect mode...")
        bleManager.startFineScanToConnect()
    }

    /**
     * Остановка режима ожидания подключения
     */
    fun stopFineScanToConnect() {
        Log.d(TAG, "Stopping Fine Scan to Connect mode...")
        bleManager.stopFineScanToConnect()
        _pairingQrBitmap.value = null
        _qrGenerationState.value = QrGenerationState.IDLE
    }

    /**
     * Начать прием данных со сканера
     */
    private fun startScanning() {
        Log.d(TAG, "Starting scan data reception...")
        bleManager.startScan()
    }

    /**
     * Остановить прием данных
     */
    fun stopScanning() {
        Log.d(TAG, "Stopping scan data reception...")
        bleManager.stopScan()
    }

    /**
     * Отключить сканер
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting scanner...")
        stopScanning()
        stopFineScanToConnect()
        // SDK автоматически обработает отключение
    }

    /**
     * Очистить последние отсканированные данные
     */
    fun clearLastScannedData() {
        _lastScannedData.value = null
    }

    /**
     * Проверка, подключен ли сканер
     */
    fun isConnected(): Boolean {
        return _connectionState.value == NewlandConnectionState.CONNECTED
    }

    /**
     * Освобождение ресурсов
     */
    fun cleanup() {
        disconnect()
        bleManager.unregisterBleEventObserver(bleObserver)
    }
}

// Состояния подключения
enum class NewlandConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

// Состояния генерации QR
enum class QrGenerationState {
    IDLE,
    GENERATING,
    READY,
    ERROR
}

// Информация об устройстве
data class NewlandDeviceInfo(
    val address: String,
    val name: String
)

// Отсканированные данные
data class ScannedData(
    val rawData: String,
    val decodedData: String,
    val timestamp: Long,
    val format: String
)