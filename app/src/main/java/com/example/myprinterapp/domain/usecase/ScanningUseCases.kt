package com.example.myprinterapp.domain.usecase

import com.example.myprinterapp.data.models.*
import com.example.myprinterapp.data.repo.ScanHistoryRepository
import com.example.myprinterapp.scanner.ScannerManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use Cases для работы со сканированием
 */

@Singleton
class ProcessScanResultUseCase @Inject constructor(
    private val scanHistoryRepository: ScanHistoryRepository,
    private val qrParser: QrCodeParser,
    dispatcher: CoroutineDispatcher
) : BaseUseCase<ProcessScanResultUseCase.Params, ParsedData>(dispatcher) {

    data class Params(
        val scanData: String,
        val context: String = "general" // acceptance, picking, general
    )

    override suspend fun execute(parameters: Params): ParsedData {
        val parsedData = when (parameters.context) {
            "acceptance" -> qrParser.parseForAcceptance(parameters.scanData)
            "picking" -> qrParser.parseForPicking(parameters.scanData)
            else -> qrParser.parseGeneral(parameters.scanData)
        }

        // Сохраняем результат сканирования
        val scanResult = ScanResult(
            data = parameters.scanData,
            format = BarcodeFormat.QR_CODE, // TODO: определять автоматически
            isProcessed = true
        )
        scanHistoryRepository.saveScanResult(scanResult)

        return parsedData
    }
}

@Singleton
class GetScanHistoryUseCase @Inject constructor(
    private val repository: ScanHistoryRepository,
    dispatcher: CoroutineDispatcher
) : NoParamsFlowUseCase<List<ScanResult>>(dispatcher) {

    override fun execute(): Flow<List<ScanResult>> {
        return repository.getScanHistory()
    }
}

@Singleton
class ConnectScannerUseCase @Inject constructor(
    private val scannerManager: ScannerManager,
    dispatcher: CoroutineDispatcher
) : BaseUseCase<ConnectScannerUseCase.Params, DeviceInfo>(dispatcher) {

    data class Params(
        val deviceId: String,
        val deviceType: DeviceType
    )

    override suspend fun execute(parameters: Params): DeviceInfo {
        return scannerManager.connectDevice(parameters.deviceId, parameters.deviceType)
    }
}

@Singleton
class GetAvailableScannersUseCase @Inject constructor(
    private val scannerManager: ScannerManager,
    dispatcher: CoroutineDispatcher
) : NoParamsFlowUseCase<List<DeviceInfo>>(dispatcher) {

    override fun execute(): Flow<List<DeviceInfo>> {
        return scannerManager.getAvailableDevices()
    }
}

/**
 * Интерфейс для парсинга QR-кодов
 */
interface QrCodeParser {
    suspend fun parseForAcceptance(data: String): ParsedData
    suspend fun parseForPicking(data: String): ParsedData
    suspend fun parseGeneral(data: String): ParsedData
}

/**
 * Результаты парсинга QR-кодов
 */
sealed class ParsedData {
    data class AcceptanceData(
        val routeCardNumber: String,
        val partNumber: String,
        val partName: String,
        val orderNumber: String,
        val quantity: Int?
    ) : ParsedData()

    data class PickingData(
        val itemId: String,
        val location: String,
        val quantity: Int?
    ) : ParsedData()

    data class GeneralData(
        val data: String,
        val type: String
    ) : ParsedData()

    data class Error(
        val message: String,
        val originalData: String
    ) : ParsedData()
} 