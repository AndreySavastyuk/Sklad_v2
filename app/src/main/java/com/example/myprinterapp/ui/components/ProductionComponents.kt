package com.example.myprinterapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myprinterapp.data.Priority

/**
 * Компоненты UI для производственных заданий
 */

@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (status.uppercase()) {
        "CREATED" -> Color(0xFF6B7280) to Icons.Default.Add
        "SENT" -> Color(0xFF3B82F6) to Icons.Default.Send
        "IN_PROGRESS" -> Color(0xFFF59E0B) to Icons.Default.PlayArrow
        "COMPLETED" -> Color(0xFF10B981) to Icons.Default.CheckCircle
        "CANCELLED" -> Color(0xFFEF4444) to Icons.Default.Cancel
        "PAUSED" -> Color(0xFF8B5CF6) to Icons.Default.Pause
        else -> Color(0xFF6B7280) to Icons.Default.Help
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = getStatusDisplayName(status),
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PriorityIndicator(
    priority: Priority,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (priority) {
        Priority.LOW -> Color(0xFF6B7280) to Icons.Default.KeyboardArrowDown
        Priority.NORMAL -> Color(0xFF3B82F6) to Icons.Default.Remove
        Priority.HIGH -> Color(0xFFF59E0B) to Icons.Default.KeyboardArrowUp
        Priority.URGENT -> Color(0xFFEF4444) to Icons.Default.PriorityHigh
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = priority.toRussianString(),
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProgressBar(
    current: Int,
    total: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$current/$total",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = when {
                progress >= 1.0f -> Color(0xFF10B981)
                progress > 0.5f -> Color(0xFF3B82F6)
                progress > 0f -> Color(0xFFF59E0B)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TaskMetricsCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                subtitle?.let { sub ->
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SyncStatusIndicator(
    syncStatus: String,
    lastSyncTime: String? = null,
    modifier: Modifier = Modifier
) {
    val (color, icon, text) = when (syncStatus.uppercase()) {
        "SYNCING" -> Triple(
            Color(0xFF3B82F6),
            Icons.Default.Sync,
            "Синхронизация..."
        )
        "SUCCESS" -> Triple(
            Color(0xFF10B981),
            Icons.Default.CloudDone,
            "Синхронизировано"
        )
        "ERROR" -> Triple(
            Color(0xFFEF4444),
            Icons.Default.CloudOff,
            "Ошибка синхронизации"
        )
        else -> Triple(
            Color(0xFF6B7280),
            Icons.Default.Cloud,
            "Оффлайн"
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = text,
                    color = color,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
                lastSyncTime?.let { time ->
                    Text(
                        text = time,
                        color = color.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count > 0) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.error
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onError,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TaskTimeline(
    events: List<TimelineEvent>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        events.forEachIndexed { index, event ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Индикатор времени
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = event.color,
                        modifier = Modifier.size(12.dp)
                    ) {}
                    
                    if (index < events.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
                
                // Содержимое события
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = event.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (index < events.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Вспомогательные функции и классы

data class TimelineEvent(
    val title: String,
    val description: String,
    val timestamp: String,
    val color: Color
)

private fun getStatusDisplayName(status: String): String = when (status.uppercase()) {
    "CREATED" -> "Создано"
    "SENT" -> "Отправлено"
    "IN_PROGRESS" -> "В работе"
    "COMPLETED" -> "Выполнено"
    "CANCELLED" -> "Отменено"
    "PAUSED" -> "Приостановлено"
    else -> status
}

fun Priority.toRussianString(): String = when (this) {
    Priority.LOW -> "Низкий"
    Priority.NORMAL -> "Обычный"
    Priority.HIGH -> "Высокий"
    Priority.URGENT -> "Срочный"
} 