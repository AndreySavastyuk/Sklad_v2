package com.example.myprinterapp.ui.pick

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.data.DetailStatus
import com.example.myprinterapp.data.PickDetail
import com.example.myprinterapp.data.PickTask
import com.example.myprinterapp.data.TaskStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickDetailsEnhanced(
    task: PickTask?,
    showQtyDialogFor: PickDetail?,
    onShowQtyDialog: (PickDetail) -> Unit,
    onDismissQtyDialog: () -> Unit,
    onSubmitQty: (detailId: Int, quantity: Int) -> Unit,
    onScanAnyCode: (String) -> Unit,
    onBack: () -> Unit,
    onPrintLabel: (PickDetail) -> Unit = {},
    onCompleteTask: () -> Unit = {},
    scannedQr: String?
) {
    val focusManager = LocalFocusManager.current
    var showCompletionDialog by remember { mutableStateOf(false) }

    // Проверяем, все ли позиции собраны
    LaunchedEffect(task) {
        if (task != null && task.completionPercentage >= 100 && task.status != TaskStatus.COMPLETED) {
            showCompletionDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Задание №${task?.id ?: ""}",
                            fontSize = 20.sp,
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
                    // Быстрое сканирование
                    IconButton(onClick = { onScanAnyCode("") }) {
                        BadgedBox(
                            badge = {
                                if (task?.details?.any { it.status == DetailStatus.NOT_STARTED } == true) {
                                    Badge { Text("!") }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.QrCodeScanner,
                                contentDescription = "Сканировать",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = when (task?.status) {
                        TaskStatus.COMPLETED -> Color(0xFF4CAF50)
                        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            )
        },
        bottomBar = {
            if (task != null) {
                TaskBottomBar(
                    task = task,
                    onCompleteTask = onCompleteTask
                )
            }
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Прогресс выполнения с анимацией
            if (task != null) {
                AnimatedTaskProgress(task)
            }

            if (task == null || task.details.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет деталей для отображения.")
                }
            } else {
                // Группировка по статусу
                val groupedDetails = task.details.groupBy { it.status }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Сначала показываем несобранные
                    groupedDetails[DetailStatus.NOT_STARTED]?.let { notStarted ->
                        item {
                            SectionHeader("К сборке", notStarted.size, Icons.Filled.Inventory)
                        }
                        items(notStarted, key = { it.id }) { detail ->
                            AnimatedDetailCard(
                                detail = detail,
                                onManualEnterQtyClick = { onShowQtyDialog(detail) },
                                onPrintLabel = { onPrintLabel(detail) }
                            )
                        }
                    }

                    // Частично собранные
                    groupedDetails[DetailStatus.PARTIAL]?.let { partial ->
                        item {
                            Spacer(Modifier.height(8.dp))
                            SectionHeader("В процессе", partial.size, Icons.Filled.Autorenew, Color(0xFFFF9800))
                        }
                        items(partial, key = { it.id }) { detail ->
                            AnimatedDetailCard(
                                detail = detail,
                                onManualEnterQtyClick = { onShowQtyDialog(detail) },
                                onPrintLabel = { onPrintLabel(detail) }
                            )
                        }
                    }

                    // Собранные
                    groupedDetails[DetailStatus.COMPLETED]?.let { completed ->
                        item {
                            Spacer(Modifier.height(8.dp))
                            SectionHeader("Собрано", completed.size, Icons.Filled.CheckCircle, Color(0xFF4CAF50))
                        }
                        items(completed, key = { it.id }) { detail ->
                            AnimatedDetailCard(
                                detail = detail,
                                onManualEnterQtyClick = { onShowQtyDialog(detail) },
                                onPrintLabel = { onPrintLabel(detail) },
                                isCompleted = true
                            )
                        }
                    }
                }
            }

            // Диалог ввода количества
            showQtyDialogFor?.let { detailToUpdate ->
                EnhancedQuantityDialog(
                    detail = detailToUpdate,
                    onDismiss = onDismissQtyDialog,
                    onSubmit = { qty ->
                        onSubmitQty(detailToUpdate.id, qty)
                        focusManager.clearFocus()
                    }
                )
            }

            // Диалог завершения задания
            if (showCompletionDialog) {
                TaskCompletionDialog(
                    task = task!!,
                    onConfirm = {
                        onCompleteTask()
                        showCompletionDialog = false
                    },
                    onDismiss = { showCompletionDialog = false }
                )
            }
        }
    }
}

@Composable
fun AnimatedTaskProgress(task: PickTask) {
    val animatedProgress by animateFloatAsState(
        targetValue = task.completionPercentage / 100f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                task.completionPercentage >= 100 -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Прогресс выполнения",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        @OptIn(ExperimentalAnimationApi::class)
                        AnimatedContent(
                            targetState = task.completedPositions,
                            transitionSpec = {
                                slideInVertically { it } + fadeIn() with
                                        slideOutVertically { -it } + fadeOut()
                            }
                        ) { count ->
                            Text(
                                "$count",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "из ${task.positionsCount} позиций",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                // Анимированный процент
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 6.dp,
                        color = when {
                            task.completionPercentage >= 100 -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Text(
                        "${task.completionPercentage.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Детальная статистика
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    task.completionPercentage >= 100 -> Color(0xFF4CAF50)
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Собрано товаров: ${task.pickedItems} из ${task.totalItems} шт",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.2f)
        ) {
            Text(
                count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedDetailCard(
    detail: PickDetail,
    onManualEnterQtyClick: () -> Unit,
    onPrintLabel: () -> Unit,
    isCompleted: Boolean = false
) {
    val scale by animateFloatAsState(
        targetValue = if (isCompleted) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onManualEnterQtyClick() },
        colors = CardDefaults.cardColors(
            containerColor = when (detail.status) {
                DetailStatus.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                DetailStatus.PARTIAL -> Color(0xFFFF9800).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = when (detail.status) {
            DetailStatus.COMPLETED -> BorderStroke(2.dp, Color(0xFF4CAF50))
            DetailStatus.PARTIAL -> BorderStroke(1.dp, Color(0xFFFF9800))
            else -> null
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок с ячейкой
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Ячейка - самое важное, крупно
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.animateContentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = detail.location,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Статус
                AnimatedVisibility(
                    visible = detail.status == DetailStatus.COMPLETED,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Выполнено",
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFF4CAF50)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Артикул и название
            Text(
                text = detail.partNumber,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
            )

            Text(
                text = detail.partName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            detail.comment?.let { comment ->
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Количество и действия
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Прогресс количества
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AnimatedContent(
                            targetState = detail.picked,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInVertically { -it } + fadeIn() with
                                            slideOutVertically { it } + fadeOut()
                                } else {
                                    slideInVertically { it } + fadeIn() with
                                            slideOutVertically { -it } + fadeOut()
                                }
                            }
                        ) { picked ->
                            Text(
                                "$picked",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (detail.status) {
                                    DetailStatus.COMPLETED -> Color(0xFF4CAF50)
                                    DetailStatus.PARTIAL -> Color(0xFFFF9800)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        Text(
                            "/ ${detail.quantityToPick} ${detail.unit}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Прогресс-бар
                    LinearProgressIndicator(
                        progress = detail.completionPercentage / 100f,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .width(150.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when (detail.status) {
                            DetailStatus.COMPLETED -> Color(0xFF4CAF50)
                            DetailStatus.PARTIAL -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }

                // Кнопки
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Печать (только если собрано)
                    AnimatedVisibility(
                        visible = detail.picked > 0,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        FilledTonalIconButton(
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

                    // Ввод количества
                    Button(
                        onClick = onManualEnterQtyClick,
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (detail.status) {
                                DetailStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
                                DetailStatus.PARTIAL -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Icon(
                            when (detail.status) {
                                DetailStatus.COMPLETED -> Icons.Filled.Edit
                                DetailStatus.PARTIAL -> Icons.Filled.Add
                                else -> Icons.Filled.PlayArrow
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when (detail.status) {
                                DetailStatus.COMPLETED -> "Изменить"
                                DetailStatus.PARTIAL -> "Добавить"
                                else -> "Собрать"
                            },
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedQuantityDialog(
    detail: PickDetail,
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit
) {
    var quantityInput by remember { mutableStateOf(detail.picked.toString()) }
    var quickMode by remember { mutableStateOf(detail.picked == 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Inventory, null, Modifier.size(28.dp))
                Text("Сборка детали")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Информационная карточка
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Ячейка крупно
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                detail.location,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Divider()

                        // Артикул и название
                        Text(
                            detail.partNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            detail.partName,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Статус
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Необходимо:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                "${detail.quantityToPick} ${detail.unit}",
                                fontWeight = FontWeight.Bold
                            )
                            if (detail.picked > 0) {
                                Text("•", Modifier.padding(horizontal = 4.dp))
                                Text(
                                    "Собрано:",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    "${detail.picked} ${detail.unit}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }

                // Режим быстрого выбора
                if (quickMode && detail.quantityToPick <= 10) {
                    Text(
                        "Быстрый выбор количества:",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..minOf(detail.quantityToPick, 5)).forEach { qty ->
                            OutlinedButton(
                                onClick = {
                                    quantityInput = qty.toString()
                                    onSubmit(qty)
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(
                                    1.dp,
                                    if (qty == detail.quantityToPick)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text(
                                    qty.toString(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (detail.quantityToPick > 5) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (6..minOf(detail.quantityToPick, 10)).forEach { qty ->
                                OutlinedButton(
                                    onClick = {
                                        quantityInput = qty.toString()
                                        onSubmit(qty)
                                    },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (qty == detail.quantityToPick)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Text(
                                        qty.toString(),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            // Заполнители для выравнивания
                            if (detail.quantityToPick < 10) {
                                repeat(10 - detail.quantityToPick) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { quickMode = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Ввести вручную")
                    }
                } else {
                    // Ручной ввод
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { new ->
                            if (new.all { it.isDigit() } || new.isEmpty()) {
                                quantityInput = new
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
                        if (detail.picked < detail.quantityToPick) {
                            AssistChip(
                                onClick = {
                                    quantityInput = (detail.quantityToPick - detail.picked).toString()
                                },
                                label = { Text("Остаток") }
                            )
                        }
                        AssistChip(
                            onClick = { quantityInput = detail.quantityToPick.toString() },
                            label = { Text("Всё") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityInput.toIntOrNull() ?: 0
                    onSubmit(qty)
                },
                enabled = quantityInput.isNotEmpty()
            ) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(4.dp))
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

@Composable
fun TaskBottomBar(
    task: PickTask,
    onCompleteTask: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp
    ) {
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
                    "Осталось собрать:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val remaining = task.details.count { it.status != DetailStatus.COMPLETED }
                Text(
                    "$remaining позиций",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Кнопка завершения
            AnimatedVisibility(
                visible = task.completionPercentage >= 100 && task.status != TaskStatus.COMPLETED,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Button(
                    onClick = onCompleteTask,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Filled.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Завершить задание")
                }
            }
        }
    }
}

@Composable
fun TaskCompletionDialog(
    task: PickTask,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF4CAF50)
            )
        },
        title = {
            Text(
                "Задание выполнено!",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Все позиции собраны",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Задание №${task.id}",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${task.positionsCount} позиций • ${task.totalItems} товаров",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Text(
                    "Завершить задание и изменить статус на \"Выполнено\"?",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Завершить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Продолжить работу")
            }
        }
    )
}