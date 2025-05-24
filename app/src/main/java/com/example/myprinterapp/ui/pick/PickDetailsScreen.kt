package com.example.myprinterapp.ui.pick

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.data.PickDetail
import com.example.myprinterapp.data.PickTask
import androidx.compose.ui.tooling.preview.Preview
import com.example.myprinterapp.data.TaskStatus

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
                        if (task != null) "Детали задания №${task.id}" else "Задание не найдено",
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
                }
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
                                "В задании №${task.id} нет деталей",
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
                    // Отображаем информацию о задании
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Информация о задании",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Дата: ${task.date}")
                            Text("Описание: ${task.description}")
                            Text("Статус: ${task.status.name}")
                            Text("Позиций: ${task.details.size}")

                            val progress = if (task.details.isNotEmpty()) {
                                task.details.sumOf { it.picked } * 100 / task.details.sumOf { it.quantityToPick }
                            } else 0
                            Text("Прогресс: $progress%")
                        }
                    }

                    // Список деталей
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(task.details, key = { it.id }) { detail ->
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
                    title = { Text("Собрать: ${detailToUpdate.partName}") },
                    text = {
                        Column {
                            Text("Артикул: ${detailToUpdate.partNumber}")
                            Text("Ячейка: ${detailToUpdate.location}")
                            Text("Нужно собрать: ${detailToUpdate.quantityToPick}")
                            Text("Уже собрано: ${detailToUpdate.picked}")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = quantityInput,
                                onValueChange = { new ->
                                    if (new.all { it.isDigit() } || new.isEmpty()) {
                                        quantityInput = new
                                    }
                                },
                                label = { Text("Собранное количество") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val enteredQty = quantityInput.toIntOrNull() ?: 0
                            onSubmitQty(detailToUpdate.id, enteredQty)
                            onDismissQtyDialog()
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismissQtyDialog) {
                            Text("Отмена")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                detail.picked == 0 -> MaterialTheme.colorScheme.surface
                detail.picked < detail.quantityToPick -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = detail.partName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Артикул: ${detail.partNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            "Нужно: ${detail.quantityToPick}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Собрано: ${detail.picked}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (detail.picked >= detail.quantityToPick) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Text(
                    "Ячейка: ${detail.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Процент выполнения
                val percentage = if (detail.quantityToPick > 0) {
                    (detail.picked * 100 / detail.quantityToPick).coerceIn(0, 100)
                } else 0

                CircularProgressIndicator(
                    progress = percentage / 100f,
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 4.dp,
                    color = when {
                        percentage >= 100 -> MaterialTheme.colorScheme.primary
                        percentage > 0 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.outline
                    }
                )

                Text(
                    "$percentage%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = onManualEnterQtyClick,
                    modifier = Modifier.defaultMinSize(minWidth = 80.dp)
                ) {
                    Text("Ввод")
                }
            }
        }
    }
}

// Preview с тестовыми данными
@Preview(showBackground = true)
@Composable
fun PickDetailsScreenPreview() {
    val testDetails = listOf(
        PickDetail(1, "PN-APPLE-01", "Яблоки красные", 10, "Склад A, Ячейка 01", 5),
        PickDetail(2, "PN-ORANGE-02", "Апельсины сладкие", 5, "Склад A, Ячейка 02", 5),
        PickDetail(3, "PN-BANANA-03", "Бананы спелые", 12, "Склад B, Ячейка 05", 0)
    )

    val testTask = PickTask(
        id = "TEST-001",
        date = "2024-07-30",
        description = "Тестовое задание",
        status = TaskStatus.IN_PROGRESS,
        details = testDetails
    )

    MaterialTheme {
        Surface {
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
}