package com.example.myprinterapp.ui.demo

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myprinterapp.scanner.BleConnectionState
import com.example.myprinterapp.scanner.NewlandBleService
import com.example.myprinterapp.scanner.ScannedData
import com.example.myprinterapp.ui.components.BlePairingDialog
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerTestDemo(
    onBack: () -> Unit,
    viewModel: ScannerTestViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val pairingQr by viewModel.pairingQr.collectAsState()
    val lastScan by viewModel.lastScan.collectAsState()
    val scanHistory by viewModel.scanHistory.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()

    var showPairingDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Тестирование BLE сканера",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                    // Индикатор состояния
                    ConnectionStatusChip(connectionState)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Информация о подключении
            item {
                ConnectionInfoCard(
                    connectionState = connectionState,
                    deviceInfo = deviceInfo,
                    onConnect = { showPairingDialog = true },
                    onDisconnect = { viewModel.disconnect() }
                )
            }

            // Последнее сканирование
            lastScan?.let { scan ->
                item {
                    LastScanCard(scan)
                }
            }

            // Тестовая зона
            if (connectionState == BleConnectionState.CONNECTED) {
                item {
                    TestZoneCard()
                }
            }

            // История сканирований
            if (scanHistory.isNotEmpty()) {
                item {
                    Text(
                        "История сканирований",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(scanHistory) { scan ->
                    ScanHistoryItem(scan)
                }
            }

            // Кнопка очистки истории
            if (scanHistory.isNotEmpty()) {
                item {
                    Button(
                        onClick = { viewModel.clearHistory() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Filled.DeleteSweep, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Очистить историю")
                    }
                }
            }
        }
    }

    // Диалог сопряжения
    if (showPairingDialog) {
        BlePairingDialog(
            connectionState = connectionState,
            qrBitmap = pairingQr,
            onDismiss = {
                showPairingDialog = false
                if (connectionState == BleConnectionState.PAIRING) {
                    viewModel.stopPairing()
                }
            },
            onStartPairing = { viewModel.startPairing() },
            onStopPairing = { viewModel.stopPairing() }
        )
    }
}

@Composable
private fun ConnectionInfoCard(
    connectionState: BleConnectionState,
    deviceInfo: DeviceInfo?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                BleConnectionState.CONNECTED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                BleConnectionState.CONNECTING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = when (connectionState) {
                        BleConnectionState.CONNECTED -> Color(0xFF4CAF50)
                        BleConnectionState.CONNECTING -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Состояние BLE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (connectionState) {
                            BleConnectionState.DISCONNECTED -> "Не подключен"
                            BleConnectionState.PAIRING -> "Сопряжение..."
                            BleConnectionState.CONNECTING -> "Подключение..."
                            BleConnectionState.CONNECTED -> "Подключен"
                            BleConnectionState.ERROR -> "Ошибка"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (connectionState) {
                            BleConnectionState.CONNECTED -> Color(0xFF4CAF50)
                            BleConnectionState.ERROR -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (deviceInfo != null) {
                Divider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Устройство:", fontWeight = FontWeight.Medium)
                    Text(deviceInfo.name)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("MAC адрес:", fontWeight = FontWeight.Medium)
                    Text(
                        deviceInfo.address,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            }

            // Кнопки действий
            when (connectionState) {
                BleConnectionState.DISCONNECTED -> {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.BluetoothSearching, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Подключить сканер")
                    }
                }
                BleConnectionState.CONNECTED -> {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.BluetoothDisabled, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Отключить")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun LastScanCard(scan: ScannedData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
        ),
        border = BorderStroke(2.dp, Color(0xFF2196F3))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF2196F3)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Последнее сканирование",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    scan.data,
                    modifier = Modifier.padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(scan.timestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Проверка на кириллицу
                if (scan.data.any { it in '\u0400'..'\u04FF' }) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Кириллица",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestZoneCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Scanner,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Тестовая зона",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "Сканер готов к работе. Наведите на любой QR-код и нажмите кнопку сканирования.",
                style = MaterialTheme.typography.bodyMedium
            )

            // Примеры QR-кодов для тестирования
            Text(
                "Примеры данных для тестирования:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TestDataExample("Латиница", "test=2024/001=PART-123=Test Part")
                TestDataExample("Кириллица", "тест=2024/001=ДЕТАЛЬ-123=Тестовая деталь")
                TestDataExample("Смешанный", "order=2024/001=PART-123=Деталь тестовая")
            }
        }
    }
}

@Composable
private fun TestDataExample(label: String, data: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "• $label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.widthIn(min = 80.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            data,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ScanHistoryItem(scan: ScannedData) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.QrCode,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    scan.data,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(scan.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusChip(state: BleConnectionState) {
    val (icon, text, color) = when (state) {
        BleConnectionState.DISCONNECTED -> Triple(
            Icons.Filled.BluetoothDisabled,
            "Отключен",
            MaterialTheme.colorScheme.error
        )
        BleConnectionState.PAIRING -> Triple(
            Icons.Filled.QrCode,
            "Сопряжение",
            MaterialTheme.colorScheme.primary
        )
        BleConnectionState.CONNECTING -> Triple(
            Icons.Filled.Sync,
            "Подключение",
            MaterialTheme.colorScheme.primary
        )
        BleConnectionState.CONNECTED -> Triple(
            Icons.Filled.BluetoothConnected,
            "Подключен",
            Color(0xFF4CAF50)
        )
        BleConnectionState.ERROR -> Triple(
            Icons.Filled.Error,
            "Ошибка",
            MaterialTheme.colorScheme.error
        )
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

// ViewModel для тестового экрана
data class DeviceInfo(
    val name: String,
    val address: String
)