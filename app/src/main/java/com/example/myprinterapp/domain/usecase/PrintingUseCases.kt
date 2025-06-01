package com.example.myprinterapp.domain.usecase

import com.example.myprinterapp.data.models.*
import com.example.myprinterapp.data.repo.PrintLogRepository
import com.example.myprinterapp.data.db.PrintLogEntry
import com.example.myprinterapp.printer.PrinterManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use Cases для работы с печатью
 */

@Singleton
class PrintLabelUseCase @Inject constructor(
    private val printerManager: PrinterManager,
    private val printLogRepository: PrintLogRepository,
    dispatcher: CoroutineDispatcher
) : BaseUseCase<PrintLabelUseCase.Params, PrintJob>(dispatcher) {

    data class Params(
        val labelData: LabelData,
        val deviceId: String? = null
    )

    override suspend fun execute(parameters: Params): PrintJob {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        // Создаем задание печати
        val printJob = PrintJob(
            id = generateId(),
            labelData = parameters.labelData,
            deviceId = parameters.deviceId ?: printerManager.getDefaultDevice()?.id ?: "",
            status = PrintJobStatus.PENDING,
            createdAt = now
        )

        try {
            // Отправляем на печать через PrinterManager
            val printResult = printerManager.printLabel(printJob)
            
            val finalJob = when (printResult) {
                is Result.Success -> {
                    printJob.copy(
                        status = PrintJobStatus.COMPLETED,
                        completedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                }
                is Result.Error -> {
                    printJob.copy(
                        status = PrintJobStatus.FAILED,
                        errorMessage = printResult.message,
                        completedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                }
            }

            // Сохраняем в журнал
            printLogRepository.addPrintLog(
                PrintLogEntry(
                    timestamp = java.time.OffsetDateTime.now(),
                    operationType = "PRINT",
                    partNumber = parameters.labelData.partNumber,
                    partName = parameters.labelData.partName,
                    orderNumber = parameters.labelData.orderNumber,
                    quantity = parameters.labelData.quantity,
                    location = parameters.labelData.cellCode,
                    qrData = parameters.labelData.qrData,
                    printerStatus = if (finalJob.status == PrintJobStatus.COMPLETED) "SUCCESS" else "FAILED",
                    errorMessage = finalJob.errorMessage
                )
            )

            // Возвращаем результат или выбрасываем ошибку
            if (finalJob.status == PrintJobStatus.FAILED) {
                throw Exception(finalJob.errorMessage ?: "Ошибка печати")
            }
            
            return finalJob

        } catch (e: Exception) {
            // Обновляем статус и сохраняем ошибку
            val failedJob = printJob.copy(
                status = PrintJobStatus.FAILED,
                errorMessage = e.message,
                completedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            )

            printLogRepository.addPrintLog(
                PrintLogEntry(
                    timestamp = java.time.OffsetDateTime.now(),
                    operationType = "PRINT",
                    partNumber = parameters.labelData.partNumber,
                    partName = parameters.labelData.partName,
                    orderNumber = parameters.labelData.orderNumber,
                    quantity = parameters.labelData.quantity,
                    location = parameters.labelData.cellCode,
                    qrData = parameters.labelData.qrData,
                    printerStatus = "FAILED",
                    errorMessage = e.message
                )
            )

            throw e
        }
    }

    private fun generateId(): String {
        return "print_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

@Singleton
class GetPrintHistoryUseCase @Inject constructor(
    private val repository: PrintLogRepository,
    dispatcher: CoroutineDispatcher
) : NoParamsFlowUseCase<List<PrintLogEntry>>(dispatcher) {

    override fun execute(): Flow<List<PrintLogEntry>> {
        return repository.logFlow()
    }
}

@Singleton
class ConnectPrinterUseCase @Inject constructor(
    private val printerManager: PrinterManager,
    dispatcher: CoroutineDispatcher
) : BaseUseCase<ConnectPrinterUseCase.Params, DeviceInfo>(dispatcher) {

    data class Params(
        val deviceId: String,
        val deviceType: DeviceType
    )

    override suspend fun execute(parameters: Params): DeviceInfo {
        return printerManager.connectDevice(parameters.deviceId, parameters.deviceType)
    }
}

@Singleton
class GetAvailablePrintersUseCase @Inject constructor(
    private val printerManager: PrinterManager,
    dispatcher: CoroutineDispatcher
) : NoParamsFlowUseCase<List<DeviceInfo>>(dispatcher) {

    override fun execute(): Flow<List<DeviceInfo>> {
        return printerManager.getAvailableDevices()
    }
}

@Singleton
class ReprintLabelUseCase @Inject constructor(
    private val printLabelUseCase: PrintLabelUseCase,
    dispatcher: CoroutineDispatcher
) : BaseUseCase<ReprintLabelUseCase.Params, PrintJob>(dispatcher) {

    data class Params(
        val originalEntry: PrintLogEntry,
        val newQuantity: Int,
        val newCellCode: String
    )

    override suspend fun execute(parameters: Params): PrintJob {
        val originalEntry = parameters.originalEntry
        
        // Создаем новые данные этикетки на основе оригинальной записи
        val labelData = LabelData(
            type = LabelType.STANDARD,
            routeCardNumber = parseRouteCardNumber(originalEntry.qrData),
            partNumber = originalEntry.partNumber,
            partName = originalEntry.partName,
            orderNumber = originalEntry.orderNumber ?: "",
            quantity = parameters.newQuantity,
            cellCode = parameters.newCellCode,
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
            qrData = buildQrData(originalEntry, parameters.newQuantity, parameters.newCellCode)
        )

        // Используем основной UseCase для печати
        val result = printLabelUseCase(PrintLabelUseCase.Params(labelData))
        return when (result) {
            is Result.Success -> result.data
            is Result.Error -> throw Exception(result.message)
        }
    }

    private fun parseRouteCardNumber(qrData: String): String {
        return qrData.split('=').firstOrNull() ?: ""
    }

    private fun buildQrData(entry: PrintLogEntry, quantity: Int, cellCode: String): String {
        val parts = entry.qrData.split('=')
        return if (parts.size >= 4) {
            "${parts[0]}=${parts[1]}=${parts[2]}=$quantity=$cellCode"
        } else {
            entry.qrData
        }
    }
} 