package com.example.myprinterapp.ui.pick

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myprinterapp.data.Priority
import com.example.myprinterapp.data.TaskStatus

/**
 * Утилиты для работы со статусами и приоритетами заданий
 */

/**
 * Получение цвета для статуса задания
 */
@Composable
fun getStatusColor(status: TaskStatus): Color {
    return when (status) {
        TaskStatus.NEW -> MaterialTheme.colorScheme.primary
        TaskStatus.IN_PROGRESS -> Color(0xFFFF9800) // Оранжевый
        TaskStatus.COMPLETED -> Color(0xFF4CAF50) // Зеленый
        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.error
        TaskStatus.PAUSED -> Color(0xFF9E9E9E) // Серый
        TaskStatus.VERIFIED -> Color(0xFF2196F3) // Синий
    }
}

/**
 * Получение иконки для статуса задания
 */
@Composable
fun getStatusIcon(status: TaskStatus): ImageVector {
    return when (status) {
        TaskStatus.NEW -> Icons.Filled.FiberNew
        TaskStatus.IN_PROGRESS -> Icons.Filled.Autorenew
        TaskStatus.COMPLETED -> Icons.Filled.CheckCircle
        TaskStatus.CANCELLED -> Icons.Filled.Cancel
        TaskStatus.PAUSED -> Icons.Filled.Pause
        TaskStatus.VERIFIED -> Icons.Filled.VerifiedUser
    }
}

/**
 * Получение цвета для приоритета задания
 */
@Composable
fun getPriorityColor(priority: Priority): Color {
    return when (priority) {
        Priority.LOW -> Color(0xFF03A9F4) // Голубой
        Priority.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
        Priority.HIGH -> Color(0xFFFF9800) // Оранжевый
        Priority.URGENT -> Color(0xFFF44336) // Красный
    }
}

/**
 * Получение иконки для приоритета задания
 */
fun getPriorityIcon(priority: Priority): ImageVector {
    return when (priority) {
        Priority.LOW -> Icons.Filled.ArrowDownward
        Priority.NORMAL -> Icons.Filled.Remove
        Priority.HIGH -> Icons.Filled.ArrowUpward
        Priority.URGENT -> Icons.Filled.PriorityHigh
    }
}

/**
 * Получение описания статуса на русском языке
 */
fun getStatusDescription(status: TaskStatus): String {
    return when (status) {
        TaskStatus.NEW -> "Новое задание готово к выполнению"
        TaskStatus.IN_PROGRESS -> "Задание выполняется"
        TaskStatus.COMPLETED -> "Задание завершено"
        TaskStatus.CANCELLED -> "Задание отменено"
        TaskStatus.PAUSED -> "Задание приостановлено"
        TaskStatus.VERIFIED -> "Задание проверено"
    }
}

/**
 * Получение описания приоритета на русском языке
 */
fun getPriorityDescription(priority: Priority): String {
    return when (priority) {
        Priority.LOW -> "Низкий приоритет"
        Priority.NORMAL -> "Обычный приоритет"
        Priority.HIGH -> "Высокий приоритет"
        Priority.URGENT -> "Срочное задание"
    }
}

/**
 * Проверка, можно ли изменить статус задания
 */
fun canChangeStatus(currentStatus: TaskStatus, newStatus: TaskStatus): Boolean {
    return when (currentStatus) {
        TaskStatus.NEW -> newStatus in listOf(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED)
        TaskStatus.IN_PROGRESS -> newStatus in listOf(TaskStatus.COMPLETED, TaskStatus.PAUSED, TaskStatus.CANCELLED)
        TaskStatus.PAUSED -> newStatus in listOf(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED, TaskStatus.COMPLETED)
        TaskStatus.COMPLETED -> newStatus == TaskStatus.VERIFIED
        TaskStatus.CANCELLED -> newStatus == TaskStatus.NEW
        TaskStatus.VERIFIED -> false // Проверенные задания нельзя изменять
    }
}

/**
 * Получение списка доступных статусов для перехода
 */
fun getAvailableStatusTransitions(currentStatus: TaskStatus): List<TaskStatus> {
    return TaskStatus.values().filter { newStatus ->
        newStatus != currentStatus && canChangeStatus(currentStatus, newStatus)
    }
}

/**
 * Проверка, является ли статус финальным (завершенным)
 */
fun isFinalStatus(status: TaskStatus): Boolean {
    return status in listOf(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.VERIFIED)
}

/**
 * Проверка, является ли статус активным (задание в работе)
 */
fun isActiveStatus(status: TaskStatus): Boolean {
    return status in listOf(TaskStatus.NEW, TaskStatus.IN_PROGRESS, TaskStatus.PAUSED)
}

/**
 * Получение прогресса выполнения на основе статуса (для анимаций)
 */
fun getStatusProgress(status: TaskStatus): Float {
    return when (status) {
        TaskStatus.NEW -> 0.0f
        TaskStatus.IN_PROGRESS -> 0.5f
        TaskStatus.PAUSED -> 0.3f
        TaskStatus.COMPLETED -> 1.0f
        TaskStatus.VERIFIED -> 1.0f
        TaskStatus.CANCELLED -> 0.0f
    }
}