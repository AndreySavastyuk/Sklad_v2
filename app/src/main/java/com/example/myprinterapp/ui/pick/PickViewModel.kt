package com.example.myprinterapp.ui.pick

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.myprinterapp.data.PickDetail
import com.example.myprinterapp.data.PickTask
import com.example.myprinterapp.data.Priority
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

            // Автоматически обновляем статус задания на основе прогресса
            checkAndUpdateTaskStatusByProgress(task.id)
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

    /**
     * Отметить задание как завершенное
     */
    fun markTaskAsCompleted(taskId: String) {
        println("Debug: markTaskAsCompleted called for task: $taskId")
        updateTaskStatus(taskId, TaskStatus.COMPLETED)
    }

    /**
     * Приостановить задание
     */
    fun pauseTask(taskId: String) {
        println("Debug: pauseTask called for task: $taskId")
        updateTaskStatus(taskId, TaskStatus.PAUSED)
    }

    /**
     * Отменить задание
     */
    fun cancelTask(taskId: String) {
        println("Debug: cancelTask called for task: $taskId")
        updateTaskStatus(taskId, TaskStatus.CANCELLED)
    }

    /**
     * Возобновить задание (из паузы)
     */
    fun resumeTask(taskId: String) {
        println("Debug: resumeTask called for task: $taskId")
        updateTaskStatus(taskId, TaskStatus.IN_PROGRESS)
    }

    /**
     * Восстановить отмененное задание
     */
    fun restoreTask(taskId: String) {
        println("Debug: restoreTask called for task: $taskId")
        updateTaskStatus(taskId, TaskStatus.NEW)
    }

    /**
     * Общий метод для обновления статуса задания
     */
    private fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        val updatedTasks = _tasks.value.map { task ->
            if (task.id == taskId) {
                val updatedTask = task.copy(status = newStatus)
                // Если это текущее задание, обновляем и его
                if (_currentTask.value?.id == taskId) {
                    _currentTask.value = updatedTask
                }
                updatedTask
            } else {
                task
            }
        }
        _tasks.value = updatedTasks
        println("Debug: Updated task $taskId status to $newStatus")
    }

    /**
     * Автоматическое обновление статуса задания на основе прогресса
     */
    private fun checkAndUpdateTaskStatusByProgress(taskId: String) {
        val task = _tasks.value.find { it.id == taskId } ?: return

        val allDetailsCompleted = task.details.all { it.picked >= it.quantityToPick }
        val hasStartedPicking = task.details.any { it.picked > 0 }

        val newStatus = when {
            allDetailsCompleted && task.status != TaskStatus.COMPLETED -> TaskStatus.COMPLETED
            hasStartedPicking && task.status == TaskStatus.NEW -> TaskStatus.IN_PROGRESS
            else -> task.status
        }

        if (newStatus != task.status) {
            updateTaskStatus(taskId, newStatus)
            println("Debug: Auto-updated task $taskId status from ${task.status} to $newStatus")
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
                date = "2025-05-30",
                description = "Изготовление деталей коробки передач для заказа 2023/023",
                status = TaskStatus.NEW,
                details = listOf(
                    PickDetail(1, "НЗ.КШ.040.25.001", "Вал приводной", 10, "A45", 0),
                    PickDetail(2, "НЗ.КШ.040.25.002", "Втулка направляющая", 5, "B12", 0),
                    PickDetail(3, "НЗ.КШ.040.25.003", "Шестерня коническая", 12, "C33", 0)
                ),
                priority = Priority.URGENT,
                customer = "ПАО КАМАЗ",
                deadline = "2025-06-01"
            ),
            PickTask(
                id = "ZADANIE-002",
                date = "2025-05-29",
                description = "Комплектующие для двигателя заказ 2024/156",
                status = TaskStatus.IN_PROGRESS,
                details = listOf(
                    PickDetail(4, "НЗ.ДВ.120.18.001", "Поршень цилиндра", 20, "D15", 5),
                    PickDetail(5, "НЗ.ДВ.120.18.002", "Кольцо поршневое", 8, "A11", 2)
                ),
                priority = Priority.HIGH,
                customer = "ОАО Автодизель",
                deadline = "2025-05-31"
            ),
            PickTask(
                id = "ZADANIE-003",
                date = "2025-05-28",
                description = "Завершенная партия для заказа 2024/078",
                status = TaskStatus.COMPLETED,
                details = listOf(
                    PickDetail(6, "НЗ.РМ.090.12.001", "Рычаг тормозной", 1, "F99", 1)
                ),
                priority = Priority.NORMAL,
                customer = "ГАЗ GROUP"
            ),
            PickTask(
                id = "ZADANIE-004",
                date = "2025-05-31",
                description = "Крупная партия заказа 2025/001 - детали подвески",
                status = TaskStatus.NEW,
                details = listOf(
                    PickDetail(7, "НЗ.ПД.055.30.001", "Амортизатор передний", 15, "E12", 0),
                    PickDetail(8, "НЗ.ПД.055.30.002", "Пружина подвески", 25, "E05", 3),
                    PickDetail(9, "НЗ.ПД.055.30.003", "Сайлентблок рычага", 8, "F08", 0),
                    PickDetail(10, "НЗ.ПД.055.30.004", "Шарнир шаровой", 12, "G15", 7),
                    PickDetail(11, "НЗ.ПД.055.30.005", "Стабилизатор поперечной устойчивости", 18, "H20", 0)
                ),
                priority = Priority.NORMAL,
                customer = "УАЗ",
                deadline = "2025-06-05"
            ),
            PickTask(
                id = "ZADANIE-005",
                date = "2025-05-27",
                description = "Приостановленное производство заказа 2024/299",
                status = TaskStatus.PAUSED,
                details = listOf(
                    PickDetail(12, "НЗ.КП.075.22.001", "Корпус коробки передач", 20, "B15", 8),
                    PickDetail(13, "НЗ.КП.075.22.002", "Синхронизатор переключения", 10, "C25", 0)
                ),
                priority = Priority.LOW,
                customer = "ЛАДА",
                deadline = "2025-06-10"
            )
        ).also {
            println("Debug: Created ${it.size} example tasks")
            it.forEach { task ->
                println("Debug: Task ${task.id} has ${task.details.size} details")
            }
        }
    }
}