package com.example.myprinterapp.ui.pick

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.data.DetailStatus
import com.example.myprinterapp.data.PickDetail
import com.example.myprinterapp.data.PickTask
import com.example.myprinterapp.data.TaskStatus
import com.example.myprinterapp.ui.theme.MyPrinterAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickDetailsScreenImproved(
    task: PickTask?,
    showQtyDialogFor: PickDetail?,
    onShowQtyDialog: (PickDetail) -> Unit,
    onDismissQtyDialog: () -> Unit,
    onSubmitQty: (detailId: Int, quantity: Int) -> Unit,
    onScanAnyCode: (String) -> Unit,
    onBack: () -> Unit,
    onPrintLabel: (PickDetail) -> Unit = {},
    scannedQr: String?
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Задание №${task?.id ?: ""}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        task?.customer?.let {
                            Text(
                                it,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onScanAnyCode("") }) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = "Сканировать",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Прогресс выполнения задания
            if (task != null) {
                TaskProgressCard(task)
            }

            if (task == null || task.details.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет деталей для отображения.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(task.details, key = { it.id }) { detail ->
                        PickDetailCard(
                            detail = detail,
                            onManualEnterQtyClick = { onShowQtyDialog(detail) },
                            onPrintLabel = { onPrintLabel(detail) }
                        )
                    }
                }
            }

            // Диалог ввода количества
            showQtyDialogFor?.let { detailToUpdate ->
                QuantityInputDialog(
                    detail = detailToUpdate,
                    onDismiss = onDismissQtyDialog,
                    onSubmit = { qty ->
                        onSubmitQty(detailToUpdate.id, qty)
                        focusManager.clearFocus()
                    }
                )
            }
        }
    }
}

@Composable
fun TaskProgressCard(task: PickTask) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Прогресс выполнения",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${task.completedPositions}/${task.positionsCount}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        " позиций",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    "${task.pickedItems}/${task.totalItems} товаров",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            // Круговой прогресс
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                CircularProgressIndicator(
                    progress = task.completionPercentage / 100f,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = when {
                        task.completionPercentage >= 100 -> Color(0xFF4CAF50)
                        task.completionPercentage > 0 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                Text(
                    text = "${task.completionPercentage.toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PickDetailCard(
    detail: PickDetail,
    onManualEnterQtyClick: () -> Unit,
    onPrintLabel: () -> Unit
) {
    val statusColor = when (detail.status) {
        DetailStatus.NOT_STARTED -> MaterialTheme.colorScheme.surfaceVariant
        DetailStatus.PARTIAL -> Color(0xFFFF9800)
        DetailStatus.COMPLETED -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onManualEnterQtyClick() },
        border = BorderStroke(
            width = 2.dp,
            color = if (detail.status == DetailStatus.COMPLETED) statusColor else Color.Transparent
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (detail.status == DetailStatus.COMPLETED)
                statusColor.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Ячейка хранения - крупно сверху
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = detail.location,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Статус
                if (detail.status == DetailStatus.COMPLETED) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Выполнено",
                        modifier = Modifier.size(32.dp),
                        tint = statusColor
                    )
                }
            }

            // Артикул детали
            Text(
                text = detail.partNumber,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            // Название детали
            Text(
                text = detail.partName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Количество и кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Прогресс количества
                Column {
                    Text(
                        "Собрано:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${detail.picked}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        Text(
                            " / ${detail.quantityToPick}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            " ${detail.unit}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }

                    // Линейный прогресс
                    LinearProgressIndicator(
                        progress = detail.completionPercentage / 100f,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .width(120.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Кнопки действий
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Кнопка печати этикетки
                    if (detail.picked > 0) {
                        OutlinedIconButton(
                            onClick = onPrintLabel,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Filled.Print,
                                contentDescription = "Печать",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Кнопка ввода количества
                    Button(
                        onClick = onManualEnterQtyClick,
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (detail.status == DetailStatus.COMPLETED)
                                MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (detail.status == DetailStatus.COMPLETED)
                                Icons.Filled.Edit
                            else Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (detail.picked == 0) "Ввод" else "Изменить",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuantityInputDialog(
    detail: PickDetail,
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit
) {
    var quantityInput by remember { mutableStateOf(detail.picked.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Inventory,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                "Укажите количество",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Информация о детали
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row {
                            Text(
                                "Ячейка: ",
                                fontWeight = FontWeight.Bold
                            )
                            Text(detail.location)
                        }
                        Row {
                            Text(
                                "Артикул: ",
                                fontWeight = FontWeight.Bold
                            )
                            Text(detail.partNumber)
                        }
                        Text(
                            detail.partName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Row {
                            Text(
                                "Необходимо: ",
                                fontWeight = FontWeight.Bold
                            )
                            Text("${detail.quantityToPick} ${detail.unit}")
                        }
                    }
                }

                // Поле ввода
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { new ->
                        if (new.all { it.isDigit() } || new.isEmpty()) {
                            quantityInput = new
                            error = null
                        }
                    },
                    label = { Text("Собранное количество") },
                    suffix = { Text(detail.unit) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val qty = quantityInput.toIntOrNull() ?: 0
                            if (qty in 0..detail.quantityToPick) {
                                onSubmit(qty)
                            }
                        }
                    ),
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Быстрые кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { quantityInput = "0" },
                        label = { Text("Сброс") }
                    )
                    AssistChip(
                        onClick = { quantityInput = detail.quantityToPick.toString() },
                        label = { Text("Всё") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val enteredQty = quantityInput.toIntOrNull() ?: 0
                    when {
                        enteredQty < 0 -> error = "Количество не может быть отрицательным"
                        enteredQty > detail.quantityToPick -> error = "Превышает необходимое количество"
                        else -> onSubmit(enteredQty)
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PickDetailsScreenImprovedPreview() {
    MyPrinterAppTheme {
        val task = PickTask(
            id = "001",
            date = "2025-05-23",
            description = "Тестовое задание",
            status = TaskStatus.IN_PROGRESS,
            customer = "ООО Техмаш",
            details = listOf(
                PickDetail(1, "НЗ.КШ.040.25.001-04", "Втулка направляющая", 5, "A12", picked = 3),
                PickDetail(2, "НЗ.КШ.040.25.002-01", "Корпус основной", 2, "B05", picked = 2),
                PickDetail(3, "НЗ.КШ.040.25.003", "Крышка верхняя", 10, "C18", picked = 0)
            )
        )

        PickDetailsScreenImproved(
            task = task,
            showQtyDialogFor = null,
            onShowQtyDialog = {},
            onDismissQtyDialog = {},
            onSubmitQty = { _, _ -> },
            onScanAnyCode = {},
            onBack = {},
            scannedQr = null
        )
    }
}