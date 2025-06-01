package com.example.myprinterapp.ui.settings

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.data.PrinterSettings
import com.example.myprinterapp.printer.ConnectionState
import com.example.myprinterapp.printer.LabelData
import com.example.myprinterapp.printer.PrinterService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
            printerSettings.printerName = device.name ?: "Неизвестный принтер"
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
            printerMac.value.let { mac ->
                if (mac.isNotEmpty()) {
                    printerService.connect(mac).fold(
                        onSuccess = {
                            _uiState.value = SettingsUiState.Success(
                                isConnected = true,
                                selectedDevice = mac
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = SettingsUiState.Error(
                                "Ошибка подключения: ${error.message}"
                            )
                        }
                    )
                } else {
                    _uiState.value = SettingsUiState.Error("Принтер не выбран")
                }
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

    fun printTestLabel() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val testLabel = LabelData(
                partNumber = "TEST-001",
                description = "Тестовая деталь",
                orderNumber = "2024/TEST",
                location = "A1",
                quantity = 10,
                qrData = "test=2024/TEST=TEST-001=Тестовая деталь",
                labelType = "Тест"
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
