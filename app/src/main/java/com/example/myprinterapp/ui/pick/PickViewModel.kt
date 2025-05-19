package com.example.myprinterapp.ui.pick

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// Убедитесь, что пути к вашим классам данных верны
import com.example.myprinterapp.data.PickDetail
import com.example.myprinterapp.data.PickTask
import com.example.myprinterapp.data.TaskStatus // Предполагаем, что TaskStatus находится здесь

class PickViewModel : ViewModel() {

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
    // val itemAwaitingScan: StateFlow<PickDetail?> = _itemAwaitingScan.asStateFlow() // Раскомментируйте, если нужно наблюдать из UI

    // ----- Состояние для последнего отсканированного кода (общее) -----
    private val _scannedCode = MutableStateFlow<String?>(null)
    val scannedCode: StateFlow<String?> = _scannedCode.asStateFlow()

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
            // TODO: Обновление статуса задания (например, если все собрано, поменять task.status)
            // checkAndUpdateTaskStatus(updatedTask)
            println("Debug: Submitted quantity $pickedQuantity for detail ID $detailId. Task ID: ${task.id}")
        }
        dismissQtyDialog()
    }

    fun prepareItemForScanning(detail: PickDetail) {
        _itemAwaitingScan.value = detail
        _showQtyDialogFor.value = null // Закрываем любой открытый диалог, т.к. начинаем скан для конкретного
        println("Debug: Prepared item for scanning - PartNo: ${detail.partNumber}, ID: ${detail.id}")
    }

    private val _lastScannedCode = MutableStateFlow<String?>(null)
    val lastScannedCode: StateFlow<String?> = _lastScannedCode

    fun handleScannedBarcodeForItem(scannedBarcode: String) {
        val targetItem = _itemAwaitingScan.value
        if (targetItem != null && targetItem.partNumber == scannedBarcode) {
            println("Debug: Scanned barcode $scannedBarcode MATCHES expected item ${targetItem.partNumber}")
            requestShowQtyDialog(targetItem)
        } else if (targetItem != null) {
            println("Error: Scanned barcode $scannedBarcode MISMATCH. Expected ${targetItem.partNumber}")
            // TODO: Показать сообщение об ошибке пользователю
        } else {
            println("Warning: Scanned barcode $scannedBarcode received, but no item was awaiting scan. Trying general processing.")
            // Если ни один элемент не ожидал сканирования, попробуем обработать как общий скан
            onBarcodeScannedGeneral(scannedBarcode)
        }
        _itemAwaitingScan.value = null // Сбрасываем элемент, ожидающий сканирования
    }

    fun onBarcodeScannedGeneral(code: String) {
        // Не устанавливаем _scannedCode.value здесь, если не хотим, чтобы UI на это реагировал отдельно
        // _scannedCode.value = code;
        println("Debug: General barcode scanned: $code")

        val partNumberFromCode = extractPartNumberFromGenericScan(code)

        if (partNumberFromCode != null) {
            _currentTask.value?.details?.firstOrNull { it.partNumber == partNumberFromCode }?.let { foundDetail ->
                println("Debug: Found detail by general scan: ${foundDetail.partNumber}")
                requestShowQtyDialog(foundDetail)
            } ?: run {
                println("Warning: Detail with part number $partNumberFromCode not found in current task after general scan.")
                // TODO: Сообщить пользователю, что деталь не найдена
            }
        } else {
            println("Warning: Could not extract part number from general scan: $code")
            // TODO: Сообщить пользователю о неверном формате штрих-кода
        }
        // _scannedCode.value = null; // Очищаем после обработки, если это необходимо
    }

    /**
     * Вспомогательная функция для извлечения артикула из общего скана.
     * Адаптируйте эту функцию под ваш формат штрих-кодов.
     * Это очень упрощенный пример. Вам может потребоваться более сложная логика.
     */
    private fun extractPartNumberFromGenericScan(scannedCode: String): String? {
        // Предположим, что штрих-код *может быть* чистым артикулом.
        // Если у вас есть префиксы или структура данных в QR, здесь должна быть логика разбора.
        // Например, если QR содержит "TYPE=PART;DATA=PN-APPLE-01;QTY=10"
        // if (scannedCode.startsWith("TYPE=PART;DATA=")) {
        //     return scannedCode.substringAfter("DATA=").substringBefore(";")
        // }
        // Для простоты, пока считаем, что отсканированный код - это и есть артикул.
        if (scannedCode.isNotBlank()) {
            return scannedCode
        }
        return null
    }

    /**
     * Вспомогательная функция для проверки и обновления статуса задания.
     * Например, если все детали собраны, статус меняется на COMPLETED.
     */
    private fun checkAndUpdateTaskStatus(task: PickTask) {
        val allDetailsPicked = task.details.all { it.picked >= it.quantityToPick }
        if (allDetailsPicked && task.status != TaskStatus.COMPLETED) {
            val updatedTask = task.copy(status = TaskStatus.COMPLETED)
            _currentTask.value = updatedTask
            _tasks.value = _tasks.value.map {
                if (it.id == updatedTask.id) updatedTask else it
            }
            println("Debug: Task ${task.id} status updated to COMPLETED.")
            // TODO: Сохранить изменение статуса в репозиторий/БД
        }
    }


    private fun loadPickTasksExample(): List<PickTask> {
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
            )
        )
    }
}