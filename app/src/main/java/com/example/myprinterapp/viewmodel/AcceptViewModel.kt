package com.example.myprinterapp.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel экрана «Приёмка».
 *
 * Держит количество и код ячейки, реагирует на изменения полей и
 * триггерит печать ярлыка.  Сейчас печать – просто заглушка (TODO).
 */
class AcceptViewModel : ViewModel() {

    /* ---------------- state ---------------- */
    private val _scannedValue = MutableStateFlow<String?>(null)
    val scannedValue: StateFlow<String?> = _scannedValue

    private val _quantity  = MutableStateFlow("")
    val quantity:  StateFlow<String> = _quantity.asStateFlow()

    private val _cellCode  = MutableStateFlow("")
    val cellCode: StateFlow<String> = _cellCode.asStateFlow()

    fun onBarcodeDetected(code: String) {
        _scannedValue.value = code
    }

    /* ---------------- events ---------------- */

    /** Количество меняется только на цифры (проверку можно вынести выше) */
    fun onQuantityChange(new: String) {
        _quantity.value = new
    }

    /** Код ячейки – максимум 4 символа, латиница/цифра */
    fun onCellCodeChange(new: String) {
        _cellCode.value = new
    }

    /**
     * Печать ярлыка. Сейчас – заглушка.
     * TODO: внедрить PrinterService, передавать текущие значения,
     *       показывать успех/ошибку.
     */
    fun onPrintLabel() {
        val qty   = _quantity.value
        val cell  = _cellCode.value
        // TODO: проверить, что qty и cell валидные,
        //       вызвать PrinterService.printLabel(...)
        println("DEBUG: print label qty=$qty cell=$cell")
    }

    /** Если нужно очищать поля после успешной печати */
    fun resetInputFields() {
        _quantity.value = ""
        _cellCode.value = ""
    }
}
