package com.example.myprinterapp.printer

import com.example.myprinterapp.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.Unit
import kotlin.String
import kotlin.Int
import kotlin.Exception

/**
 * Интерфейс для управления принтерами
 */
interface PrinterManager {
    /**
     * Печать этикетки
     */
    suspend fun printLabel(printJob: PrintJob): Result<Unit>
    
    /**
     * Подключение к устройству
     */
    suspend fun connectDevice(deviceId: String, deviceType: DeviceType): DeviceInfo
    
    /**
     * Получение списка доступных устройств
     */
    fun getAvailableDevices(): Flow<List<DeviceInfo>>
    
    /**
     * Получение устройства по умолчанию
     */
    suspend fun getDefaultDevice(): DeviceInfo?
    
    /**
     * Отключение от устройства
     */
    suspend fun disconnectDevice(deviceId: String)
    
    /**
     * Проверка состояния подключения
     */
    suspend fun isConnected(deviceId: String): Boolean
}

/**
 * Реальная реализация PrinterManager использующая PrinterService
 */
class RealPrinterManager @Inject constructor(
    private val printerService: PrinterService,
    private val printerSettings: com.example.myprinterapp.data.PrinterSettings
) : PrinterManager {
    
    override suspend fun printLabel(printJob: PrintJob): Result<Unit> {
        return try {
            android.util.Log.d("RealPrinterManager", "Printing label for: ${printJob.labelData.partNumber}")
            
            // Используем PrinterService для печати
            printerService.printLabel(printJob.labelData, printJob.labelData.type).fold(
                onSuccess = {
                    android.util.Log.d("RealPrinterManager", "Label printed successfully")
                    Result.Success(Unit)
                },
                onFailure = { error ->
                    android.util.Log.e("RealPrinterManager", "Print failed: ${error.message}")
                    Result.Error("Ошибка печати: ${error.message}", error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("RealPrinterManager", "Print error", e)
            Result.Error("Ошибка печати: ${e.message}", e)
        }
    }
    
    override suspend fun connectDevice(deviceId: String, deviceType: DeviceType): DeviceInfo {
        return try {
            android.util.Log.d("RealPrinterManager", "Connecting to device: $deviceId")
            
            // Используем PrinterService для подключения
            printerService.connect(deviceId).fold(
                onSuccess = {
                    DeviceInfo(
                        id = deviceId,
                        name = printerSettings.printerName,
                        address = deviceId,
                        type = deviceType,
                        connectionState = com.example.myprinterapp.data.models.ConnectionState.CONNECTED,
                        lastConnected = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                },
                onFailure = { error ->
                    throw Exception("Ошибка подключения: ${error.message}")
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("RealPrinterManager", "Connection error", e)
            throw e
        }
    }
    
    override fun getAvailableDevices(): Flow<List<DeviceInfo>> {
        // Возвращаем сохраненные принтеры
        val savedPrinter = if (printerSettings.printerMacAddress.isNotEmpty()) {
            listOf(
                DeviceInfo(
                    id = printerSettings.printerMacAddress,
                    name = printerSettings.printerName,
                    address = printerSettings.printerMacAddress,
                    type = DeviceType.PRINTER_BLUETOOTH,
                    connectionState = when (printerService.connectionState.value) {
                        ConnectionState.CONNECTED -> com.example.myprinterapp.data.models.ConnectionState.CONNECTED
                        ConnectionState.CONNECTING -> com.example.myprinterapp.data.models.ConnectionState.CONNECTING
                        ConnectionState.DISCONNECTED -> com.example.myprinterapp.data.models.ConnectionState.DISCONNECTED
                    }
                )
            )
        } else {
            emptyList()
        }
        return flowOf(savedPrinter)
    }
    
    override suspend fun getDefaultDevice(): DeviceInfo? {
        return if (printerSettings.printerMacAddress.isNotEmpty()) {
            DeviceInfo(
                id = printerSettings.printerMacAddress,
                name = printerSettings.printerName,
                address = printerSettings.printerMacAddress,
                type = DeviceType.PRINTER_BLUETOOTH,
                connectionState = when (printerService.connectionState.value) {
                    ConnectionState.CONNECTED -> com.example.myprinterapp.data.models.ConnectionState.CONNECTED
                    ConnectionState.CONNECTING -> com.example.myprinterapp.data.models.ConnectionState.CONNECTING
                    ConnectionState.DISCONNECTED -> com.example.myprinterapp.data.models.ConnectionState.DISCONNECTED
                }
            )
        } else {
            null
        }
    }
    
    override suspend fun disconnectDevice(deviceId: String) {
        android.util.Log.d("RealPrinterManager", "Disconnecting device: $deviceId")
        printerService.disconnect()
    }
    
    override suspend fun isConnected(deviceId: String): Boolean {
        return printerService.connectionState.value == ConnectionState.CONNECTED &&
               printerSettings.printerMacAddress == deviceId
    }
}

/**
 * Базовая реализация PrinterManager для тестирования
 */
class PrinterManagerImpl(
    private val context: android.content.Context
) : PrinterManager {
    
    override suspend fun printLabel(printJob: PrintJob): Result<Unit> {
        return try {
            // Имитируем процесс печати
            kotlinx.coroutines.delay(1500)
            
            // Простая логика - всегда успешно
            android.util.Log.d("PrinterManager", "Этикетка напечатана: ${printJob.labelData.partNumber}")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("PrinterManager", "Ошибка печати", e)
            Result.Error("Ошибка печати: ${e.message}", e)
        }
    }
    
    override suspend fun connectDevice(deviceId: String, deviceType: DeviceType): DeviceInfo {
        // Имитируем подключение
        kotlinx.coroutines.delay(1000)
        
        val deviceName = when (deviceType) {
            DeviceType.PRINTER_BLUETOOTH -> "BT Printer"
            DeviceType.PRINTER_WIFI -> "WiFi Printer"
            else -> "Unknown Printer"
        }
        
        return DeviceInfo(
            id = deviceId,
            name = deviceName,
            address = "AA:BB:CC:DD:EE:FF",
            type = deviceType,
            connectionState = com.example.myprinterapp.data.models.ConnectionState.CONNECTED,
            lastConnected = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )
    }
    
    override fun getAvailableDevices(): Flow<List<DeviceInfo>> {
        // Возвращаем простой список устройств
        val devices = listOf(
            DeviceInfo(
                id = "test_printer_1",
                name = "Test Printer 1",
                address = "AA:BB:CC:DD:EE:01",
                type = DeviceType.PRINTER_BLUETOOTH,
                connectionState = com.example.myprinterapp.data.models.ConnectionState.DISCONNECTED
            ),
            DeviceInfo(
                id = "test_printer_2",
                name = "Test Printer 2",
                address = "192.168.1.100",
                type = DeviceType.PRINTER_WIFI,
                connectionState = com.example.myprinterapp.data.models.ConnectionState.DISCONNECTED
            )
        )
        return flowOf(devices)
    }
    
    override suspend fun getDefaultDevice(): DeviceInfo? {
        // Возвращаем первое доступное устройство
        return DeviceInfo(
            id = "default_printer",
            name = "Default Printer",
            address = "AA:BB:CC:DD:EE:00",
            type = DeviceType.PRINTER_BLUETOOTH,
            connectionState = com.example.myprinterapp.data.models.ConnectionState.CONNECTED
        )
    }
    
    override suspend fun disconnectDevice(deviceId: String) {
        android.util.Log.d("PrinterManager", "Отключение принтера: $deviceId")
    }
    
    override suspend fun isConnected(deviceId: String): Boolean {
        return deviceId == "default_printer"
    }
} 