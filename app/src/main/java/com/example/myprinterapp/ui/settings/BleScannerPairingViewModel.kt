package com.example.myprinterapp.ui.settings

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.data.models.DeviceInfo
import com.example.myprinterapp.scanner.BleScannerManager
import com.example.myprinterapp.scanner.BleConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BleScannerPairingViewModel @Inject constructor(
    private val bleScannerManager: BleScannerManager
) : ViewModel() {

    // Состояния из BLE Scanner Manager
    val connectionState: StateFlow<BleConnectionState> = bleScannerManager.connectionState
    val pairingQrCode: StateFlow<Bitmap?> = bleScannerManager.pairingQrCode
    val connectedDevice: StateFlow<DeviceInfo?> = bleScannerManager.connectedDevice
    val scanResult = bleScannerManager.scanResult

    /**
     * Начало процесса сопряжения
     */
    fun startPairing() {
        Timber.d("Starting BLE scanner pairing")
        
        // Используем новый метод connectScanner
        bleScannerManager.connectScanner { success, message ->
            if (success) {
                Timber.d("Pairing started successfully: $message")
            } else {
                Timber.e("Failed to start pairing: $message")
            }
        }
    }

    /**
     * Остановка процесса сопряжения
     */
    fun stopPairing() {
        Timber.d("Stopping BLE scanner pairing")
        bleScannerManager.disconnectScanner()
    }

    /**
     * Обновление QR-кода сопряжения
     */
    fun refreshPairing() {
        Timber.d("Refreshing BLE scanner pairing")
        stopPairing()
        // Небольшая задержка перед повторным запуском
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            startPairing()
        }
    }

    /**
     * Отключение сканера
     */
    fun disconnectScanner() {
        Timber.d("Disconnecting BLE scanner")
        bleScannerManager.disconnectScanner()
    }

    /**
     * Очистка результатов сканирования
     */
    fun clearScanResult() {
        bleScannerManager.clearScanResult()
    }

    override fun onCleared() {
        super.onCleared()
        // Останавливаем сопряжение при уничтожении ViewModel
        bleScannerManager.disconnectScanner()
    }
} 