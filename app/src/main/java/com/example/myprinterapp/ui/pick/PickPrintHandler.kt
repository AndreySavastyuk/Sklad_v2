package com.example.myprinterapp.ui.pick

import com.example.myprinterapp.data.PickDetail
import com.example.myprinterapp.data.PickTask
import com.example.myprinterapp.printer.LabelData
import com.example.myprinterapp.printer.LabelType
import com.example.myprinterapp.printer.PrinterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Обработчик печати этикеток для комплектации
 */
class PickPrintHandler @Inject constructor(
    private val printerService: PrinterService,
    private val scope: CoroutineScope
) {

    /**
     * Печать этикетки для скомплектованной детали
     */
    fun printPickingLabel(
        detail: PickDetail,
        task: PickTask,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            try {
                // Формируем данные для этикетки
                val labelData = LabelData(
                    partNumber = detail.partNumber,
                    description = detail.partName,
                    orderNumber = task.id, // Номер задания
                    location = detail.location,
                    quantity = detail.picked,
                    qrData = generatePickingQrData(detail, task),
                    labelType = "Комплектация"
                )

                // Печатаем с форматом для комплектации
                printerService.printLabel(labelData, LabelType.PICKING_57x40)
                    .onSuccess {
                        onSuccess()
                    }
                    .onFailure { error ->
                        onError(error.message ?: "Неизвестная ошибка печати")
                    }
            } catch (e: Exception) {
                onError(e.message ?: "Ошибка при подготовке печати")
            }
        }
    }

    /**
     * Генерация QR-кода для комплектации
     */
    private fun generatePickingQrData(detail: PickDetail, task: PickTask): String {
        // Формат: TASK_ID=PART_NUMBER=QUANTITY=LOCATION
        return "${task.id}=${detail.partNumber}=${detail.picked}=${detail.location}"
    }
}

/**
 * Расширение для PickViewModel для добавления функции печати
 */
fun PickViewModel.printLabel(detail: PickDetail) {
    currentTask.value?.let { task ->
        // Проверяем, что деталь собрана
        if (detail.picked > 0) {
            // TODO: Вызвать PickPrintHandler для печати
            println("Печать этикетки для ${detail.partNumber}, количество: ${detail.picked}")
        }
    }
}