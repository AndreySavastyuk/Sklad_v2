package com.example.myprinterapp.ui.pick

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.data.PickTask
import com.example.myprinterapp.data.TaskStatus
import com.example.myprinterapp.data.toRussianString
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.padding


@Composable
fun TaskTransferDialog(
    tasks: List<PickTask>,
    onDismiss: () -> Unit,
    onTasksSelected: (List<PickTask>) -> Unit
) {
    var selectedTasks by remember { mutableStateOf(setOf<String>()) }
    var showQrCode by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()

    if (!showQrCode) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.SendToMobile,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Передача заданий на планшет", fontSize = 20.sp)
                }
            },
            text = {
                Column {
                    Text(
                        "Выберите задания для передачи:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tasks.filter { it.status != TaskStatus.COMPLETED }) { task ->
                            TaskSelectionItem(
                                task = task,
                                isSelected = selectedTasks.contains(task.id),
                                onToggle = {
                                    selectedTasks = if (selectedTasks.contains(task.id)) {
                                        selectedTasks - task.id
                                    } else {
                                        selectedTasks + task.id
                                    }
                                }
                            )
                        }
                    }

                    if (selectedTasks.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Выбрано заданий: ${selectedTasks.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val selectedTasksList = tasks.filter { selectedTasks.contains(it.id) }
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                qrBitmap = generateTransferQrCode(selectedTasksList)
                            }
                            showQrCode = true
                        }
                    },
                    enabled = selectedTasks.isNotEmpty()
                ) {
                    Icon(Icons.Filled.QrCode2, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Создать QR-код")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        )
    } else {
        // Показываем QR-код
        AlertDialog(
            onDismissRequest = {
                showQrCode = false
                onDismiss()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("QR-код для передачи", fontSize = 20.sp)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    qrBitmap?.let { bitmap ->
                        Card(
                            modifier = Modifier
                                .size(300.dp)
                                .padding(8.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR код для передачи заданий",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Отсканируйте этот QR-код на планшете\nдля получения заданий",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showQrCode = false
                    onDismiss()
                }) {
                    Text("Готово")
                }
            }
        )
    }
}

@Composable
private fun TaskSelectionItem(
    task: PickTask,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Задание №${task.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        getStatusIcon(task.status),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = getStatusColor(task.status)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        task.status.toRussianString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = getStatusColor(task.status)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "${task.pickedItems}/${task.totalItems} шт",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Прогресс
            CircularProgressIndicator(
                progress = task.completionPercentage / 100f,
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp,
                color = when {
                    task.completionPercentage >= 100 -> Color(0xFF4CAF50)
                    task.completionPercentage > 0 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        }
    }
}

// Функция генерации QR-кода для передачи заданий
private fun generateTransferQrCode(tasks: List<PickTask>): Bitmap {
    val transferData = TaskTransferPayload(
        tasks = tasks.map { task ->
            TaskTransferInfo(
                id = task.id,
                date = task.date,
                description = task.description,
                status = task.status.name,
                priority = task.priority.name,
                customer = task.customer,
                deadline = task.deadline,
                details = task.details.map { detail ->
                    DetailTransferInfo(
                        id = detail.id,
                        partNumber = detail.partNumber,
                        partName = detail.partName,
                        quantityToPick = detail.quantityToPick,
                        location = detail.location,
                        picked = detail.picked,
                        unit = detail.unit,
                        comment = detail.comment
                    )
                }
            )
        },
        transferredAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        deviceId = "TERMINAL-001" // ID терминала
    )

    val gson = Gson()
    val jsonData = gson.toJson(transferData)

    // Генерируем QR-код
    val writer = QRCodeWriter()
    val hints = hashMapOf<EncodeHintType, Any>()
    hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
    hints[EncodeHintType.MARGIN] = 1
    hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M

    val bitMatrix = writer.encode(jsonData, BarcodeFormat.QR_CODE, 512, 512, hints)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }

    return bitmap
}

// Модели данных для передачи (временное решение без Serializable)
data class TaskTransferPayload(
    val tasks: List<TaskTransferInfo>,
    val transferredAt: String,
    val deviceId: String
)

data class TaskTransferInfo(
    val id: String,
    val date: String,
    val description: String,
    val status: String,
    val priority: String,
    val customer: String?,
    val deadline: String?,
    val details: List<DetailTransferInfo>
)

data class DetailTransferInfo(
    val id: Int,
    val partNumber: String,
    val partName: String,
    val quantityToPick: Int,
    val location: String,
    val picked: Int,
    val unit: String,
    val comment: String?
)

