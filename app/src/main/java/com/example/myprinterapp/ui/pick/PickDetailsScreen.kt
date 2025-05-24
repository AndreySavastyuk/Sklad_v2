package com.example.myprinterapp.ui.pick

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.data.PickDetail
import com.example.myprinterapp.data.PickTask
import androidx.compose.ui.tooling.preview.Preview
import com.example.myprinterapp.data.TaskStatus
import com.example.myprinterapp.data.Priority

// Определение пользовательских иконок для случаев, когда стандартные недоступны
object CustomIcons {
    val Precision = Icons.Filled.Build  // Замена недоступной иконки Precision на похожую Build
    val InventoryOff = Icons.Filled.Inventory  // Замена недоступной иконки на базовую
}

// Цветовая палитра для машиностроения
object IndustrialColors {
    val MetalBlue = Color(0xFF1E3A8A)
    val SteelGray = Color(0xFF64748B)
    val MachineGreen = Color(0xFF059669)
    val WarningOrange = Color(0xFFEA580C)
    val CompletedGreen = Color(0xFF16A34A)
    val TechnicalBlue = Color(0xFF0284C7)
    val PrecisionPurple = Color(0xFF7C3AED)
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
                        if (task != null) "Задание №${extractTaskNumber(task.id)}" else "Задание не найдено",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onScanAnyCode("") },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(IndustrialColors.TechnicalBlue, IndustrialColors.PrecisionPurple)
                                )
                            )
                    ) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = "Сканировать",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = IndustrialColors.MetalBlue
                )
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            when {
                task == null -> {
                    EmptyStateView(
                        title = "Задание не найдено",
                        subtitle = "Проверьте правильность выбора задания",
                        icon = Icons.Filled.ErrorOutline,
                        onBack = onBack
                    )
                }

                task.details.isEmpty() -> {
                    EmptyStateView(
                        title = "Нет деталей в задании №${extractTaskNumber(task.id)}",
                        subtitle = "Возможно, задание еще не загружено или произошла ошибка",
                        icon = CustomIcons.InventoryOff,
                        onBack = onBack
                    )
                }

                else -> {
                    // Информация о задании
                    TaskInfoHeader(task = task)

                    // Список деталей
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(task.details, key = { it.id }) { detail ->
                            IndustrialPickDetailItem(
                                detail = detail,
                                onManualEnterQtyClick = { onShowQtyDialog(detail) }
                            )
                        }
                    }
                }
            }

            // Диалог ввода количества
            showQtyDialogFor?.let { detailToUpdate ->
                IndustrialQtyDialog(
                    detail = detailToUpdate,
                    onDismiss = onDismissQtyDialog,
                    onSubmit = { qty ->
                        onSubmitQty(detailToUpdate.id, qty)
                        onDismissQtyDialog()
                    }
                )
            }
        }
    }
}

@Composable
private fun TaskInfoHeader(task: PickTask) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок с градиентом
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(IndustrialColors.MetalBlue, IndustrialColors.TechnicalBlue)
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            "ИНФОРМАЦИЯ О ЗАДАНИИ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "№${extractTaskNumber(task.id)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Icon(
                        CustomIcons.Precision,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Информация в две колонки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Левая колонка
                Column(modifier = Modifier.weight(1f)) {
                    InfoItem(
                        icon = Icons.Filled.CalendarToday,
                        label = "Дата создания",
                        value = formatDate(task.date),
                        color = IndustrialColors.TechnicalBlue
                    )

                    if (task.customer != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoItem(
                            icon = Icons.Filled.Business,
                            label = "Заказчик",
                            value = task.customer,
                            color = IndustrialColors.MachineGreen
                        )
                    }

                    if (task.deadline != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoItem(
                            icon = Icons.Filled.Schedule,
                            label = "Срок сдачи",
                            value = formatDate(task.deadline),
                            color = IndustrialColors.WarningOrange
                        )
                    }
                }

                // Правая колонка
                Column(modifier = Modifier.weight(1f)) {
                    InfoItem(
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        label = "Статус",
                        value = task.status.toRussianString(),
                        color = getTaskStatusColor(task.status)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    InfoItem(
                        icon = Icons.Filled.Inventory,
                        label = "Позиций",
                        value = "${task.details.size} шт",
                        color = IndustrialColors.SteelGray
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    val progress = if (task.details.isNotEmpty()) {
                        task.details.sumOf { it.picked } * 100 / task.details.sumOf { it.quantityToPick }
                    } else 0
                    InfoItem(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        label = "Прогресс",
                        value = "$progress%",
                        color = if (progress >= 100) IndustrialColors.CompletedGreen else IndustrialColors.TechnicalBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = color
            )
        }
    }
}

@Composable
private fun IndustrialPickDetailItem(
    detail: PickDetail,
    onManualEnterQtyClick: () -> Unit
) {
    val completionRatio = if (detail.quantityToPick > 0) {
        detail.picked.toFloat() / detail.quantityToPick.toFloat()
    } else 0f

    val statusColor = when {
        completionRatio >= 1f -> IndustrialColors.CompletedGreen
        completionRatio > 0f -> IndustrialColors.WarningOrange
        else -> IndustrialColors.SteelGray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column {
            // Полоса статуса
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            0f to statusColor.copy(alpha = 0.3f),
                            completionRatio to statusColor,
                            1f to statusColor.copy(alpha = 0.1f)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левая часть - основная информация
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Обозначение чертежа - ГЛАВНАЯ информация
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Engineering,
                            contentDescription = null,
                            tint = IndustrialColors.MetalBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = detail.partNumber,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = IndustrialColors.MetalBlue
                        )
                    }

                    // Название детали
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            CustomIcons.Precision,
                            contentDescription = null,
                            tint = IndustrialColors.SteelGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = detail.partName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    // Количество
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = IndustrialColors.TechnicalBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "План: ${detail.quantityToPick}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = IndustrialColors.TechnicalBlue
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Done,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "Факт: ${detail.picked}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }

                // Центральная часть - Ячейка хранения
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .widthIn(min = 85.dp)
                        .heightIn(min = 70.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = IndustrialColors.MetalBlue.copy(alpha = 0.1f),
                    border = BorderStroke(2.dp, IndustrialColors.MetalBlue)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Warehouse,
                                contentDescription = null,
                                tint = IndustrialColors.MetalBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "ЯЧЕЙКА",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndustrialColors.MetalBlue
                            )
                            Text(
                                text = detail.location,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = IndustrialColors.MetalBlue,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Правая часть - Прогресс и действия
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Круговой прогресс
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(45.dp),
                            strokeWidth = 4.dp,
                            color = statusColor,
                            trackColor = statusColor.copy(alpha = 0.2f),
                            progress = { completionRatio }
                        )
                        Text(
                            "${(completionRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = statusColor
                        )
                    }

                    // Кнопка ввода
                    Button(
                        onClick = onManualEnterQtyClick,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 80.dp, minHeight = 36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = statusColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "ВВОД",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndustrialQtyDialog(
    detail: PickDetail,
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit
) {
    var quantityInput by remember { mutableStateOf(detail.picked.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    CustomIcons.Precision,
                    contentDescription = null,
                    tint = IndustrialColors.MetalBlue,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "Учет изготовления",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = IndustrialColors.MetalBlue
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Информация о детали
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = IndustrialColors.MetalBlue.copy(alpha = 0.05f)
                    ),
                    border = BorderStroke(1.dp, IndustrialColors.MetalBlue.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Engineering,
                                contentDescription = null,
                                tint = IndustrialColors.MetalBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Обозначение: ${detail.partNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IndustrialColors.MetalBlue
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                CustomIcons.Precision,
                                contentDescription = null,
                                tint = IndustrialColors.SteelGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Наименование: ${detail.partName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Ячейка хранения
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = IndustrialColors.MetalBlue.copy(alpha = 0.1f),
                    border = BorderStroke(2.dp, IndustrialColors.MetalBlue)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Warehouse,
                                contentDescription = null,
                                tint = IndustrialColors.MetalBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "ЯЧЕЙКА ХРАНЕНИЯ",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = IndustrialColors.MetalBlue
                            )
                            Text(
                                text = detail.location,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                color = IndustrialColors.MetalBlue
                            )
                        }
                    }
                }

                // Информация о количестве
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "ПЛАН",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IndustrialColors.TechnicalBlue
                        )
                        Text(
                            "${detail.quantityToPick}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = IndustrialColors.TechnicalBlue
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "ФАКТ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IndustrialColors.CompletedGreen
                        )
                        Text(
                            "${detail.picked}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = IndustrialColors.CompletedGreen
                        )
                    }
                }

                // Поле ввода количества
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { new ->
                        if (new.all { it.isDigit() } || new.isEmpty()) {
                            quantityInput = new
                        }
                    },
                    label = {
                        Text(
                            "Изготовлено деталей",
                            fontSize = 16.sp,
                            color = IndustrialColors.MetalBlue
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Done,
                            contentDescription = null,
                            tint = IndustrialColors.CompletedGreen
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = IndustrialColors.MetalBlue
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndustrialColors.MetalBlue,
                        focusedLabelColor = IndustrialColors.MetalBlue
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val enteredQty = quantityInput.toIntOrNull() ?: 0
                    onSubmit(enteredQty)
                },
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IndustrialColors.CompletedGreen
                )
            ) {
                Icon(
                    Icons.Filled.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("СОХРАНИТЬ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("ОТМЕНА", fontSize = 16.sp, color = IndustrialColors.SteelGray)
            }
        }
    )
}

@Composable
private fun EmptyStateView(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = IndustrialColors.SteelGray
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = IndustrialColors.MetalBlue,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IndustrialColors.MetalBlue
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("НАЗАД К СПИСКУ")
            }
        }
    }
}

// Функция для отображения статуса на русском языке, так как метод из класса не находится
private fun TaskStatus.toRussianString(): String {
    return when (this) {
        TaskStatus.NEW -> "Новое"
        TaskStatus.IN_PROGRESS -> "В работе"
        TaskStatus.COMPLETED -> "Завершено"
        TaskStatus.CANCELLED -> "Отменено"
        TaskStatus.PAUSED -> "Приостановлено"
        TaskStatus.VERIFIED -> "Проверено"
    }
}

// Вспомогательные функции
private fun extractTaskNumber(taskId: String): String {
    return taskId.replace("ZADANIE-", "")
}

private fun formatDate(dateString: String): String {
    return try {
        val date = java.time.LocalDate.parse(dateString)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("ru"))
        date.format(formatter)
    } catch (e: Exception) {
        // e не используется, но оставляем для обработки ошибки
        dateString
    }
}

// Единственная версия функции getStatusColor
private fun getTaskStatusColor(status: TaskStatus): Color {
    return when (status) {
        TaskStatus.NEW -> IndustrialColors.TechnicalBlue
        TaskStatus.IN_PROGRESS -> IndustrialColors.WarningOrange
        TaskStatus.COMPLETED -> IndustrialColors.CompletedGreen
        TaskStatus.CANCELLED -> Color(0xFFDC2626)
        TaskStatus.PAUSED -> IndustrialColors.SteelGray
        TaskStatus.VERIFIED -> IndustrialColors.PrecisionPurple
    }
}

// Preview с реалистичными данными машиностроения
@Preview(showBackground = true,
    widthDp = 800,
    heightDp = 1340)
@Composable
fun PickDetailsScreenPreview() {
    val testDetails = listOf(
        PickDetail(1, "НЗ.КШ.040.25.001", "Вал приводной", 10, "A45", 5),
        PickDetail(2, "НЗ.КШ.040.25.002", "Втулка направляющая", 5, "B12", 5),
        PickDetail(3, "НЗ.КШ.040.25.003", "Шестерня коническая", 12, "C33", 0)
    )

    val testTask = PickTask(
        id = "ZADANIE-001",
        date = "2025-05-30",
        description = "Изготовление деталей коробки передач",
        status = TaskStatus.IN_PROGRESS,
        details = testDetails,
        priority = Priority.HIGH,
        customer = "ПАО КАМАЗ",
        deadline = "2025-06-15"
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

