package com.example.myprinterapp.ui.pick

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.data.PickDetail
import com.example.myprinterapp.data.PickTask
import com.example.myprinterapp.data.TaskStatus
import com.example.myprinterapp.data.Priority
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

// Функция для форматирования даты в формат "30 мая 2025"
private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
        date.format(formatter)
    } catch (e: Exception) {
        dateString // Возвращаем исходную строку если не удалось распарсить
    }
}

// Функция для извлечения номера задания
private fun extractTaskNumber(taskId: String): String {
    return taskId.replace("ZADANIE-", "")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickDetailsScreen(
    task: PickTask?,
    showQtyDialogFor: PickDetail?,
    onShowQtyDialog: (PickDetail) -> Unit,
    onDismissQtyDialog: () -> Unit,
    onSubmitQty: (detailId: Int, quantity: Int) -> Unit,
    onScanAnyCode: (String) -> Unit,
    onBack: () -> Unit,
    onSubmitPickedQty: (detailId: Int, quantity: Int) -> Unit,
    scannedQr: String?
) {
    // Отладочная информация
    LaunchedEffect(task) {
        println("Debug: PickDetailsScreen - task received: $task")
        println("Debug: PickDetailsScreen - task details: ${task?.details}")
        println("Debug: PickDetailsScreen - details count: ${task?.details?.size}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (task != null) "Детали задания №${extractTaskNumber(task.id)}" else "Задание не найдено",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { onScanAnyCode("") }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Сканировать")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                task == null -> {
                    // Задание не найдено
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Задание не найдено",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = onBack) {
                                Text("Вернуться к списку")
                            }
                        }
                    }
                }

                task.details.isEmpty() -> {
                    // Нет деталей в задании
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "В задании №${extractTaskNumber(task.id)} нет деталей",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                "Возможно, задание еще не загружено или произошла ошибка",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onBack) {
                                Text("Вернуться к списку")
                            }
                        }
                    }
                }

                else -> {
                    // Отображаем информацию о задании - новый дизайн с 2 колонками
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Информация о задании",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            // Разбиваем на 2 колонки
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // Левая колонка
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Дата: ${formatDate(task.date)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontSize = 18.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Assignment,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Заказ: 2025/005",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Inventory,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Позиций: ${task.details.size}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontSize = 18.sp
                                        )
                                    }
                                }

                                // Правая колонка - Прогресс
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val progress = if (task.details.isNotEmpty()) {
                                        task.details.sumOf { it.picked } * 100 / task.details.sumOf { it.quantityToPick }
                                    } else 0

                                    // Круговой прогресс
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(80.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = progress / 100f,
                                            modifier = Modifier.fillMaxSize(),
                                            strokeWidth = 8.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "$progress%",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        "Прогресс",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Описание на всю ширину
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Описание: ${task.description}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }

                    // Список деталей - сортируем: сначала не выполненные, потом выполненные
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sortedDetails = task.details.sortedBy { detail ->
                            // Выполненные идут в конец (true = 1, false = 0)
                            detail.picked >= detail.quantityToPick
                        }

                        items(sortedDetails, key = { it.id }) { detail ->
                            PickDetailItem(
                                detail = detail,
                                onManualEnterQtyClick = { onShowQtyDialog(detail) }
                            )
                        }
                    }
                }
            }

            // Диалог ввода количества
            showQtyDialogFor?.let { detailToUpdate ->
                var quantityInput by remember { mutableStateOf(detailToUpdate.picked.toString()) }
                AlertDialog(
                    onDismissRequest = onDismissQtyDialog,
                    title = {
                        Text(
                            "Собрать: ${detailToUpdate.partName}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                "Номер детали: ${detailToUpdate.partNumber}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Text(
                                        text = detailToUpdate.location,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 45.sp, // Размер 45sp как требовалось
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Нужно собрать: ${detailToUpdate.quantityToPick} шт",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp
                            )
                            Text(
                                "Уже собрано: ${detailToUpdate.picked} шт",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            OutlinedTextField(
                                value = quantityInput,
                                onValueChange = { new ->
                                    if (new.all { it.isDigit() } || new.isEmpty()) {
                                        quantityInput = new
                                    }
                                },
                                label = { Text("Собранное количество", fontSize = 18.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val enteredQty = quantityInput.toIntOrNull() ?: 0
                                onSubmitQty(detailToUpdate.id, enteredQty)
                                onDismissQtyDialog()
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("СОХРАНИТЬ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = onDismissQtyDialog,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("ОТМЕНА", fontSize = 16.sp)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PickDetailItem(
    detail: PickDetail,
    onManualEnterQtyClick: () -> Unit
) {
    val isCompleted = detail.picked >= detail.quantityToPick

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                // Зеленый фон для выполненных заданий
                androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isCompleted) {
            BorderStroke(2.dp, androidx.compose.ui.graphics.Color(0xFF4CAF50))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .heightIn(min = 120.dp), // Увеличенная высота для разрешения 800х1340
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Левая часть - основная информация
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Номер детали - ГЛАВНАЯ информация
                Text(
                    text = detail.partNumber,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp, // Увеличен для лучшей видимости
                    color = if (isCompleted) {
                        androidx.compose.ui.graphics.Color(0xFF2E7D32) // Темно-зеленый для выполненных
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                // Название детали - увеличенный шрифт
                Text(
                    text = detail.partName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 22.sp, // Увеличен с 18sp до 22sp
                    color = if (isCompleted) {
                        androidx.compose.ui.graphics.Color(0xFF388E3C) // Средне-зеленый для выполненных
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                // Количество
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "Нужно: ${detail.quantityToPick}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = if (isCompleted) {
                            androidx.compose.ui.graphics.Color(0xFF388E3C)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        "Собрано: ${detail.picked}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) {
                            androidx.compose.ui.graphics.Color(0xFF2E7D32) // Темно-зеленый
                        } else if (detail.picked > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Центральная часть - только номер ячейки (убран значок и надпись "ЯЧЕЙКА")
            Surface(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .size(100.dp), // Размер контейнера
                shape = MaterialTheme.shapes.medium,
                color = if (isCompleted) {
                    androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                border = BorderStroke(
                    2.dp,
                    if (isCompleted) {
                        androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = detail.location,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        fontSize = 45.sp, // Размер 45sp как требовалось
                        color = if (isCompleted) {
                            androidx.compose.ui.graphics.Color(0xFF2E7D32)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Правая часть - Прогресс/Галочка и кнопка
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isCompleted) {
                    // Большая зеленая галочка для выполненных заданий
                    Surface(
                        modifier = Modifier.size(65.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Выполнено",
                                modifier = Modifier.size(40.dp),
                                tint = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }

                    Text(
                        "ГОТОВО",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                    )
                } else {
                    // Обычный прогресс для невыполненных заданий
                    val percentage = if (detail.quantityToPick > 0) {
                        (detail.picked * 100 / detail.quantityToPick).coerceIn(0, 100)
                    } else 0

                    CircularProgressIndicator(
                        progress = percentage / 100f,
                        modifier = Modifier.size(65.dp), // Увеличен размер
                        strokeWidth = 7.dp,
                        color = when {
                            percentage > 0 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )

                    Text(
                        "$percentage%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Button(
                    onClick = onManualEnterQtyClick,
                    modifier = Modifier.defaultMinSize(minWidth = 110.dp, minHeight = 52.dp), // Увеличен размер кнопки
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) {
                            androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(
                        if (isCompleted) "ИЗМЕНИТЬ" else "ВВОД",
                        fontSize = 18.sp, // Увеличен шрифт кнопки
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Preview с тестовыми данными - показываем разные состояния выполнения
@Preview(showBackground = true, widthDp = 800, heightDp = 1340)
@Composable
fun PickDetailsScreenPreview() {
    val testDetails = listOf(
        PickDetail(1, "PN-APPLE-01", "Яблоки красные", 10, "A01", 5), // Частично выполнено
        PickDetail(2, "PN-ORANGE-02", "Апельсины сладкие", 8, "A02", 0), // Не начато
        PickDetail(3, "PN-BANANA-03", "Бананы спелые", 12, "B05", 12) // Полностью выполнено
    )

    val testTask = PickTask(
        id = "ZADANIE-001",
        date = "2025-05-30", // Дата будет отформатирована как "30 мая 2025"
        description = "Тестовое задание для демонстрации",
        status = TaskStatus.IN_PROGRESS,
        details = testDetails
    )

    MaterialTheme {
        PickDetailsScreen(
            task = testTask,
            showQtyDialogFor = null,
            onShowQtyDialog = {},
            onDismissQtyDialog = {},
            onSubmitQty = { _, _ -> },
            onScanAnyCode = {},
            onBack = {},
            onSubmitPickedQty = { _, _ -> },
            scannedQr = null
        )
    }
}