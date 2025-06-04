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
import android.annotation.SuppressLint

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(
        val isConnected: Boolean = false,
        val selectedDevice: String? = null,
        val pairedDevices: List<BluetoothDeviceInfo> = emptyList()
    ) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
    data class PermissionRequired(val explanation: String) : SettingsUiState
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

    // Колбэк для запроса разрешений от Activity
    var onRequestBluetoothPermissions: ((onGranted: () -> Unit, onDenied: (List<String>) -> Unit) -> Unit)? = null

    init {
        _uiState.value = SettingsUiState.Success(
            isConnected = connectionState.value == ConnectionState.CONNECTED,
            selectedDevice = printerSettings.printerMacAddress
        )

        // Наблюдаем за состоянием подключения для автоматического обновления UI
        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    ConnectionState.CONNECTED -> {
                        _uiState.value = SettingsUiState.Success(
                            isConnected = true,
                            selectedDevice = printerMac.value
                        )
                    }
                    ConnectionState.CONNECTING -> {
                        _uiState.value = SettingsUiState.Loading
                    }
                    ConnectionState.DISCONNECTED -> {
                        // Обновляем только если текущее состояние не является ошибкой
                        if (_uiState.value !is SettingsUiState.Error && _uiState.value !is SettingsUiState.PermissionRequired) {
                            _uiState.value = SettingsUiState.Success(
                                isConnected = false,
                                selectedDevice = printerMac.value
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectPrinter(device: BluetoothDevice) {
        try {
            printerSettings.printerMacAddress = device.address
            // Проверяем разрешение для доступа к имени устройства Bluetooth
            @SuppressLint("MissingPermission")
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
        val requestPermissions = onRequestBluetoothPermissions

        if (requestPermissions == null) {
            // Fallback: пытаемся подключиться без проверки разрешений
            connectPrinterInternal()
            return
        }

        // Запрашиваем разрешения через Activity - исправленный вызов
        requestPermissions(
            { connectPrinterInternal() },
            { deniedPermissions: List<String> ->
                val explanation = generatePermissionExplanation(deniedPermissions)
                _uiState.value = SettingsUiState.PermissionRequired(explanation)
            }
        )
    }

    private fun connectPrinterInternal() {
        _uiState.value = SettingsUiState.Loading
        printerMac.value.let { mac ->
            if (mac.isNotEmpty()) {
                printerService.connect(mac)
                // Состояние будет обновлено через connectionState flow
            } else {
                _uiState.value = SettingsUiState.Error("Принтер не выбран")
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
            this.printerName.value = printerSettings.printerName
            printerMac.value = printerSettings.printerMacAddress

            _uiState.value = SettingsUiState.Success(
                selectedDevice = cleanMac,
                isConnected = connectionState.value == ConnectionState.CONNECTED
            )

        } catch (e: Exception) {
            _uiState.value = SettingsUiState.Error("Ошибка при выборе принтера: ${e.message}")
        }
    }

    fun printTestLabel() {
        val requestPermissions = onRequestBluetoothPermissions
        
        if (requestPermissions == null) {
            // Fallback: пытаемся печатать без проверки разрешений
            printTestLabelInternal()
            return
        }

        // Запрашиваем разрешения через Activity
        requestPermissions(
            { printTestLabelInternal() },
            { deniedPermissions: List<String> ->
                val explanation = generatePermissionExplanation(deniedPermissions)
                _uiState.value = SettingsUiState.PermissionRequired(explanation)
            }
        )
    }

    private fun printTestLabelInternal() {
        if (connectionState.value != ConnectionState.CONNECTED) {
            _uiState.value = SettingsUiState.Error("Принтер не подключен. Сначала подключитесь к принтеру.")
            return
        }

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
                location = "A1"
            )
            printerService.printLabel(testLabel, LabelType.STANDARD).fold(
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

    /**
     * Генерирует объяснение для отклоненных разрешений
     */
    private fun generatePermissionExplanation(deniedPermissions: List<String>): String {
        return buildString {
            appendLine("Для работы с принтером необходимы следующие разрешения:")
            
            deniedPermissions.forEach { permission ->
                when (permission) {
                    android.Manifest.permission.BLUETOOTH_SCAN -> {
                        appendLine("• BLUETOOTH_SCAN - для поиска Bluetooth устройств")
                    }
                    android.Manifest.permission.BLUETOOTH_CONNECT -> {
                        appendLine("• BLUETOOTH_CONNECT - для подключения к принтеру")
                    }
                    android.Manifest.permission.ACCESS_FINE_LOCATION -> {
                        appendLine("• ACCESS_FINE_LOCATION - для поиска Bluetooth устройств (Android 6-11)")
                    }
                }
            }
            
            appendLine()
            appendLine("Пожалуйста, предоставьте разрешения в настройках приложения.")
            appendLine("Разрешения используются только для работы с принтером.")
        }
    }

    /**
     * Очищает состояние запроса разрешений
     */
    fun clearPermissionState() {
        if (_uiState.value is SettingsUiState.PermissionRequired) {
            _uiState.value = SettingsUiState.Success(
                isConnected = connectionState.value == ConnectionState.CONNECTED,
                selectedDevice = printerMac.value
            )
        }
    }
}

