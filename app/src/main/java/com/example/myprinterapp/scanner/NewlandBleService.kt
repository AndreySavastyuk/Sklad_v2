package com.example.myprinterapp.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис для работы со сканерами Newland через BLE
 * Упрощенная версия для работы без официального SDK
 */
@Singleton
class NewlandBleService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "NewlandBleService"
    }

    // Состояние подключения
    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    // Подключенное устройство
    private val _connectedDevice = MutableStateFlow<NewlandDeviceInfo?>(null)
    val connectedDevice: StateFlow<NewlandDeviceInfo?> = _connectedDevice.asStateFlow()

    // Последние отсканированные данные
    private val _lastScannedData = MutableStateFlow<ScannedData?>(null)
    val lastScannedData: StateFlow<ScannedData?> = _lastScannedData.asStateFlow()

    // QR код для сопряжения (заглушка)
    private val _pairingQrBitmap = MutableStateFlow<Bitmap?>(null)
    val pairingQrBitmap: StateFlow<Bitmap?> = _pairingQrBitmap.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        Log.d(TAG, "NewlandBleService initialized")
    }

    /**
     * Начать процесс сопряжения
     */
    fun startPairing() {
        Log.d(TAG, "Starting pairing process")
        _connectionState.value = BleConnectionState.PAIRING

        // Эмулируем генерацию QR-кода
        coroutineScope.launch {
            delay(1000) // Имитация генерации
            generateMockQrCode()

            // Эмулируем подключение через 5 секунд
            delay(5000)
            if (_connectionState.value == BleConnectionState.PAIRING) {
                _connectionState.value = BleConnectionState.CONNECTING
                delay(2000)
                onDeviceConnected()
            }
        }
    }

    /**
     * Остановить процесс сопряжения
     */
    fun stopPairing() {
        Log.d(TAG, "Stopping pairing process")
        _connectionState.value = BleConnectionState.DISCONNECTED
        _pairingQrBitmap.value = null
    }

    /**
     * Отключить устройство
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting device")
        _connectionState.value = BleConnectionState.DISCONNECTED
        _connectedDevice.value = null
        _lastScannedData.value = null
    }

    /**
     * Проверить подключенные устройства
     */
    fun checkConnectedDevices() {
        Log.d(TAG, "Checking for connected devices")
        // В реальном приложении здесь была бы проверка Bluetooth устройств
        // Для демонстрации просто проверяем сохраненное состояние
        if (_connectedDevice.value != null) {
            _connectionState.value = BleConnectionState.CONNECTED
        }
    }

    /**
     * Получить информацию об устройстве
     */
    fun getDeviceInfo(): NewlandDeviceInfo? {
        return _connectedDevice.value
    }

    /**
     * Эмуляция сканирования (для тестирования)
     */
    fun emulateScan(data: String) {
        if (_connectionState.value == BleConnectionState.CONNECTED) {
            _lastScannedData.value = ScannedData(
                data = data,
                rawData = data,
                timestamp = System.currentTimeMillis(),
                format = "QR_CODE"
            )
        }
    }

    /**
     * Очистить последние отсканированные данные
     */
    fun clearLastScannedData() {
        _lastScannedData.value = null
    }

    /**
     * Генерация заглушки QR-кода
     */
    private fun generateMockQrCode() {
        // Создаем простой Bitmap для демонстрации
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)

        // В реальном приложении здесь был бы настоящий QR-код
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 20f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("MOCK QR CODE", 150f, 150f, paint)

        _pairingQrBitmap.value = bitmap
        Log.d(TAG, "Mock QR code generated")
    }

    /**
     * Обработка подключения устройства
     */
    private fun onDeviceConnected() {
        Log.d(TAG, "Device connected")
        _connectionState.value = BleConnectionState.CONNECTED
        _connectedDevice.value = NewlandDeviceInfo(
            address = "00:11:22:33:44:55",
            name = "Newland HR32-BT (Demo)"
        )
        _pairingQrBitmap.value = null
    }

    /**
     * Очистка ресурсов
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up resources")
        disconnect()
    }
}

// Единый enum для состояний подключения
enum class BleConnectionState {
    DISCONNECTED,
    PAIRING,
    CONNECTING,
    CONNECTED,
    ERROR
}

// Информация об устройстве
data class NewlandDeviceInfo(
    val address: String,
    val name: String
)

// Отсканированные данные
data class ScannedData(
    val data: String,
    val rawData: String,
    val timestamp: Long,
    val format: String
)