package com.example.myprinterapp.ui.pick // Убедитесь, что пакет правильный

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.data.PickTask
import com.example.myprinterapp.data.TaskStatus
import com.example.myprinterapp.ui.theme.MyPrinterAppTheme // Предполагая, что у вас есть тема

// Утилита для затемнения цвета, если она не в общем файле
// fun Color.darker(factor: Float = 0.8f): Color { ... }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickTasksScreen(
    tasks: List<PickTask>,
    onOpenTask: (taskId: String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Задания на комплектацию",
                        fontSize = 24.sp, // Немного меньше для соответствия
                        fontWeight = FontWeight.Bold
                    )
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        if (tasks.isEmpty()) {
            EmptyState(
                message = "Нет доступных заданий на комплектацию.",
                icon = Icons.Filled.PlaylistRemove,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    PickTaskItem(task = task, onOpenTask = { onOpenTask(task.id) })
                }
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Задание № ${task.id}",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = "Статус: ${task.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = task.status.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            InfoRow(icon = Icons.Filled.CalendarToday, label = "Дата:", value = task.date)

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

            Spacer(modifier = Modifier.height(8.dp))

            val totalItems = task.details.sumOf { it.quantityToPick }
            val pickedItems = task.details.sumOf { it.picked }
            val progress = if (totalItems > 0) pickedItems.toFloat() / totalItems.toFloat() else 0f

            InfoRow(
                icon = Icons.Filled.FormatListNumbered,
                label = "Позиций к сборке:",
                value = "${task.details.size} шт."
            )
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow(
                icon = Icons.Filled.Inventory, // Или Icons.Filled.FactCheck
                label = "Собрано/Всего:",
                value = "$pickedItems / $totalItems шт."
            )

            if (totalItems > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress =  progress,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.3f),
                )
            }


            Spacer(modifier = Modifier.height(10.dp))

            // Кнопка или выделение для перехода
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onOpenTask) {
                    Text("Открыть детали", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
                }
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
    maxLinesValue: Int = 1
) {
    Row(verticalAlignment = if (singleLineValue) Alignment.CenterVertically else Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp).padding(top = if(singleLineValue) 0.dp else 2.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
fun getStatusColor(status: TaskStatus): Color {
    return when (status) {
        TaskStatus.NEW -> MaterialTheme.colorScheme.primary
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary // Можно выбрать другой цвет
        TaskStatus.COMPLETED -> Color(0xFF388E3C) // Зеленый
        TaskStatus.CANCELED -> MaterialTheme.colorScheme.error
    }
}

@Composable
fun getStatusIcon(status: TaskStatus): ImageVector {
    return when (status) {
        TaskStatus.NEW -> Icons.Filled.FiberNew
        TaskStatus.IN_PROGRESS -> Icons.Filled.Autorenew // или DonutLarge, Pending
        TaskStatus.COMPLETED -> Icons.Filled.CheckCircle
        TaskStatus.CANCELED -> Icons.Filled.Cancel
    }
}

@Composable
fun EmptyState(message: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
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

// Утилита для затемнения цвета (если она не в общем файле)
private fun Color.darker(factor: Float = 0.8f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}


// ----- Preview -----
val previewPickTasks = listOf(
    PickTask("101", "2024-07-28", "Сборка для клиента A", TaskStatus.NEW, listOf(
        // ... детали ...
    )),
    PickTask("102", "2024-07-29", "Срочная комплектация для VIP", TaskStatus.IN_PROGRESS, listOf(
        // ... детали ...
    )),
    PickTask("103", "2024-07-27", "", TaskStatus.COMPLETED, listOf(
        // ... детали ...
    )),
    PickTask("104", "2024-07-26", "Отменено клиентом", TaskStatus.CANCELED, listOf(
        // ... детали ...
    ))
)
// Добавьте данные для PickDetail, если хотите видеть прогресс в превью
private fun generatePreviewTasks(): List<PickTask> {
    val details1 = listOf(
        com.example.myprinterapp.data.PickDetail(1, "PART-001", "Деталь A1", 5, "S69", 2),
        com.example.myprinterapp.data.PickDetail(2, "PART-002", "Деталь A2", 3, "S70", 3)
    )
    val details2 = listOf(
        com.example.myprinterapp.data.PickDetail(1, "PART-101", "Деталь B1", 10, "S01", 1)
    )
    val details3 = listOf(
        com.example.myprinterapp.data.PickDetail(1, "PART-201", "Деталь C1", 2, "A05", 2)
    )

    return listOf(
        PickTask("T-2024-001", "10.12.2024", "Сборка для Клиента А", TaskStatus.NEW, details1),
        PickTask("T-2024-002", "11.12.2024", "Плановая сборка компонентов", TaskStatus.IN_PROGRESS, details2),
        PickTask("T-2024-003", "09.12.2024", "Заказ отменен", TaskStatus.CANCELED, details2.map { it.copy(quantityToPick = 5, picked = 0) }),
        PickTask("T-2024-004", "08.12.2024", "Полностью укомплектовано", TaskStatus.COMPLETED, details3)
    )
}


@Preview(showBackground = true, name = "Pick Tasks List")
@Composable
fun PickTasksScreenPreview() {
    MyPrinterAppTheme { // Оберните в вашу тему
        PickTasksScreen(
            tasks = generatePreviewTasks(),
            onOpenTask = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Pick Tasks List - Dark Theme")
@Composable
fun PickTasksScreenDarkPreview() {
    MyPrinterAppTheme(darkTheme = true) { // Оберните в вашу тему
        PickTasksScreen(
            tasks = generatePreviewTasks(),
            onOpenTask = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Pick Tasks List - Empty")
@Composable
fun PickTasksScreenEmptyPreview() {
    MyPrinterAppTheme {
        PickTasksScreen(
            tasks = emptyList(),
            onOpenTask = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Single Pick Task Item")
@Composable
fun PickTaskItemPreview() {
    MyPrinterAppTheme {
        Surface(modifier = Modifier.padding(8.dp)) {
            PickTaskItem(
                task = generatePreviewTasks().first {it.status == TaskStatus.IN_PROGRESS },
                onOpenTask = {}
            )
        }
    }
}