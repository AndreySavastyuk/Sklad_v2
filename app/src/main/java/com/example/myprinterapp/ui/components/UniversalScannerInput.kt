package com.example.myprinterapp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.scanner.NewlandConnectionState
import com.example.myprinterapp.scanner.ScannerState

/**
 * Универсальное поле для работы со сканерами в HID и BLE режимах
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalScannerInput(
    // HID режим
    hidScannerState: ScannerState,
    hidInputValue: String,
    onHidValueChange: (String) -> Unit,
    onHidScanComplete: (String) -> Unit,

    // BLE режим (Newland)
    bleConnectionState: NewlandConnectionState,
    bleDeviceName: String?,
    onShowBlePairing: () -> Unit,

    // Общие параметры
    label: String = "Сканируйте QR-код",
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(ScannerMode.AUTO) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Селектор режима
        ScannerModeSelector(
            selectedMode = selectedMode,
            onModeChange = { selectedMode = it },
            hidConnected = hidScannerState == ScannerState.CONNECTED,
            bleConnected = bleConnectionState == NewlandConnectionState.CONNECTED
        )

        // Поле ввода или кнопка подключения
        AnimatedContent(
            targetState = selectedMode,
            transitionSpec = {
                fadeIn() with fadeOut()
            }
        ) { mode ->
            when (mode) {
                ScannerMode.AUTO -> {
                    // Автоматический выбор на основе подключения
                    when {
                        bleConnectionState == NewlandConnectionState.CONNECTED -> {
                            BleConnectedCard(
                                deviceName = bleDeviceName ?: "Newland Scanner",
                                onReconnect = onShowBlePairing
                            )
                        }
                        hidScannerState == ScannerState.CONNECTED -> {
                            ScannerInputField(
                                value = hidInputValue,
                                onValueChange = onHidValueChange,
                                onScanComplete = onHidScanComplete,
                                label = label,
                                placeholder = "Наведите сканер HR32-BT и нажмите кнопку",
                                isConnected = true,
                                enabled = enabled,
                                autoFocus = true,
                                clearAfterScan = true
                            )
                        }
                        else -> {
                            NoScannerCard(
                                onConnectHid = { selectedMode = ScannerMode.HID },
                                onConnectBle = onShowBlePairing
                            )
                        }
                    }
                }

                ScannerMode.HID -> {
                    ScannerInputField(
                        value = hidInputValue,
                        onValueChange = onHidValueChange,
                        onScanComplete = onHidScanComplete,
                        label = label,
                        placeholder = if (hidScannerState == ScannerState.CONNECTED) {
                            "Наведите сканер HR32-BT и нажмите кнопку"
                        } else {
                            "Подключите сканер в HID режиме"
                        },
                        isConnected = hidScannerState == ScannerState.CONNECTED,
                        enabled = enabled && hidScannerState == ScannerState.CONNECTED,
                        autoFocus = hidScannerState == ScannerState.CONNECTED,
                        clearAfterScan = true
                    )
                }

                ScannerMode.BLE -> {
                    if (bleConnectionState == NewlandConnectionState.CONNECTED) {
                        BleConnectedCard(
                            deviceName = bleDeviceName ?: "Newland Scanner",
                            onReconnect = onShowBlePairing
                        )
                    } else {
                        BleConnectionButton(
                            onClick = onShowBlePairing,
                            enabled = enabled
                        )
                    }
                }
            }
        }

        // Индикатор статуса
        ScannerStatusIndicator(
            hidState = hidScannerState,
            bleState = bleConnectionState,
            selectedMode = selectedMode
        )
    }
}

@Composable
private fun ScannerModeSelector(
    selectedMode: ScannerMode,
    onModeChange: (ScannerMode) -> Unit,
    hidConnected: Boolean,
    bleConnected: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScannerMode.values().forEach { mode ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onModeChange(mode) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        when (mode) {
                            ScannerMode.AUTO -> Icon(Icons.Filled.AutoMode, null, Modifier.size(16.dp))
                            ScannerMode.HID -> Icon(Icons.Filled.Keyboard, null, Modifier.size(16.dp))
                            ScannerMode.BLE -> Icon(Icons.Filled.Bluetooth, null, Modifier.size(16.dp))
                        }
                        Text(mode.displayName)

                        // Индикатор подключения
                        when (mode) {
                            ScannerMode.HID -> if (hidConnected) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    Modifier.size(14.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                            ScannerMode.BLE -> if (bleConnected) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    Modifier.size(14.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                            else -> {}
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NoScannerCard(
    onConnectHid: () -> Unit,
    onConnectBle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                "Сканер не подключен",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                "Выберите способ подключения:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onConnectHid,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Keyboard, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("HID режим")
                }

                Button(
                    onClick = onConnectBle,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Bluetooth, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("BLE режим")
                }
            }

            Text(
                "BLE режим поддерживает кириллицу",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun BleConnectedCard(
    deviceName: String,
    onReconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, Color(0xFF4CAF50))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.BluetoothConnected,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF4CAF50)
                )

                Column {
                    Text(
                        deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "BLE режим • Готов к сканированию",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        "Поддержка кириллицы ✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onReconnect) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Настройки",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BleConnectionButton(
    onClick: () -> Unit,
    enabled: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(Icons.Filled.BluetoothSearching, null, Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text("Подключить сканер по BLE", fontSize = 16.sp)
    }
}

@Composable
private fun ScannerStatusIndicator(
    hidState: ScannerState,
    bleState: NewlandConnectionState,
    selectedMode: ScannerMode
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // HID статус
            if (selectedMode == ScannerMode.AUTO || selectedMode == ScannerMode.HID) {
                StatusChip(
                    icon = Icons.Filled.Keyboard,
                    text = "HID: ${if (hidState == ScannerState.CONNECTED) "Подключен" else "Отключен"}",
                    isConnected = hidState == ScannerState.CONNECTED
                )
            }

            // BLE статус
            if (selectedMode == ScannerMode.AUTO || selectedMode == ScannerMode.BLE) {
                StatusChip(
                    icon = Icons.Filled.Bluetooth,
                    text = "BLE: ${
                        when (bleState) {
                            NewlandConnectionState.CONNECTED -> "Подключен"
                            NewlandConnectionState.CONNECTING -> "Подключение..."
                            else -> "Отключен"
                        }
                    }",
                    isConnected = bleState == NewlandConnectionState.CONNECTED
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isConnected: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isConnected) {
            Color(0xFF4CAF50).copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Режимы работы сканера
enum class ScannerMode(val displayName: String) {
    AUTO("Авто"),
    HID("HID"),
    BLE("BLE")
}