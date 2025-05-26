package com.example.myprinterapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.scanner.BluetoothScannerService
import kotlinx.coroutines.launch

/**
 * Диалог для отладки подключения сканера
 */
@Composable
fun ScannerDebugDialog(
    scannerService: BluetoothScannerService,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val availableScanners by scannerService.availableScanners.collectAsState()
    val connectedScanner by scannerService.connectedScanner.collectAsState()
    val scannerState by scannerService.scannerState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Отладка подключения сканера")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Текущее состояние
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (scannerState) {
                            com.example.myprinterapp.scanner.ScannerState.CONNECTED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Состояние: ${scannerState}",
                            fontWeight = FontWeight.Bold
                        )
                        if (connectedScanner != null) {
                            Text("Подключен: ${connectedScanner?.name}")
                            Text(
                                "MAC: ${connectedScanner?.address}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Список доступных устройств
                Text(
                    "Сопряженные сканеры (${availableScanners.size}):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (availableScanners.isEmpty()) {
                    Text(
                        "Нет сопряженных сканеров",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(availableScanners) { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Devices,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            device.name ?: "Неизвестное устройство",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            device.address,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    val isConnected = scannerService.getDeviceConnectionState(device)
                                    Icon(
                                        if (isConnected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }

                // Кнопка обновления
                Button(
                    onClick = {
                        scope.launch {
                            scannerService.forceCheckConnection()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Обновить состояние")
                }

                // Подсказка
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Убедитесь, что сканер включен и находится в режиме HID",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
fun ScannerDebugPanel(
    modifier: Modifier = Modifier,
    onTestDecoder: () -> Unit = {}
) {
    var rawInput by remember { mutableStateOf("") }
    var debugLog by remember { mutableStateOf(mutableListOf<String>()) }
    val scrollState = rememberScrollState()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Отладка HID POS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Кнопки действий
                Row {
                    IconButton(onClick = {
                        rawInput = ""
                        debugLog.clear()
                    }) {
                        Icon(Icons.Filled.Clear, "Очистить")
                    }
                    IconButton(onClick = onTestDecoder) {
                        Icon(Icons.Filled.PlayArrow, "Тест")
                    }
                }
            }

            Divider()

            // Поле для перехвата ввода
            OutlinedTextField(
                value = rawInput,
                onValueChange = { newValue ->
                    rawInput = newValue
                    if (newValue.isNotEmpty()) {
                        analyzeHidPosInput(newValue, debugLog)
                    }
                },
                label = { Text("Введите или отсканируйте данные") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                maxLines = 3
            )

            // Анализ данных
            if (rawInput.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Анализ данных:", fontWeight = FontWeight.Bold)

                        // Основная информация
                        InfoRow("Длина:", "${rawInput.length} символов")
                        InfoRow("Байты:", "${rawInput.toByteArray().size}")

                        // HEX представление первых байтов
                        val hexBytes = rawInput.toByteArray().take(20)
                            .joinToString(" ") { "%02X".format(it) }
                        InfoRow("HEX:", hexBytes)

                        // Проверка на префиксы HID POS
                        when {
                            rawInput.startsWith("]") -> {
                                InfoRow("AIM ID:", rawInput.take(3), Color(0xFF4CAF50))
                            }
                            rawInput.contains('\u001D') -> {
                                InfoRow("Формат:", "С разделителем GS", Color(0xFF2196F3))
                            }
                        }

                        // Проверка кириллицы
                        val hasCyrillic = rawInput.any { it in '\u0400'..'\u04FF' }
                        InfoRow(
                            "Кириллица:",
                            if (hasCyrillic) "Обнаружена ✓" else "Не обнаружена ✗",
                            if (hasCyrillic) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )

                        // Определение кодировки
                        val encoding = detectHidPosEncoding(rawInput)
                        InfoRow("Кодировка:", encoding)
                    }
                }
            }

            // Лог событий
            if (debugLog.isNotEmpty()) {
                Text("История:", style = MaterialTheme.typography.labelMedium)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .verticalScroll(scrollState)
                    ) {
                        debugLog.takeLast(5).forEach { log ->
                            Text(
                                log,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )
    }
}

private fun analyzeHidPosInput(input: String, log: MutableList<String>) {
    val timestamp = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())

    // Определяем тип данных
    val dataType = when {
        input.startsWith("]") -> "AIM ID: ${input.take(3)}"
        input.contains('\u001D') -> "Code ID формат"
        input.any { it in '\u0400'..'\u04FF' } -> "Кириллица UTF-8"
        else -> "Обычный текст"
    }

    log.add("[$timestamp] $dataType (${input.length} симв.)")
}

private fun detectHidPosEncoding(input: String): String {
    return when {
        input.all { it.code < 128 } -> "ASCII"
        input.any { it in '\u0400'..'\u04FF' } -> "UTF-8 (корректно)"
        input.contains("Ð") || input.contains("Ñ") -> "Windows-1251→UTF-8"
        input.any { it.code in 128..255 } -> "Extended ASCII"
        else -> "Неизвестно"
    }
}