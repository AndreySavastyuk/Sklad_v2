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

sealed class AppSettingsUiState {
    object Loading : AppSettingsUiState()
    object Success : AppSettingsUiState()
    data class Error(val message: String) : AppSettingsUiState()
}

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val printerManager: PrinterManager,
    private val scannerManager: ScannerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppSettingsUiState>(AppSettingsUiState.Loading)
    val uiState: StateFlow<AppSettingsUiState> = _uiState.asStateFlow()

    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = AppSettingsUiState.Loading

                val settings = settingsRepository.getAppSettings()
                _appSettings.value = settings
                _uiState.value = AppSettingsUiState.Success

            } catch (e: Exception) {
                android.util.Log.e("AppSettingsViewModel", "Ошибка загрузки настроек", e)
                _uiState.value = AppSettingsUiState.Error("Ошибка загрузки настроек: ${e.message}")
            }
        }
    }
}
