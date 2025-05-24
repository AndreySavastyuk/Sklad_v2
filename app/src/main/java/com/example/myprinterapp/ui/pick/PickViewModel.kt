package com.example.myprinterapp.ui.pick

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.myprinterapp.data.PickDetail
import com.example.myprinterapp.data.PickTask
import com.example.myprinterapp.data.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PickViewModel @Inject constructor() : ViewModel() {

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

    // ----- Состояние для последнего отсканированного кода (общее) -----
    private val _scannedCode = MutableStateFlow<String?>(null)
    val scannedCode: StateFlow<String?> = _scannedCode.asStateFlow()

    private val _lastScannedCode = MutableStateFlow<String?>(null)
    val lastScannedCode: StateFlow<String?> = _lastScannedCode

    init {
        loadTasks()
        println("Debug: PickViewModel initialized")
    }

    private fun loadTasks() {
        val loadedTasks = loadPickTasksExample()
        _tasks.value = loadedTasks
        println("Debug: Loaded ${loadedTasks.size} tasks into ViewModel")
        loadedTasks.forEachIndexed { index, task ->
            println("Debug: Task $index: id=${task.id}, details_count=${task.details.size}")
            task.details.forEachIndexed { detailIndex, detail ->
                println("Debug:   Detail $detailIndex: ${detail.partNumber} - ${detail.partName}")
            }
        }
    }

    fun openTask(taskId: String) {
        println("Debug: openTask called with taskId: $taskId")
        val foundTask = _tasks.value.firstOrNull { it.id == taskId }
        _currentTask.value = foundTask

        if (foundTask == null) {
            println("Warning: Task with ID $taskId not found in ${_tasks.value.map { it.id }}")
        } else {
            println("Debug: Opened task ID ${foundTask.id} with ${foundTask.details.size} details")
            foundTask.details.forEach { detail ->
                println("Debug: Detail: ${detail.id} - ${detail.partNumber} - ${detail.partName}")
            }
        }
    }

    fun requestShowQtyDialog(detail: PickDetail) {
        println("Debug: requestShowQtyDialog for detail: ${detail.partNumber}")
        _showQtyDialogFor.value = detail
    }

    fun dismissQtyDialog() {
        println("Debug: dismissQtyDialog")
        _showQtyDialogFor.value = null
    }

    fun submitPickedQuantity(detailId: Int, pickedQuantity: Int) {
        println("Debug: submitPickedQuantity - detailId: $detailId, quantity: $pickedQuantity")

        _currentTask.value?.let { task ->
            val updatedDetails = task.details.map { detail ->
                if (detail.id == detailId) {
                    val newPickedQty = pickedQuantity.coerceIn(0, detail.quantityToPick)
                    println("Debug: Updating detail ${detail.partNumber}: picked ${detail.picked} -> $newPickedQty")
                    detail.copy(picked = newPickedQty)
                } else {
                    detail
                }
            }
            val updatedTask = task.copy(details = updatedDetails)
            _currentTask.value = updatedTask

            // Обновляем задание в общем списке
            _tasks.value = _tasks.value.map {
                if (it.id == updatedTask.id) updatedTask else it
            }

            println("Debug: Updated task ${task.id}, new picked quantities:")
            updatedDetails.forEach { detail ->
                println("Debug:   ${detail.partNumber}: ${detail.picked}/${detail.quantityToPick}")
            }
        }
        dismissQtyDialog()
    }

    fun prepareItemForScanning(detail: PickDetail) {
        _itemAwaitingScan.value = detail
        _showQtyDialogFor.value = null
        println("Debug: Prepared item for scanning - PartNo: ${detail.partNumber}, ID: ${detail.id}")
    }

    fun handleScannedBarcodeForItem(scannedBarcode: String) {
        println("Debug: handleScannedBarcodeForItem - barcode: $scannedBarcode")
        val targetItem = _itemAwaitingScan.value
        if (targetItem != null && targetItem.partNumber == scannedBarcode) {
            println("Debug: Scanned barcode $scannedBarcode MATCHES expected item ${targetItem.partNumber}")
            requestShowQtyDialog(targetItem)
        } else if (targetItem != null) {
            println("Error: Scanned barcode $scannedBarcode MISMATCH. Expected ${targetItem.partNumber}")
        } else {
            println("Debug: No item awaiting scan, trying general processing")
            onBarcodeScannedGeneral(scannedBarcode)
        }
        _itemAwaitingScan.value = null
    }

    fun onBarcodeScannedGeneral(code: String) {
        println("Debug: General barcode scanned: $code")
        _lastScannedCode.value = code

        val partNumberFromCode = extractPartNumberFromGenericScan(code)

        if (partNumberFromCode != null) {
            _currentTask.value?.details?.firstOrNull { it.partNumber == partNumberFromCode }?.let { foundDetail ->
                println("Debug: Found detail by general scan: ${foundDetail.partNumber}")
                requestShowQtyDialog(foundDetail)
            } ?: run {
                println("Warning: Detail with part number $partNumberFromCode not found in current task")
            }
        } else {
            println("Warning: Could not extract part number from general scan: $code")
        }
    }

    private fun extractPartNumberFromGenericScan(scannedCode: String): String? {
        return if (scannedCode.isNotBlank()) scannedCode else null
    }

    private fun loadPickTasksExample(): List<PickTask> {
        println("Debug: Creating example tasks...")

        return listOf(
            PickTask(
                id = "ZADANIE-001",
                date = "2024-07-30",
                description = "Сборка для клиента 'ООО Ромашка'",
                status = TaskStatus.NEW,
                details = listOf(
                    PickDetail(1, "PN-APPLE-01", "Яблоки красные", 10, "Склад A, Секция 1, Ячейка 01", 0),
                    PickDetail(2, "PN-ORANGE-02", "Апельсины сладкие", 5, "Склад A, Секция 1, Ячейка 02", 0),
                    PickDetail(3, "PN-BANANA-03", "Бананы спелые", 12, "Склад B, Секция 2, Ячейка 05", 0)
                )
            ),
            PickTask(
                id = "ZADANIE-002",
                date = "2024-07-29",
                description = "Срочная сборка для 'ИП Васильков'",
                status = TaskStatus.IN_PROGRESS,
                details = listOf(
                    PickDetail(4, "PN-GRAPE-01", "Виноград Кишмиш", 20, "Склад C, Холодильник 1", 5),
                    PickDetail(5, "PN-PEAR-04", "Груши Конференция", 8, "Склад A, Секция 3, Ячейка 11", 2)
                )
            ),
            PickTask(
                id = "ZADANIE-003",
                date = "2024-07-28",
                description = "Мелкая сборка",
                status = TaskStatus.COMPLETED,
                details = listOf(
                    PickDetail(6, "PN-WATERMELON-01", "Арбуз Астраханский", 1, "Склад D, Напольное", 1)
                )
            ),
            PickTask(
                id = "ZADANIE-004",
                date = "2024-07-31",
                description = "Тестовое задание с большим количеством позиций",
                status = TaskStatus.NEW,
                details = listOf(
                    PickDetail(7, "PN-CARROT-01", "Морковь молодая", 15, "Склад A, Ячейка 12", 0),
                    PickDetail(8, "PN-POTATO-01", "Картофель белый", 25, "Склад B, Ячейка 05", 3),
                    PickDetail(9, "PN-ONION-01", "Лук репчатый", 8, "Склад A, Ячейка 08", 0),
                    PickDetail(10, "PN-TOMATO-01", "Помидоры красные", 12, "Склад C, Ячейка 15", 7),
                    PickDetail(11, "PN-CUCUMBER-01", "Огурцы свежие", 18, "Склад A, Ячейка 20", 0)
                )
            )
        ).also {
            println("Debug: Created ${it.size} example tasks")
            it.forEach { task ->
                println("Debug: Task ${task.id} has ${task.details.size} details")
            }
        }
    }
}