package com.example.myprinterapp.ui.pick

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.myprinterapp.data.PickDetail
import com.example.myprinterapp.data.PickTask
import com.example.myprinterapp.data.TaskStatus
import com.example.myprinterapp.data.Priority
import com.example.myprinterapp.printer.PrinterService
import com.example.myprinterapp.printer.LabelData
import com.example.myprinterapp.printer.LabelType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PickViewModel @Inject constructor(
    private val printerService: PrinterService
) : ViewModel() {

    // ----- Состояние для списка всех заданий -----
    private val _tasks = MutableStateFlow<List<PickTask>>(emptyList())
    val tasks: StateFlow<List<PickTask>> = _tasks.asStateFlow()

    // ----- Состояние для текущего открытого задания -----
    private val _currentTask = MutableStateFlow<PickTask?>(null)
    val currentTask: StateFlow<PickTask?> = _currentTask.asStateFlow()

    // ----- Состояние для диалога ввода количества -----
    private val _showQtyDialogFor = MutableStateFlow<PickDetail?>(null)
    val showQtyDialogFor: StateFlow<PickDetail?> = _showQtyDialogFor.asStateFlow()

    // ----- Состояние для элемента, ожидающего сканирования -----
    private val _itemAwaitingScan = MutableStateFlow<PickDetail?>(null)

    // ----- Состояние для последнего отсканированного кода -----
    private val _lastScannedCode = MutableStateFlow<String?>(null)
    val lastScannedCode: StateFlow<String?> = _lastScannedCode

    // ----- Состояние печати -----
    private val _printingState = MutableStateFlow<PrintingState>(PrintingState.Idle)
    val printingState: StateFlow<PrintingState> = _printingState.asStateFlow()

    init {
        loadTasks()
        println("Debug: PickViewModel initialized and tasks loading initiated.")
    }

    private fun loadTasks() {
        _tasks.value = loadPickTasksExample().also {
            println("Debug: Loaded ${it.size} tasks into ViewModel")
        }
    }

    fun openTask(taskId: String) {
        _currentTask.value = _tasks.value.firstOrNull { it.id == taskId }
        if (_currentTask.value == null) {
            println("Warning: Task with ID $taskId not found.")
        } else {
            println("Debug: Opened task ID ${taskId}")
        }
    }

    fun requestShowQtyDialog(detail: PickDetail) {
        _showQtyDialogFor.value = detail
    }

    fun dismissQtyDialog() {
        _showQtyDialogFor.value = null
    }

    fun submitPickedQuantity(detailId: Int, pickedQuantity: Int) {
        _currentTask.value?.let { task ->
            val updatedDetails = task.details.map { detail ->
                if (detail.id == detailId) {
                    val newPickedQty = pickedQuantity.coerceIn(0, detail.quantityToPick)
                    detail.copy(picked = newPickedQty)
                } else {
                    detail
                }
            }
            val updatedTask = task.copy(details = updatedDetails)
            _currentTask.value = updatedTask

            _tasks.value = _tasks.value.map {
                if (it.id == updatedTask.id) updatedTask else it
            }

            // Обновляем статус задания
            checkAndUpdateTaskStatus(updatedTask)

            println("Debug: Submitted quantity $pickedQuantity for detail ID $detailId. Task ID: ${task.id}")
        }
        dismissQtyDialog()
    }

    fun prepareItemForScanning(detail: PickDetail) {
        _itemAwaitingScan.value = detail
        _showQtyDialogFor.value = null
        println("Debug: Prepared item for scanning - PartNo: ${detail.partNumber}, ID: ${detail.id}")
    }

    fun handleScannedBarcodeForItem(scannedBarcode: String) {
        val targetItem = _itemAwaitingScan.value
        if (targetItem != null && targetItem.partNumber == scannedBarcode) {
            println("Debug: Scanned barcode $scannedBarcode MATCHES expected item ${targetItem.partNumber}")
            requestShowQtyDialog(targetItem)
        } else if (targetItem != null) {
            println("Error: Scanned barcode $scannedBarcode MISMATCH. Expected ${targetItem.partNumber}")
        } else {
            println("Warning: Scanned barcode $scannedBarcode received, but no item was awaiting scan. Trying general processing.")
            onBarcodeScannedGeneral(scannedBarcode)
        }
        _itemAwaitingScan.value = null
    }

    fun onBarcodeScannedGeneral(code: String) {
        println("Debug: General barcode scanned: $code")

        val partNumberFromCode = extractPartNumberFromGenericScan(code)

        if (partNumberFromCode != null) {
            _currentTask.value?.details?.firstOrNull { it.partNumber == partNumberFromCode }?.let { foundDetail ->
                println("Debug: Found detail by general scan: ${foundDetail.partNumber}")
                requestShowQtyDialog(foundDetail)
            } ?: run {
                println("Warning: Detail with part number $partNumberFromCode not found in current task after general scan.")
            }
        } else {
            println("Warning: Could not extract part number from general scan: $code")
        }
    }

    /**
     * Печать этикетки для комплектации
     */
    fun printPickingLabel(detail: PickDetail) {
        _currentTask.value?.let { task ->
            if (detail.picked > 0) {
                viewModelScope.launch {
                    _printingState.value = PrintingState.Printing

                    val labelData = LabelData(
                        partNumber = detail.partNumber,
                        description = detail.partName,
                        orderNumber = "Задание №${task.id}",
                        location = detail.location,
                        quantity = detail.picked,
                        qrData = generatePickingQrData(detail, task),
                        labelType = "Комплектация"
                    )

                    printerService.printLabel(labelData, LabelType.PICKING_57x40)
                        .onSuccess {
                            _printingState.value = PrintingState.Success("Этикетка напечатана")
                            println("Debug: Label printed successfully for ${detail.partNumber}")
                        }
                        .onFailure { error ->
                            _printingState.value = PrintingState.Error(error.message ?: "Ошибка печати")
                            println("Error: Failed to print label - ${error.message}")
                        }
                }
            } else {
                _printingState.value = PrintingState.Error("Нет собранных товаров для печати")
            }
        }
    }

    /**
     * Очистка состояния печати
     */
    fun clearPrintingState() {
        _printingState.value = PrintingState.Idle
    }

    /**
     * Генерация QR-кода для комплектации
     */
    private fun generatePickingQrData(detail: PickDetail, task: PickTask): String {
        return "${task.id}=${detail.partNumber}=${detail.picked}=${detail.location}"
    }

    private fun extractPartNumberFromGenericScan(scannedCode: String): String? {
        if (scannedCode.isNotBlank()) {
            return scannedCode
        }
        return null
    }

    private fun checkAndUpdateTaskStatus(task: PickTask) {
        val allDetailsPicked = task.details.all { it.picked >= it.quantityToPick }
        if (allDetailsPicked && task.status != TaskStatus.COMPLETED) {
            val updatedTask = task.copy(status = TaskStatus.COMPLETED)
            _currentTask.value = updatedTask
            _tasks.value = _tasks.value.map {
                if (it.id == updatedTask.id) updatedTask else it
            }
            println("Debug: Task ${task.id} status updated to COMPLETED.")
        }
    }

    private fun loadPickTasksExample(): List<PickTask> {
        val random = kotlin.random.Random(42)

        fun randomCell(): String {
            val letter = ('A'..'Z').random(random)
            val number = (1..30).random(random).toString().padStart(2, '0')
            return "$letter$number"
        }

        fun randomDate2025(): String {
            val month = (1..12).random(random)
            val day = when(month) {
                2 -> (1..28).random(random)
                4, 6, 9, 11 -> (1..30).random(random)
                else -> (1..31).random(random)
            }
            return "2025-%02d-%02d".format(month, day)
        }

        return listOf(
            PickTask(
                id = "001",
                date = randomDate2025(),
                description = "Сборка для ООО Техмаш",
                status = TaskStatus.NEW,
                customer = "ООО Техмаш",
                details = listOf(
                    PickDetail(1, "НЗ.КШ.040.25.001-04", "Втулка", 5, randomCell()),
                    PickDetail(2, "НЗ.КШ.040.25.002-01", "Корпус", 2, randomCell())
                )
            ),
            PickTask(
                id = "002",
                date = randomDate2025(),
                description = "Заказ 2024/005",
                status = TaskStatus.NEW,
                customer = "ИП Петров А.С.",
                details = listOf(
                    PickDetail(3, "НЗ.КШ.065.25.021", "Вал", 8, randomCell()),
                    PickDetail(4, "НЗ.КШ.065.25.022", "Крышка", 8, randomCell()),
                    PickDetail(5, "НЗ.КШ.065.25.023-01", "Втулка направляющая", 16, randomCell())
                )
            ),
            // ... остальные задания из предыдущего кода ...
        )
    }
}

/**
 * Состояние печати
 */
sealed class PrintingState {
    object Idle : PrintingState()
    object Printing : PrintingState()
    data class Success(val message: String) : PrintingState()
    data class Error(val message: String) : PrintingState()
}