package com.example.myprinterapp.ui.pick

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.data.*
import com.example.myprinterapp.ui.theme.MyPrinterAppTheme
import java.time.LocalDateTime

// Утилита для затемнения цвета
private fun Color.darker(factor: Float = 0.8f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickTasksScreen(
    tasks: List<PickTask>,
    onOpenTask: (taskId: String) -> Unit,
    onBack: () -> Unit,
    onImportTasks: () -> Unit = {},
    onFilterTasks: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf<TaskStatus?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }

    val filteredTasks = if (selectedFilter != null) {
        tasks.filter { it.status == selectedFilter }
    } else {
        tasks
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Задания на комплектацию",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Всего: ${tasks.size} | Активных: ${tasks.count { it.status == TaskStatus.IN_PROGRESS }}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                actions = {
                    // Кнопка передачи заданий
                    IconButton(
                        onClick = { showTransferDialog = true },
                        enabled = tasks.any { it.status != TaskStatus.COMPLETED }
                    ) {
                        Icon(
                            Icons.Filled.SendToMobile,
                            contentDescription = "Передать на планшет",
                            modifier = Modifier.size(28.dp),
                            tint = if (tasks.any { it.status != TaskStatus.COMPLETED }) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    }

                    // Кнопка импорта заданий
                    IconButton(onClick = onImportTasks) {
                        Icon(
                            Icons.Filled.CloudDownload,
                            contentDescription = "Импорт заданий",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Кнопка фильтра
                    IconButton(onClick = onFilterTasks) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Фильтр",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Фильтры по статусам
            StatusFilterChips(
                selectedStatus = selectedFilter,
                onStatusSelected = { selectedFilter = it },
                taskCounts = tasks.groupBy { it.status }.mapValues { it.value.size }
            )

            if (filteredTasks.isEmpty()) {
                EmptyState(
                    message = if (selectedFilter != null) {
                        "Нет заданий со статусом \"${selectedFilter!!.toRussianString()}\""
                    } else {
                        "Нет доступных заданий на комплектацию"
                    },
                    icon = Icons.Filled.PlaylistRemove,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            PickTaskItem(task = task, onOpenTask = { onOpenTask(task.id) })
                        }
                    }
                }
            }
        }
    }

    // Диалог передачи заданий
    if (showTransferDialog) {
        TaskTransferDialog(
            tasks = tasks,
            onDismiss = { showTransferDialog = false },
            onTasksSelected = { selectedTasks ->
                // TODO: Обработка выбранных заданий
                showTransferDialog = false
            }
        )
    }
}

@Composable
fun StatusFilterChips(
    selectedStatus: TaskStatus?,
    onStatusSelected: (TaskStatus?) -> Unit,
    taskCounts: Map<TaskStatus, Int>
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Чип "Все"
        item {
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onStatusSelected(null) },
                label = {
                    Text(
                        "Все (${taskCounts.values.sum()})",
                        fontWeight = if (selectedStatus == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = if (selectedStatus == null) {
                    { Icon(Icons.Filled.Done, contentDescription = null, Modifier.size(18.dp)) }
                } else null
            )
        }

        // Чипы для каждого статуса
        items(TaskStatus.values().toList()) { status ->
            val count = taskCounts[status] ?: 0
            if (count > 0) {
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { onStatusSelected(status) },
                    label = {
                        Text(
                            "${status.toRussianString()} ($count)",
                            fontWeight = if (selectedStatus == status) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            getStatusIcon(status),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = getStatusColor(status)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getStatusColor(status).copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}

@Composable
fun PickTaskItem(
    task: PickTask,
    onOpenTask: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline.darker(0.8f)
    val statusColor = getStatusColor(task.status)
    val statusIcon = getStatusIcon(task.status)
    val priorityColor = getPriorityColor(task.priority)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenTask),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Заголовок с приоритетом
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Индикатор приоритета
                    if (task.priority != Priority.NORMAL) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = priorityColor.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                getPriorityIcon(task.priority),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(2.dp),
                                tint = priorityColor
                            )
                        }
                    }

                    Text(
                        text = "Задание № ${task.id}",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Статус
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.status.toRussianString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Информация о задании
            if (task.customer != null) {
                InfoRow(
                    icon = Icons.Filled.Business,
                    label = "Клиент:",
                    value = task.customer,
                    iconTint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            InfoRow(icon = Icons.Filled.CalendarToday, label = "Дата:", value = task.date)

            if (task.deadline != null) {
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow(
                    icon = Icons.Filled.Schedule,
                    label = "Срок:",
                    value = task.deadline,
                    iconTint = if (task.priority == Priority.URGENT) priorityColor else null
                )
            }

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow(
                    icon = Icons.Filled.Description,
                    label = "Описание:",
                    value = task.description,
                    singleLineValue = false,
                    maxLinesValue = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Статистика позиций
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    InfoRow(
                        icon = Icons.Filled.FormatListNumbered,
                        label = "Позиций:",
                        value = "${task.completedPositions}/${task.positionsCount}"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    InfoRow(
                        icon = Icons.Filled.Inventory,
                        label = "Товаров:",
                        value = "${task.pickedItems}/${task.totalItems} шт"
                    )
                }

                // Круговой прогресс
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(60.dp)
                ) {
                    CircularProgressIndicator(
                        progress = task.completionPercentage / 100f,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 6.dp,
                        color = when {
                            task.completionPercentage >= 100 -> Color(0xFF4CAF50)
                            task.completionPercentage > 0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    Text(
                        text = "${task.completionPercentage.toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Кнопка действия
            Button(
                onClick = onOpenTask,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (task.status) {
                        TaskStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
                        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Icon(
                    when (task.status) {
                        TaskStatus.COMPLETED -> Icons.Filled.Visibility
                        TaskStatus.CANCELLED -> Icons.Filled.Restore
                        else -> Icons.Filled.PlayArrow
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when (task.status) {
                        TaskStatus.COMPLETED -> "Просмотреть"
                        TaskStatus.CANCELLED -> "Восстановить"
                        TaskStatus.NEW -> "Начать сборку"
                        TaskStatus.IN_PROGRESS -> "Продолжить сборку"
                        TaskStatus.PAUSED -> "Возобновить"
                        TaskStatus.VERIFIED -> "Детали"
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    singleLineValue: Boolean = true,
    maxLinesValue: Int = 1,
    iconTint: Color? = null
) {
    Row(verticalAlignment = if (singleLineValue) Alignment.CenterVertically else Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp).padding(top = if(singleLineValue) 0.dp else 2.dp),
            tint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label ",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            overflow = if (singleLineValue) TextOverflow.Ellipsis else TextOverflow.Visible,
            maxLines = if (singleLineValue) 1 else maxLinesValue
        )
    }
}

@Composable
fun EmptyState(message: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// Preview функции
@Preview(showBackground = true, name = "Pick Tasks List")
@Composable
fun PickTasksScreenPreview() {
    MyPrinterAppTheme {
        PickTasksScreen(
            tasks = generatePreviewTasks(),
            onOpenTask = {},
            onBack = {}
        )
    }
}

private fun generatePreviewTasks(): List<PickTask> {
    val details1 = listOf(
        PickDetail(1, "PART-001", "Деталь A1", 5, "S69", 2),
        PickDetail(2, "PART-002", "Деталь A2", 3, "S70", 3)
    )
    val details2 = listOf(
        PickDetail(3, "PART-101", "Деталь B1", 10, "S01", 5),
        PickDetail(4, "PART-102", "Деталь B2", 8, "S02", 0)
    )

    return listOf(
        PickTask("T-2024-001", "23.05.2025", "Срочная сборка для VIP клиента",
            TaskStatus.NEW, details1, Priority.URGENT, "ООО Ромашка", "24.05.2025"),
        PickTask("T-2024-002", "23.05.2025", "Плановая комплектация",
            TaskStatus.IN_PROGRESS, details2, Priority.NORMAL, "ИП Петров"),
        PickTask("T-2024-003", "22.05.2025", "Выполнено вчера",
            TaskStatus.COMPLETED, details1.map { it.copy(picked = it.quantityToPick) }, Priority.HIGH),
        PickTask("T-2024-004", "21.05.2025", "Отменен клиентом",
            TaskStatus.CANCELLED, details2, Priority.LOW, "ЗАО Техно")
    )
}