package com.example.myprinterapp.ui.pick

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickManagementScreen(
    tasks: List<PickTask>,
    onOpenTask: (String) -> Unit,
    onCreateTestTask: () -> Unit,
    onImportTasks: () -> Unit,
    onDeleteTask: (String) -> Unit,
    onChangeTaskStatus: (String, TaskStatus) -> Unit,
    onBack: () -> Unit
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<TaskStatus?>(null) }
    var showTestTaskDialog by remember { mutableStateOf(false) }

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
                            "Управление заданиями",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Всего: ${tasks.size} | Новых: ${tasks.count { it.status == TaskStatus.NEW }}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                actions = {
                    // Создать тестовое задание
                    IconButton(onClick = { showTestTaskDialog = true }) {
                        Icon(
                            Icons.Filled.Science,
                            contentDescription = "Тестовое задание",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Импорт заданий
                    IconButton(onClick = onImportTasks) {
                        Icon(
                            Icons.Filled.CloudDownload,
                            contentDescription = "Импорт",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    // Фильтр
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = "Фильтр",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Все задания") },
                                onClick = {
                                    selectedFilter = null
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    if (selectedFilter == null) {
                                        Icon(Icons.Filled.Check, null)
                                    }
                                }
                            )
                            Divider()
                            TaskStatus.values().forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.toRussianString()) },
                                    onClick = {
                                        selectedFilter = status
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        Row {
                                            if (selectedFilter == status) {
                                                Icon(Icons.Filled.Check, null)
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                getStatusIcon(status),
                                                null,
                                                tint = getStatusColor(status)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (filteredTasks.isEmpty()) {
            EmptyTasksState(
                selectedFilter = selectedFilter,
                onCreateTest = { showTestTaskDialog = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    SwipeableTaskCard(
                        task = task,
                        onOpen = { onOpenTask(task.id) },
                        onDelete = { onDeleteTask(task.id) },
                        onChangeStatus = { newStatus ->
                            onChangeTaskStatus(task.id, newStatus)
                        }
                    )
                }
            }
        }
    }

    // Диалог создания тестового задания
    if (showTestTaskDialog) {
        TestTaskCreationDialog(
            onDismiss = { showTestTaskDialog = false },
            onCreate = {
                onCreateTestTask()
                showTestTaskDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskCard(
    task: PickTask,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onChangeStatus: (TaskStatus) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Основное содержимое карточки
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Левая часть - информация
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Номер задания
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "№${task.id}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Статус
                        StatusChip(task.status)

                        // Приоритет
                        if (task.priority != Priority.NORMAL) {
                            PriorityIndicator(task.priority)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Клиент
                    task.customer?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Описание
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(8.dp))

                    // Статистика
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatItem(
                            icon = Icons.Filled.Inventory,
                            value = "${task.positionsCount}",
                            label = "позиций"
                        )
                        StatItem(
                            icon = Icons.Filled.ShoppingCart,
                            value = "${task.totalItems}",
                            label = "товаров"
                        )
                        if (task.completionPercentage > 0) {
                            StatItem(
                                icon = Icons.Filled.CheckCircle,
                                value = "${task.completionPercentage.toInt()}%",
                                label = "готово",
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }

                // Правая часть - меню
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, "Меню")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // Открыть
                        DropdownMenuItem(
                            text = { Text("Открыть") },
                            onClick = {
                                onOpen()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.OpenInNew, null) }
                        )

                        Divider()

                        // Изменить статус
                        Text(
                            "Изменить статус:",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium
                        )

                        TaskStatus.values().filter { it != task.status }.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.toRussianString()) },
                                onClick = {
                                    onChangeStatus(status)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        getStatusIcon(status),
                                        null,
                                        tint = getStatusColor(status)
                                    )
                                }
                            )
                        }

                        Divider()

                        // Удалить
                        DropdownMenuItem(
                            text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            // Прогресс-бар внизу карточки
            if (task.completionPercentage > 0) {
                LinearProgressIndicator(
                    progress = task.completionPercentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = when {
                        task.completionPercentage >= 100 -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: TaskStatus) {
    val color = getStatusColor(status)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                getStatusIcon(status),
                null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(
                status.toRussianString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun PriorityIndicator(priority: Priority) {
    val color = getPriorityColor(priority)
    Icon(
        getPriorityIcon(priority),
        contentDescription = priority.toRussianString(),
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
            .padding(2.dp),
        tint = color
    )
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(16.dp),
            tint = color.copy(alpha = 0.7f)
        )
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = color
        )
        Text(
            label,
            fontSize = 12.sp,
            color = color.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun EmptyTasksState(
    selectedFilter: TaskStatus?,
    onCreateTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Assignment,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (selectedFilter != null) {
                "Нет заданий со статусом \"${selectedFilter.toRussianString()}\""
            } else {
                "Нет активных заданий"
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onCreateTest) {
            Icon(Icons.Filled.Science, null)
            Spacer(Modifier.width(8.dp))
            Text("Создать тестовое задание")
        }
    }
}

@Composable
fun TestTaskCreationDialog(
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Science,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("Создать тестовое задание") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Будет создано тестовое задание со следующими параметрами:")

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("• 5 позиций разных деталей", style = MaterialTheme.typography.bodyMedium)
                        Text("• Случайные ячейки хранения", style = MaterialTheme.typography.bodyMedium)
                        Text("• Статус: Новое", style = MaterialTheme.typography.bodyMedium)
                        Text("• Клиент: Тестовый заказ", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Text(
                    "Это задание можно использовать для проверки работы системы комплектации",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onCreate) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}