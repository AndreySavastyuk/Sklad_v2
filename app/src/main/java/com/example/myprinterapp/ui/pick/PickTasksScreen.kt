package com.example.myprinterapp.ui.pick

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Утилита для затемнения цвета
private fun Color.darker(factor: Float = 0.8f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}

// Форматирование даты в нужный формат
private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
        date.format(formatter)
    } catch (e: Exception) {
        dateString // Возвращаем исходную строку если не удалось распарсить
    }
}

// Извлечение номера задания из ID
private fun extractTaskNumber(taskId: String): String {
    return taskId.replace("ZADANIE-", "")
}

// Функция для извлечения номера заказа из описания
private fun extractOrderNumber(description: String): String {
    // Ищем паттерн вида 2023/023
    val orderPattern = Regex("""\d{4}/\d{3}""")
    return orderPattern.find(description)?.value ?: ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickTasksScreen(
    tasks: List<PickTask>,
    onOpenTask: (taskId: String) -> Unit,
    onBack: () -> Unit,
    onImportTasks: () -> Unit = {},
    onFilterTasks: () -> Unit = {},
    onMarkAsCompleted: (taskId: String) -> Unit = {},
    onPauseTask: (taskId: String) -> Unit = {},
    onCancelTask: (taskId: String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf<TaskStatus?>(null) }
    var showTaskMenu by remember { mutableStateOf<String?>(null) }

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
                            ImprovedPickTaskItem(
                                task = task,
                                onOpenTask = { onOpenTask(task.id) },
                                onMenuClick = { showTaskMenu = task.id },
                                onMarkCompleted = { onMarkAsCompleted(task.id) },
                                onPauseTask = { onPauseTask(task.id) },
                                onCancelTask = { onCancelTask(task.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Меню действий для задания
    showTaskMenu?.let { taskId ->
        val task = tasks.find { it.id == taskId }
        if (task != null) {
            TaskActionsDialog(
                task = task,
                onDismiss = { showTaskMenu = null },
                onOpenTask = {
                    onOpenTask(taskId)
                    showTaskMenu = null
                },
                onMarkCompleted = {
                    onMarkAsCompleted(taskId)
                    showTaskMenu = null
                },
                onPauseTask = {
                    onPauseTask(taskId)
                    showTaskMenu = null
                },
                onCancelTask = {
                    onCancelTask(taskId)
                    showTaskMenu = null
                }
            )
        }
    }
}

@Composable
fun ImprovedPickTaskItem(
    task: PickTask,
    onOpenTask: () -> Unit,
    onMenuClick: () -> Unit,
    onMarkCompleted: () -> Unit,
    onPauseTask: () -> Unit,
    onCancelTask: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline.darker(0.8f)
    val statusColor = getStatusColor(task.status)
    val priorityColor = getPriorityColor(task.priority)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Заголовок задания
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Индикатор приоритета
                        if (task.priority != Priority.NORMAL) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = priorityColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    getPriorityIcon(task.priority),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(4.dp),
                                    tint = priorityColor
                                )
                            }
                        }

                        Text(
                            text = "Задание №${extractTaskNumber(task.id)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Статус
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = getStatusIcon(task.status),
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.status.toRussianString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Кнопка меню
                IconButton(onClick = onMenuClick) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Меню действий",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Основная информация
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Дата и заказ в одной строке
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip(
                        icon = Icons.Filled.CalendarToday,
                        label = "Дата",
                        value = formatDate(task.date),
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )

                    // Номер заказа (извлекаем из описания или других данных)
                    val orderNumber = extractOrderNumber(task.description)
                    if (orderNumber.isNotEmpty()) {
                        InfoChip(
                            icon = Icons.Filled.Receipt,
                            label = "Заказ",
                            value = orderNumber,
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    }
                }

                // Клиент
                if (task.customer != null) {
                    InfoRow(
                        icon = Icons.Filled.Business,
                        label = "Клиент:",
                        value = task.customer,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }

                // Описание
                if (task.description.isNotBlank()) {
                    InfoRow(
                        icon = Icons.Filled.Description,
                        label = "Описание:",
                        value = task.description,
                        singleLineValue = false,
                        maxLinesValue = 2
                    )
                }

                // Срок выполнения
                if (task.deadline != null) {
                    InfoRow(
                        icon = Icons.Filled.Schedule,
                        label = "Срок:",
                        value = formatDate(task.deadline),
                        iconTint = if (task.priority == Priority.URGENT) priorityColor else MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Прогресс и статистика
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Статистика
                Column {
                    Text(
                        "Позиций: ${task.completedPositions}/${task.positionsCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Товаров: ${task.pickedItems}/${task.totalItems} шт",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Кнопка основного действия
            Button(
                onClick = onOpenTask,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (task.status) {
                        TaskStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
                        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.primary
                    }
                ),
                shape = MaterialTheme.shapes.medium
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
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: ImageVector,
    label: String,
    value: String,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TaskActionsDialog(
    task: PickTask,
    onDismiss: () -> Unit,
    onOpenTask: () -> Unit,
    onMarkCompleted: () -> Unit,
    onPauseTask: () -> Unit,
    onCancelTask: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Задание №${extractTaskNumber(task.id)}")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Выберите действие:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Кнопки действий
                TaskActionButton(
                    icon = Icons.Filled.PlayArrow,
                    text = "Открыть задание",
                    onClick = onOpenTask
                )

                if (task.status == TaskStatus.IN_PROGRESS || task.status == TaskStatus.PAUSED) {
                    TaskActionButton(
                        icon = Icons.Filled.CheckCircle,
                        text = "Отметить завершенным",
                        onClick = onMarkCompleted
                    )
                }

                if (task.status == TaskStatus.IN_PROGRESS) {
                    TaskActionButton(
                        icon = Icons.Filled.Pause,
                        text = "Приостановить",
                        onClick = onPauseTask
                    )
                }

                if (task.status != TaskStatus.CANCELLED && task.status != TaskStatus.COMPLETED) {
                    TaskActionButton(
                        icon = Icons.Filled.Cancel,
                        text = "Отменить задание",
                        onClick = onCancelTask,
                        isDestructive = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun TaskActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ),
        border = BorderStroke(
            1.dp,
            if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
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
        PickDetail(1, "PART-001", "Деталь A1", 5, "A69", 2),
        PickDetail(2, "PART-002", "Деталь A2", 3, "A70", 3)
    )
    val details2 = listOf(
        PickDetail(3, "PART-101", "Деталь B1", 10, "B01", 5),
        PickDetail(4, "PART-102", "Деталь B2", 8, "B02", 0)
    )

    return listOf(
        PickTask("ZADANIE-001", "2025-05-30", "Сборка для клиента 'ООО Ромашка' по заказу 2023/023",
            TaskStatus.NEW, details1, Priority.URGENT, "ООО Ромашка", "2025-06-01"),
        PickTask("ZADANIE-002", "2025-05-29", "Плановая комплектация заказа 2024/156",
            TaskStatus.IN_PROGRESS, details2, Priority.NORMAL, "ИП Петров", "2025-05-31"),
        PickTask("ZADANIE-003", "2025-05-28", "Выполнено вчера заказ 2024/078",
            TaskStatus.COMPLETED, details1.map { it.copy(picked = it.quantityToPick) }, Priority.HIGH),
        PickTask("ZADANIE-004", "2025-05-27", "Отменен клиентом заказ 2023/199",
            TaskStatus.CANCELLED, details2, Priority.LOW, "ЗАО Техно")
    )
}