package com.example.myprinterapp.ui.settings

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.data.PrinterSettings
import com.example.myprinterapp.printer.ConnectionState
import com.example.myprinterapp.data.models.LabelData
import com.example.myprinterapp.data.models.LabelType
import com.example.myprinterapp.printer.PrinterService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(
        val isConnected: Boolean = false,
        val selectedDevice: String? = null,
        val pairedDevices: List<BluetoothDeviceInfo> = emptyList()
    ) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

data class BluetoothDeviceInfo(
    val name: String?,
    val address: String
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val printerService: PrinterService,
    private val printerSettings: PrinterSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val connectionState = printerService.connectionState
    val printerName = MutableStateFlow(printerSettings.printerName)
    val printerMac = MutableStateFlow(printerSettings.printerMacAddress)

    init {
        _uiState.value = SettingsUiState.Success(
            isConnected = connectionState.value == ConnectionState.CONNECTED,
            selectedDevice = printerSettings.printerMacAddress
        )
    }

    fun selectPrinter(device: BluetoothDevice) {
        try {
            printerSettings.printerMacAddress = device.address
            printerSettings.printerMacAddress = device.address
            printerSettings.printerName = try {
                device.name ?: "Неизвестный принтер"
            } catch (e: SecurityException) {
                "Неизвестный принтер"
            }
            printerName.value = printerSettings.printerName
            printerMac.value = printerSettings.printerMacAddress
            _uiState.value = SettingsUiState.Success(
                selectedDevice = device.address
            )
        } catch (e: Exception) {
            _uiState.value = SettingsUiState.Error("Ошибка при выборе принтера: ${e.message}")
        }
    }

    fun connectPrinter() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val mac = printerMac.value
            
            if (mac.isEmpty()) {
                _uiState.value = SettingsUiState.Error("Принтер не выбран. Сначала выберите принтер для подключения.")
                return@launch
            }
            
            // Проверяем формат MAC-адреса
            val macRegex = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()
            if (!macRegex.matches(mac)) {
                _uiState.value = SettingsUiState.Error("Неверный формат MAC-адреса: $mac")
                return@launch
            }
            
            timber.log.Timber.d("Attempting to connect to printer with MAC: $mac")
            
            try {
                printerService.connect(mac).fold(
                    onSuccess = {
                        timber.log.Timber.i("Successfully connected to printer: $mac")
                        _uiState.value = SettingsUiState.Success(
                            isConnected = true,
                            selectedDevice = mac
                        )
                    },
                    onFailure = { error ->
                        timber.log.Timber.e(error, "Failed to connect to printer: $mac")
                        val errorMessage = when {
                            error.message?.contains("timeout", ignoreCase = true) == true -> 
                                "Превышено время ожидания подключения. Проверьте:\n• Принтер включен\n• Bluetooth активен\n• Принтер находится в зоне действия"
                            error.message?.contains("failed", ignoreCase = true) == true -> 
                                "Не удалось подключиться к принтеру. Проверьте:\n• MAC-адрес ($mac) правильный\n• Принтер не подключен к другому устройству"
                            else -> 
                                "Ошибка подключения: ${error.message}\n\nПроверьте:\n• Принтер включен и готов к работе\n• Bluetooth разрешения предоставлены"
                        }
                        _uiState.value = SettingsUiState.Error(errorMessage)
                    }
                )
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Unexpected error during printer connection")
                _uiState.value = SettingsUiState.Error("Неожиданная ошибка: ${e.message}")
            }
        }
    }

    fun disconnectPrinter() {
        printerService.disconnect()
        _uiState.value = SettingsUiState.Success(
            isConnected = false,
            selectedDevice = printerMac.value
        )
    }

    fun selectPrinterManually(macAddress: String, printerName: String = "Термопринтер") {
        try {
            val cleanMac = macAddress.trim().uppercase()
            
            // Проверяем формат MAC-адреса
            val macRegex = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()
            if (!macRegex.matches(cleanMac)) {
                _uiState.value = SettingsUiState.Error("Неверный формат MAC-адреса. Используйте формат: AA:BB:CC:DD:EE:FF")
                return
            }
            
            printerSettings.printerMacAddress = cleanMac
            printerSettings.printerName = printerName
            printerMac.value = printerSettings.printerMacAddress
            
            _uiState.value = SettingsUiState.Success(
                selectedDevice = cleanMac,
                isConnected = connectionState.value == ConnectionState.CONNECTED
            )
            
            timber.log.Timber.d("Printer manually selected: $cleanMac")
        } catch (e: Exception) {
            _uiState.value = SettingsUiState.Error("Ошибка при выборе принтера: ${e.message}")
        }
    }

    fun printTestLabel() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val testLabel = LabelData(
                type = LabelType.STANDARD,
                routeCardNumber = "ТК-2025/06/01",
                partNumber = "TEST-001",
                partName = "Тестовая деталь",
                orderNumber = "2024/TEST",
                quantity = 10,
                cellCode = "A1-B2-C3",
                date = "01.06.2025",
                qrData = "test=2024/TEST=TEST-001=Тестовая деталь",
                labelType = "Тест",
                description = "Тестовая деталь",
                location = "A1",
                customFields = mapOf("тест" to "значение")
            )
            printerService.printLabel(testLabel).fold(
                onSuccess = {
                    _uiState.value = SettingsUiState.Success(
                        isConnected = true,
                        selectedDevice = printerMac.value
                    )
                },
                onFailure = { error ->
                    _uiState.value = SettingsUiState.Error(
                        "Ошибка печати: ${error.message}"
                    )
                }
            )
        }
    }
}

