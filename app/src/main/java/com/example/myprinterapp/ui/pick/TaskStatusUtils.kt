package com.example.myprinterapp.ui.pick

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myprinterapp.data.Priority
import com.example.myprinterapp.data.TaskStatus

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

@Composable
fun getPriorityColor(priority: Priority): Color {
    return when (priority) {
        Priority.LOW -> Color(0xFF03A9F4) // Голубой
        Priority.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
        Priority.HIGH -> Color(0xFFFF9800) // Оранжевый
        Priority.URGENT -> Color(0xFFF44336) // Красный
    }
}

fun getPriorityIcon(priority: Priority): ImageVector {
    return when (priority) {
        Priority.LOW -> Icons.Filled.ArrowDownward
        Priority.NORMAL -> Icons.Filled.Remove
        Priority.HIGH -> Icons.Filled.ArrowUpward
        Priority.URGENT -> Icons.Filled.PriorityHigh
    }
}