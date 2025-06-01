package com.example.myprinterapp.data

import java.time.LocalDateTime

// Статусы заданий на русском языке
enum class TaskStatus {
    NEW,         // Новое
    IN_PROGRESS, // Выполняется
    COMPLETED,   // Выполнено
    CANCELLED,   // Отменено
    PAUSED,      // Приостановлено
    VERIFIED     // Проверено
}

// Расширение для получения русского названия статуса
fun TaskStatus.toRussianString(): String = when (this) {
    TaskStatus.NEW -> "Новое"
    TaskStatus.IN_PROGRESS -> "Выполняется"
    TaskStatus.COMPLETED -> "Выполнено"
    TaskStatus.CANCELLED -> "Отменено"
    TaskStatus.PAUSED -> "Приостановлено"
    TaskStatus.VERIFIED -> "Проверено"
}

// Детали для комплектации
data class PickDetail(
    val id: Int,
    val partNumber: String,
    val partName: String,
    val quantityToPick: Int,
    val location: String,
    var picked: Int = 0,
    val unit: String = "шт", // Единица измерения
    val comment: String? = null // Комментарий к позиции
) {
    // Процент выполнения позиции
    val completionPercentage: Float
        get() = if (quantityToPick > 0) (picked.toFloat() / quantityToPick.toFloat()) * 100f else 0f

    // Статус позиции
    val status: DetailStatus
        get() = when {
            picked == 0 -> DetailStatus.NOT_STARTED
            picked < quantityToPick -> DetailStatus.PARTIAL
            picked >= quantityToPick -> DetailStatus.COMPLETED
            else -> DetailStatus.NOT_STARTED
        }
}

// Статус отдельной позиции
enum class DetailStatus {
    NOT_STARTED, // Не начато
    PARTIAL,     // Частично
    COMPLETED    // Завершено
}

// Задание на комплектацию
data class PickTask(
    val id: String,
    val date: String,
    val description: String,
    var status: TaskStatus,
    val details: List<PickDetail>,
    val priority: Priority = Priority.NORMAL,
    val customer: String? = null,
    val deadline: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // Общее количество позиций к сборке
    val totalItems: Int
        get() = details.sumOf { it.quantityToPick }

    // Количество собранных позиций
    val pickedItems: Int
        get() = details.sumOf { it.picked }

    // Процент выполнения задания
    val completionPercentage: Float
        get() = if (totalItems > 0) (pickedItems.toFloat() / totalItems.toFloat()) * 100f else 0f

    // Количество позиций в задании
    val positionsCount: Int
        get() = details.size

    // Количество завершенных позиций
    val completedPositions: Int
        get() = details.count { it.status == DetailStatus.COMPLETED }

    // Автоматическое обновление статуса на основе прогресса
    fun updateStatus() {
        status = when {
            pickedItems == 0 -> TaskStatus.NEW
            pickedItems < totalItems -> TaskStatus.IN_PROGRESS
            pickedItems >= totalItems -> TaskStatus.COMPLETED
            else -> status
        }
    }
}

// Приоритет задания
enum class Priority {
    LOW,      // Низкий
    NORMAL,   // Обычный
    HIGH,     // Высокий
    URGENT    // Срочный
}

// Расширение для получения русского названия приоритета
fun Priority.toRussianString(): String = when (this) {
    Priority.LOW -> "Низкий"
    Priority.NORMAL -> "Обычный"
    Priority.HIGH -> "Высокий"
    Priority.URGENT -> "Срочный"
}

// Модель для передачи задания с планшета
data class TaskTransferData(
    val task: PickTask,
    val transferredAt: LocalDateTime,
    val transferredBy: String? = null,
    val deviceId: String? = null
)

enum class ScannerConnectionState {
    DISCONNECTED,
    SEARCHING,
    CONNECTING,
    CONNECTED
}

// Временные заглушки для недостающих типов
enum class Symbology {
    QR_CODE, CODE_128, CODE_39, EAN_13, DATA_MATRIX
}

enum class CharacterSet {
    UTF8, LATIN1, WINDOWS1251
}

data class ScannerConfig(
    val enabled: Boolean = true,
    val beepOnScan: Boolean = true
)

data class EnhancedScanData(
    val data: String,
    val rawData: ByteArray,
    val symbology: Symbology,
    val aimId: String?,
    val codeId: String?,
    val timestamp: Long,
    val quality: Int,
    val characterSet: CharacterSet
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EnhancedScanData

        if (data != other.data) return false
        if (!rawData.contentEquals(other.rawData)) return false
        if (symbology != other.symbology) return false
        if (aimId != other.aimId) return false
        if (codeId != other.codeId) return false
        if (timestamp != other.timestamp) return false
        if (quality != other.quality) return false
        if (characterSet != other.characterSet) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.hashCode()
        result = 31 * result + rawData.contentHashCode()
        result = 31 * result + symbology.hashCode()
        result = 31 * result + (aimId?.hashCode() ?: 0)
        result = 31 * result + (codeId?.hashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + quality
        result = 31 * result + characterSet.hashCode()
        return result
    }
}

data class ScannerDetailedInfo(
    val name: String,
    val address: String,
    val model: String,
    val firmwareVersion: String,
    val serialNumber: String,
    val batteryLevel: Int,
    val supportedSymbologies: List<Symbology>,
    val currentConfig: ScannerConfig
)

enum class QRType {
    CYRILLIC_HEAVY,    // Много кириллицы
    MIXED_CONTENT,     // Смешанный контент
    STANDARD_LATIN     // Стандартный латинский
}