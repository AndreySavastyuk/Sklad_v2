package com.example.myprinterapp.ui.demo

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.scanner.BleConnectionState
import com.example.myprinterapp.scanner.NewlandBleService
import com.example.myprinterapp.scanner.ScannedData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerTestViewModel @Inject constructor(
    private val bleService: NewlandBleService
) : ViewModel() {

    // Прямое проксирование состояний из сервиса
    val connectionState: StateFlow<BleConnectionState> = bleService.connectionState
    val pairingQr: StateFlow<Bitmap?> = bleService.pairingQrBitmap
    val lastScan: StateFlow<ScannedData?> = bleService.lastScannedData

    // Локальные состояния
    private val _scanHistory = MutableStateFlow<List<ScannedData>>(emptyList())
    val scanHistory: StateFlow<List<ScannedData>> = _scanHistory.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    init {
        // Подписываемся на новые сканирования
        viewModelScope.launch {
            bleService.lastScannedData.collect { scan ->
                scan?.let {
                    addToHistory(it)
                }
            }
        }

        // Обновляем информацию об устройстве
        viewModelScope.launch {
            bleService.connectionState.collect { state ->
                if (state == BleConnectionState.CONNECTED) {
                    updateDeviceInfo()
                } else if (state == BleConnectionState.DISCONNECTED) {
                    _deviceInfo.value = null
                }
            }
        }

        // Проверяем уже подключенные устройства
        bleService.checkConnectedDevices()
    }

    fun startPairing() {
        bleService.startPairing()
    }

    fun stopPairing() {
        bleService.stopPairing()
    }

    fun disconnect() {
        bleService.disconnect()
        clearHistory()
    }

    fun clearHistory() {
        _scanHistory.value = emptyList()
    }

    private fun addToHistory(scan: ScannedData) {
        val currentHistory = _scanHistory.value.toMutableList()
        currentHistory.add(0, scan) // Добавляем в начало

        // Ограничиваем историю 50 записями
        if (currentHistory.size > 50) {
            _scanHistory.value = currentHistory.take(50)
        } else {
            _scanHistory.value = currentHistory
        }
    }

    private fun updateDeviceInfo() {
        bleService.getDeviceInfo()?.let { info ->
            _deviceInfo.value = DeviceInfo(
                name = info.name,
                address = info.address
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Не вызываем cleanup здесь, так как сервис singleton
        // и должен продолжать работать для других экранов
    }
}