// файл: ScanViewModel.kt
package com.example.myprinterapp.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ScanViewModel : ViewModel() {
    private val _acceptScan = MutableStateFlow<String?>(null)
    val acceptScan: StateFlow<String?> = _acceptScan

    private val _pickScan = MutableStateFlow<String?>(null)
    val pickScan: StateFlow<String?> = _pickScan

    fun setAcceptScan(code: String) {
        _acceptScan.value = code
    }
    fun setPickScan(code: String) {
        _pickScan.value = code
    }

    /** Очистить, если нужно, после того как прочитали значение */
    fun clearAcceptScan() {
        _acceptScan.value = null
    }
    fun clearPickScan() {
        _pickScan.value = null
    }
}
