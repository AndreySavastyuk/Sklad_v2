package com.example.myprinterapp.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Базовые модели данных для всего приложения
 */

// ============= ОБЩИЕ МОДЕЛИ =============

@Parcelize
data class DeviceInfo(
    val id: String,
    val name: String,
    val address: String,
    val type: DeviceType,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val lastConnected: @RawValue LocalDateTime? = null,
    val batteryLevel: Int? = null
) : Parcelable

enum class DeviceType {
    PRINTER_BLUETOOTH,
    PRINTER_WIFI,
    PRINTER_BLE,
    SCANNER_BLE,
    SCANNER_BLUETOOTH
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
    PAIRING
}

// ============= МОДЕЛИ ПРИНТЕРА =============

@Parcelize
data class PrintJob(
    val id: String,
    val labelData: LabelData,
    val deviceId: String,
    val status: PrintJobStatus,
    val createdAt: @RawValue LocalDateTime,
    val completedAt: @RawValue LocalDateTime? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0
) : Parcelable

@Parcelize
data class LabelData(
    val type: LabelType,
    val routeCardNumber: String,
    val partNumber: String,
    val partName: String,
    val orderNumber: String,
    val quantity: Int,
    val cellCode: String,
    val date: String,
    val qrData: String,
    val customFields: @RawValue Map<String, String> = emptyMap(),
    val labelType: String? = null,
    val description: String? = null,
    val location: String? = null
) : Parcelable

enum class LabelType {
    STANDARD,
    SMALL,
    LARGE,
    CUSTOM
}

enum class PrintJobStatus {
    PENDING,
    PRINTING,
    COMPLETED,
    FAILED,
    CANCELLED
}

// ============= МОДЕЛИ СКАНЕРА =============

@Parcelize
data class ScanResult(
    val id: String = generateId(),
    val data: String,
    val format: BarcodeFormat,
    val timestamp: @RawValue LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    val deviceId: String? = null,
    val isProcessed: Boolean = false,
    val metadata: ScanMetadata? = null
) : Parcelable

@Parcelize
data class ScanMetadata(
    val width: Int? = null,
    val height: Int? = null,
    val quality: Float? = null,
    val orientation: Int? = null
) : Parcelable

enum class BarcodeFormat {
    QR_CODE,
    CODE_128,
    CODE_39,
    EAN_13,
    EAN_8,
    UPC_A,
    UPC_E,
    DATA_MATRIX,
    PDF_417,
    AZTEC,
    UNKNOWN
}

// ============= МОДЕЛИ ПРИЕМКИ =============

@Parcelize
data class AcceptanceOperation(
    val id: String = generateId(),
    val scannedData: String,
    val parsedData: ParsedQrData,
    val quantity: Int,
    val cellCode: String,
    val timestamp: @RawValue LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    val operatorId: String? = null,
    val printed: Boolean = false,
    val printJobId: String? = null,
    val editHistory: @RawValue List<EditRecord> = emptyList()
) : Parcelable

@Parcelize
data class ParsedQrData(
    val routeCardNumber: String,
    val partNumber: String,
    val partName: String,
    val orderNumber: String,
    val originalQuantity: Int? = null,
    val additionalData: @RawValue Map<String, String> = emptyMap()
) : Parcelable

@Parcelize
data class EditRecord(
    val timestamp: @RawValue LocalDateTime,
    val field: String,
    val oldValue: String,
    val newValue: String,
    val reason: String? = null
) : Parcelable

// ============= МОДЕЛИ КОМПЛЕКТАЦИИ =============

@Parcelize
data class PickTask(
    val id: String,
    val number: String,
    val description: String,
    val priority: Priority,
    val status: TaskStatus,
    val createdAt: @RawValue LocalDateTime,
    val deadline: @RawValue LocalDateTime? = null,
    val assignedTo: String? = null,
    val items: @RawValue List<PickItem> = emptyList(),
    val progress: TaskProgress
) : Parcelable

@Parcelize
data class PickItem(
    val id: String,
    val partNumber: String,
    val partName: String,
    val requiredQuantity: Int,
    val pickedQuantity: Int = 0,
    val location: String,
    val status: ItemStatus = ItemStatus.PENDING,
    val scannedAt: @RawValue LocalDateTime? = null,
    val notes: String? = null
) : Parcelable

@Parcelize
data class TaskProgress(
    val totalItems: Int,
    val completedItems: Int,
    val percentage: Float = if (totalItems > 0) (completedItems.toFloat() / totalItems * 100) else 0f
) : Parcelable

enum class Priority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

enum class TaskStatus {
    CREATED,
    IN_PROGRESS,
    COMPLETED,
    PAUSED,
    CANCELLED
}

enum class ItemStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
}

// ============= МОДЕЛИ НАСТРОЕК =============

// Настройки принтера
data class PrinterSettings(
    val defaultPrinterId: String? = null,
    val printDensity: Int = 8,
    val printSpeed: Int = 4,
    val paperType: PaperType = PaperType.NORMAL,
    val labelSize: LabelSize = LabelSize.SIZE_40x30,
    val autoConnect: Boolean = true,
    val connectionTimeout: Int = 30
)

// Настройки сканера
data class ScannerSettings(
    val autoConnect: Boolean = true,
    val beepOnScan: Boolean = true,
    val vibrationOnScan: Boolean = true,
    val scanMode: ScanMode = ScanMode.SINGLE,
    val enabledFormats: @RawValue Set<BarcodeFormat> = setOf(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128),
    val connectionTimeout: Int = 30
)

// Настройки UI
data class UiSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: String = "ru",
    val showDebugInfo: Boolean = false,
    val animationsEnabled: Boolean = true,
    val confirmationDialogs: Boolean = true
)

// Сетевые настройки
data class NetworkSettings(
    val serverUrl: String? = null,
    val apiKey: String? = null,
    val syncEnabled: Boolean = false,
    val syncInterval: Int = 60,
    val wifiOnly: Boolean = true
)

// Общие настройки приложения
data class AppSettings(
    val printerSettings: PrinterSettings = PrinterSettings(),
    val scannerSettings: ScannerSettings = ScannerSettings(),
    val uiSettings: UiSettings = UiSettings(),
    val networkSettings: NetworkSettings = NetworkSettings()
)

enum class PaperType {
    NORMAL,
    THERMAL,
    LABEL
}

enum class LabelSize {
    SIZE_40x30,
    SIZE_57x40,
    SIZE_80x60,
    CUSTOM
}

enum class ScanMode {
    SINGLE,
    CONTINUOUS,
    BATCH
}

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

// ============= УТИЛИТЫ =============

/**
 * Генерирует уникальный ID
 */
fun generateId(): String = java.util.UUID.randomUUID().toString()

/**
 * UI состояния для экранов
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
}

/**
 * Результат операции
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Result<Nothing>()
}

/**
 * Расширения для конвертации между состояниями
 */
fun <T> Result<T>.toUiState(): UiState<T> = when (this) {
    is Result.Success -> UiState.Success(data)
    is Result.Error -> UiState.Error(message, throwable)
} 