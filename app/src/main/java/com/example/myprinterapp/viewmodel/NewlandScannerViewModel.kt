package com.example.myprinterapp.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.scanner.NewlandBleService
import com.example.myprinterapp.scanner.NewlandConnectionState
import com.example.myprinterapp.scanner.QrGenerationState
import com.example.myprinterapp.scanner.ScannedData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для управления Newland BLE сканером
 */
@HiltViewModel
class NewlandScannerViewModel @Inject constructor(
    private val newlandBleService: NewlandBleService
) : ViewModel() {

    companion object {
        private const val TAG = "NewlandScannerVM"
    }

    // Состояния из сервиса
    val connectionState: StateFlow<NewlandConnectionState> = newlandBleService.connectionState
    val qrGenerationState: StateFlow<QrGenerationState> = newlandBleService.qrGenerationState
    val pairingQrBitmap: StateFlow<Bitmap?> = newlandBleService.pairingQrBitmap
    val connectedDevice: StateFlow<String?> = newlandBleService.connectedDevice
        .map { it?.name }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Показывать ли диалог сопряжения
    private val _showPairingDialog = MutableStateFlow(false)
    val showPairingDialog: StateFlow<Boolean> = _showPairingDialog.asStateFlow()

    // Обработчик отсканированных данных
    private val _onScanCallback: MutableStateFlow<((String) -> Unit)?> = MutableStateFlow(null)

    init {
        // Подписываемся на получение данных со сканера
        viewModelScope.launch {
            newlandBleService.lastScannedData.collect { scannedData ->
                scannedData?.let {
                    handleScannedData(it)
                }
            }
        }
    }

    /**
     * Показать диалог сопряжения
     */
    fun showPairingDialog(onScanCallback: (String) -> Unit) {
        Log.d(TAG, "Showing pairing dialog")
        _onScanCallback.value = onScanCallback
        _showPairingDialog.value = true

        // Если сканер уже подключен, не генерируем QR
        if (connectionState.value != NewlandConnectionState.CONNECTED) {
            generatePairingQr()
        }
    }

    /**
     * Скрыть диалог сопряжения
     */
    fun hidePairingDialog() {
        Log.d(TAG, "Hiding pairing dialog")
        _showPairingDialog.value = false
        _onScanCallback.value = null

        // Останавливаем режим ожидания подключения
        if (connectionState.value != NewlandConnectionState.CONNECTED) {
            newlandBleService.stopFineScanToConnect()
        }
    }

    /**
     * Генерация QR для сопряжения
     */
    fun generatePairingQr() {
        Log.d(TAG, "Generating pairing QR")
        viewModelScope.launch {
            newlandBleService.generatePairingQr()
        }
    }

    /**
     * Проверка подключения сканера
     */
    fun checkConnection(): Boolean {
        return newlandBleService.isConnected()
    }

    /**
     * Отключить сканер
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting scanner")
        newlandBleService.disconnect()
    }

    /**
     * Обработка отсканированных данных
     */
    private fun handleScannedData(data: ScannedData) {
        Log.d(TAG, "Handling scanned data: ${data.decodedData}")

        // Вызываем callback если он установлен
        _onScanCallback.value?.invoke(data.decodedData)

        // Очищаем данные после обработки
        newlandBleService.clearLastScannedData()
    }

    /**
     * Установить обработчик сканирования для конкретного экрана
     */
    fun setScanCallback(callback: (String) -> Unit) {
        _onScanCallback.value = callback
    }

    /**
     * Очистить обработчик сканирования
     */
    fun clearScanCallback() {
        _onScanCallback.value = null
    }

    override fun onCleared() {
        super.onCleared()
        newlandBleService.cleanup()
    }
}