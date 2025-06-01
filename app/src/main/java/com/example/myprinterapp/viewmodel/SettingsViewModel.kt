package com.example.myprinterapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.data.models.*
import com.example.myprinterapp.data.repo.SettingsRepository
import com.example.myprinterapp.printer.PrinterManager
import com.example.myprinterapp.scanner.ScannerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    object Success : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val printerManager: PrinterManager,
    private val scannerManager: ScannerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = SettingsUiState.Loading
                
                val settings = settingsRepository.getAppSettings()
                _appSettings.value = settings
                _uiState.value = SettingsUiState.Success
                
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Ошибка загрузки настроек", e)
                _uiState.value = SettingsUiState.Error("Ошибка загрузки настроек: ${e.message}")
            }
        }
    }

    fun retry() {
        loadSettings()
    }

    fun updatePrinterSettings(settings: PrinterSettings) {
        viewModelScope.launch {
            try {
                settingsRepository.updatePrinterSettings(settings)
                _appSettings.value = _appSettings.value.copy(printerSettings = settings)
                android.util.Log.d("SettingsViewModel", "Настройки принтера обновлены")
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Ошибка обновления настроек принтера", e)
            }
        }
    }

    fun updateScannerSettings(settings: ScannerSettings) {
        viewModelScope.launch {
            try {
                settingsRepository.updateScannerSettings(settings)
                _appSettings.value = _appSettings.value.copy(scannerSettings = settings)
                android.util.Log.d("SettingsViewModel", "Настройки сканера обновлены")
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Ошибка обновления настроек сканера", e)
            }
        }
    }

    fun updateUiSettings(settings: UiSettings) {
        viewModelScope.launch {
            try {
                settingsRepository.updateAppSettings(_appSettings.value.copy(uiSettings = settings))
                _appSettings.value = _appSettings.value.copy(uiSettings = settings)
                android.util.Log.d("SettingsViewModel", "Настройки UI обновлены")
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Ошибка обновления настроек UI", e)
            }
        }
    }

    fun updateNetworkSettings(settings: NetworkSettings) {
        viewModelScope.launch {
            try {
                settingsRepository.updateAppSettings(_appSettings.value.copy(networkSettings = settings))
                _appSettings.value = _appSettings.value.copy(networkSettings = settings)
                android.util.Log.d("SettingsViewModel", "Сетевые настройки обновлены")
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Ошибка обновления сетевых настроек", e)
            }
        }
    }

    fun testPrinterConnection() {
        viewModelScope.launch {
            try {
                android.util.Log.d("SettingsViewModel", "Тестирование подключения принтера...")
                
                val defaultDevice = printerManager.getDefaultDevice()
                if (defaultDevice != null) {
                    val isConnected = printerManager.isConnected(defaultDevice.id)
                    if (isConnected) {
                        android.util.Log.i("SettingsViewModel", "Принтер подключен: ${defaultDevice.name}")
                        // TODO: Показать Toast или снackbar
                    } else {
                        android.util.Log.w("SettingsViewModel", "Принтер не подключен")
                        // TODO: Показать сообщение об ошибке
                    }
                } else {
                    android.util.Log.w("SettingsViewModel", "Принтер по умолчанию не найден")
                    // TODO: Показать сообщение
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Ошибка тестирования принтера", e)
            }
        }
    }

    fun testScanner() {
        viewModelScope.launch {
            try {
                android.util.Log.d("SettingsViewModel", "Тестирование сканера...")
                
                val result = scannerManager.startScanning()
                when (result) {
                    is Result.Success -> {
                        android.util.Log.i("SettingsViewModel", "Сканер готов к работе")
                        // Останавливаем сканирование через секунду
                        kotlinx.coroutines.delay(1000)
                        scannerManager.stopScanning()
                        // TODO: Показать сообщение об успехе
                    }
                    is Result.Error -> {
                        android.util.Log.e("SettingsViewModel", "Ошибка сканера: ${result.message}")
                        // TODO: Показать сообщение об ошибке
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Ошибка тестирования сканера", e)
            }
        }
    }
} 