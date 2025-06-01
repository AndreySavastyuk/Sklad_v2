package com.example.myprinterapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.data.db.PrintLogEntry
import com.example.myprinterapp.domain.usecase.GetPrintHistoryUseCase
import com.example.myprinterapp.domain.usecase.ReprintLabelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PrintLogUiState {
    object Loading : PrintLogUiState()
    object Success : PrintLogUiState()
    data class Error(val message: String) : PrintLogUiState()
}

@HiltViewModel
class PrintLogViewModel @Inject constructor(
    private val getPrintHistoryUseCase: GetPrintHistoryUseCase,
    private val reprintLabelUseCase: ReprintLabelUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PrintLogUiState>(PrintLogUiState.Loading)
    val uiState: StateFlow<PrintLogUiState> = _uiState.asStateFlow()

    private val _printHistory = MutableStateFlow<List<PrintLogEntry>>(emptyList())
    val printHistory: StateFlow<List<PrintLogEntry>> = _printHistory.asStateFlow()

    init {
        loadPrintHistory()
    }

    private fun loadPrintHistory() {
        viewModelScope.launch {
            try {
                _uiState.value = PrintLogUiState.Loading
                
                getPrintHistoryUseCase().collect { history ->
                    _printHistory.value = history
                    _uiState.value = PrintLogUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = PrintLogUiState.Error("Ошибка загрузки данных: ${e.message}")
            }
        }
    }

    fun retry() {
        loadPrintHistory()
    }

    fun reprintLabel(entry: PrintLogEntry) {
        viewModelScope.launch {
            try {
                // TODO: Показать диалог выбора количества и ячейки
                // Пока используем оригинальные значения
                val result = reprintLabelUseCase(
                    ReprintLabelUseCase.Params(
                        originalEntry = entry,
                        newQuantity = entry.quantity,
                        newCellCode = entry.location
                    )
                )
                
                when (result) {
                    is com.example.myprinterapp.data.models.Result.Success -> {
                        // TODO: Показать сообщение об успехе
                        android.util.Log.d("PrintLogViewModel", "Этикетка перепечатана успешно")
                    }
                    is com.example.myprinterapp.data.models.Result.Error -> {
                        // TODO: Показать сообщение об ошибке
                        android.util.Log.e("PrintLogViewModel", "Ошибка перепечати: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PrintLogViewModel", "Ошибка перепечати", e)
            }
        }
    }

    /**
     * Перепечать этикетку с новыми параметрами
     */
    fun reprintLabelWithParams(entry: PrintLogEntry, newQuantity: Int, newCellCode: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("PrintLogViewModel", "Перепечать с новыми параметрами: qty=$newQuantity, cell=$newCellCode")
                
                val result = reprintLabelUseCase(
                    ReprintLabelUseCase.Params(
                        originalEntry = entry,
                        newQuantity = newQuantity,
                        newCellCode = newCellCode
                    )
                )
                
                when (result) {
                    is com.example.myprinterapp.data.models.Result.Success -> {
                        android.util.Log.i("PrintLogViewModel", "Этикетка успешно перепечатана с новыми параметрами")
                        // TODO: Показать Toast с успехом
                    }
                    is com.example.myprinterapp.data.models.Result.Error -> {
                        android.util.Log.e("PrintLogViewModel", "Ошибка перепечати: ${result.message}")
                        // TODO: Показать Toast с ошибкой  
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PrintLogViewModel", "Ошибка перепечати с новыми параметрами", e)
                // TODO: Показать Toast с ошибкой
            }
        }
    }
} 