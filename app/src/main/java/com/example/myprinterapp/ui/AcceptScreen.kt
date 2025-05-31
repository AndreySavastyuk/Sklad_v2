package com.example.myprinterapp.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LabelImportant
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myprinterapp.printer.ConnectionState
import com.example.myprinterapp.scanner.BleConnectionState
import com.example.myprinterapp.scanner.ScannerState
import com.example.myprinterapp.ui.components.EditAcceptanceDialog
import com.example.myprinterapp.ui.components.NewlandPairingDialog
import com.example.myprinterapp.ui.components.ScannerInputField
import com.example.myprinterapp.ui.theme.WarmYellow
import com.example.myprinterapp.viewmodel.AcceptUiState
import com.example.myprinterapp.viewmodel.AcceptViewModel
import com.example.myprinterapp.viewmodel.NewlandScannerViewModel
import kotlinx.coroutines.delay

// Утилита для затемнения цвета
private fun Color.darker(factor: Float = 0.8f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}

data class ParsedQrData(
    val key: String,
    val value: String,
    val icon: ImageVector
)

fun parseFixedQrValue(scannedValue: String?): List<ParsedQrData> {
    if (scannedValue.isNullOrBlank()) return emptyList()
    val parts = scannedValue.split('=')
    if (parts.size != 4) return emptyList()

    return listOf(
        ParsedQrData("Номер маршрутной карты", parts[0], Icons.Filled.ConfirmationNumber),
        ParsedQrData("Номер заказа", parts[1], Icons.Filled.ShoppingCart),
        ParsedQrData("Номер детали", parts[2], Icons.Filled.DataObject),
        ParsedQrData("Название детали", parts[3], Icons.AutoMirrored.Filled.LabelImportant)
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptScreen(
    scannedValue: String?,
    quantity: String,
    cellCode: String,
    uiState: AcceptUiState,
    printerConnectionState: ConnectionState,
    scannerConnectionState: ScannerState,
    onScanWithScanner: (String) -> Unit,
    onScanWithCamera: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onCellCodeChange: (String) -> Unit,
    onPrintLabel: () -> Unit,
    onResetInputFields: () -> Unit,
    onClearMessage: () -> Unit,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: AcceptViewModel = hiltViewModel(),
    // Добавляем ViewModel для BLE сканера
    newlandViewModel: NewlandScannerViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val parsedData = remember(scannedValue) { parseFixedQrValue(scannedValue) }

    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val editingRecord by viewModel.editingRecord.collectAsState()
    val lastOperations by viewModel.lastOperations.collectAsState()

    // BLE сканер состояния
    val bleConnectionState by newlandViewModel.connectionState.collectAsState()
    val blePairingQr by newlandViewModel.pairingQrBitmap.collectAsState()
    val bleQrGenerationState by newlandViewModel.qrGenerationState.collectAsState()
    val bleConnectedDevice by newlandViewModel.connectedDevice.collectAsState()
    val showBlePairingDialog by newlandViewModel.showPairingDialog.collectAsState()

    var showScannerSetup by remember { mutableStateOf(false) }
    var scannerInputValue by remember { mutableStateOf("") }

    // Настройка callback для BLE сканера
    LaunchedEffect(Unit) {
        newlandViewModel.setScanCallback { scannedCode ->
            onScanWithScanner(scannedCode)
        }
    }

    // Очистка callback при уничтожении
    DisposableEffect(Unit) {
        onDispose {
            newlandViewModel.clearScanCallback()
        }
    }

    val borderColor = MaterialTheme.colorScheme.outline.darker(0.8f)
    val buttonBorder = BorderStroke(1.dp, borderColor)

    // Автоматически скрываем сообщения через 3 секунды
    LaunchedEffect(uiState) {
        when (uiState) {
            is AcceptUiState.Success, is AcceptUiState.Error -> {
                delay(3000)
                onClearMessage()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Приемка продукции", fontSize = 26.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", modifier = Modifier.size(36.dp))
                    }
                },
                actions = {
                    // Кнопка истории/редактирования последней операции
                    if (lastOperations.isNotEmpty()) {
                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            Button(
                                onClick = { viewModel.openLastOperation() },
                                modifier = Modifier.height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(
                                    Icons.Filled.History,
                                    contentDescription = "Последние операции",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "История (${lastOperations.size})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Индикатор состояния BLE сканера
                    BleStatusIndicator(
                        connectionState = bleConnectionState,
                        onClick = {
                            newlandViewModel.showPairingDialog { scannedCode ->
                                onScanWithScanner(scannedCode)
                            }
                        }
                    )

                    // Индикатор состояния HID сканера
                    ScannerStatusIndicator(
                        connectionState = scannerConnectionState,
                        onClick = {
                            showScannerSetup = true
                        }
                    )

                    // Индикатор состояния принтера
                    PrinterStatusIndicator(
                        connectionState = printerConnectionState,
                        onClick = onNavigateToSettings
                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() },
                modifier = Modifier.padding(8.dp)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Выберите способ сканирования:", style = MaterialTheme.typography.titleLarge, fontSize = 22.sp)

                // Поле для ввода со сканера (только если подключен HID сканер)
                if (scannerConnectionState == ScannerState.CONNECTED) {
                    ScannerInputField(
                        value = scannerInputValue,
                        onValueChange = { scannerInputValue = it },
                        onScanComplete = { code ->
                            onScanWithScanner(code)
                            scannerInputValue = ""
                        },
                        label = "Сканируйте QR-код (HID режим)",
                        placeholder = "Наведите сканер HR32-BT и нажмите кнопку",
                        isConnected = true,
                        autoFocus = bleConnectionState != NewlandConnectionState.CONNECTED,
                        clearAfterScan = true
                    )
                }

                // Кнопки для разных типов сканирования
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    // BLE сканер (приоритет, если доступен)
                    Button(
                        onClick = {
                            if (bleConnectionState == NewlandConnectionState.CONNECTED) {
                                // Уже подключен, показываем статус
                                newlandViewModel.showPairingDialog { scannedCode ->
                                    onScanWithScanner(scannedCode)
                                }
                            } else {
                                // Запускаем процесс сопряжения
                                newlandViewModel.showPairingDialog { scannedCode ->
                                    onScanWithScanner(scannedCode)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(100.dp),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        border = buttonBorder,
                        enabled = uiState !is AcceptUiState.Printing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (bleConnectionState) {
                                NewlandConnectionState.CONNECTED -> Color(0xFF4CAF50).copy(alpha = 0.9f)
                                NewlandConnectionState.CONNECTING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                when (bleConnectionState) {
                                    NewlandConnectionState.CONNECTED -> Icons.Filled.BluetoothConnected
                                    NewlandConnectionState.CONNECTING -> Icons.Filled.Sync
                                    else -> Icons.Filled.BluetoothSearching
                                },
                                "BLE сканер",
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                when (bleConnectionState) {
                                    NewlandConnectionState.CONNECTED -> "BLE готов"
                                    NewlandConnectionState.CONNECTING -> "Подключение..."
                                    else -> "BLE режим"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            if (bleConnectionState == NewlandConnectionState.CONNECTED) {
                                Text(
                                    "UTF-8 ✓",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // HID сканер
                    Button(
                        onClick = {
                            if (scannerConnectionState == ScannerState.CONNECTED) {
                                // Фокусируемся на поле ввода
                                focusManager.clearFocus()
                            } else {
                                showScannerSetup = true
                            }
                        },
                        modifier = Modifier.weight(1f).height(100.dp),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        border = buttonBorder,
                        enabled = uiState !is AcceptUiState.Printing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (scannerConnectionState == ScannerState.CONNECTED)
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (scannerConnectionState == ScannerState.CONNECTED)
                                    Icons.Filled.Keyboard
                                else Icons.Filled.KeyboardAlt,
                                "HID сканер",
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (scannerConnectionState == ScannerState.CONNECTED)
                                    "HID готов"
                                else "HID режим",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Камера
                    Button(
                        onClick = onScanWithCamera,
                        modifier = Modifier.weight(1f).height(100.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmYellow.darker(0.9f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        border = buttonBorder,
                        enabled = uiState !is AcceptUiState.Printing
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.CameraAlt, "Камера", modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Камера", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Статус подключенных сканеров
                ScannerStatusRow(
                    hidState = scannerConnectionState,
                    bleState = bleConnectionState,
                    bleDeviceName = bleConnectedDevice
                )

                // Остальная часть интерфейса остается без изменений...
                // [Поле отображения QR, поля ввода количества и ячейки, кнопки и т.д.]
            }

            // Сообщения о состоянии
            AnimatedVisibility(
                visible = uiState is AcceptUiState.Success || uiState is AcceptUiState.Error,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                when (uiState) {
                    is AcceptUiState.Success -> {
                        SuccessMessage(message = uiState.message)
                    }
                    is AcceptUiState.Error -> {
                        ErrorMessage(message = uiState.message)
                    }
                    else -> {}
                }
            }
        }
    }

    // Диалог сопряжения BLE
    if (showBlePairingDialog) {
        ImprovedBlePairingDialog(
            qrBitmap = blePairingQr,
            qrGenerationState = bleQrGenerationState,
            connectionState = bleConnectionState,
            connectedDevice = bleConnectedDevice,
            onGenerateQr = { newlandViewModel.generatePairingQr() },
            onDismiss = { newlandViewModel.hidePairingDialog() }
        )
    }

    // Диалог редактирования (без изменений)
    editingRecord?.let { record ->
        if (showEditDialog) {
            EditAcceptanceDialog(
                record = record,
                onDismiss = { viewModel.closeEditDialog() },
                onConfirm = { qty, cell ->
                    viewModel.updateRecord(record.id, qty, cell)
                }
            )
        }
    }
}

@Composable
fun BleStatusIndicator(
    connectionState: NewlandConnectionState,
    onClick: () -> Unit
) {
    val (icon, tint) = when (connectionState) {
        NewlandConnectionState.CONNECTED -> Icons.Filled.BluetoothConnected to Color(0xFF4CAF50)
        NewlandConnectionState.CONNECTING -> Icons.Filled.Sync to MaterialTheme.colorScheme.primary
        NewlandConnectionState.ERROR -> Icons.Filled.ErrorOutline to MaterialTheme.colorScheme.error
        else -> Icons.Filled.BluetoothDisabled to MaterialTheme.colorScheme.error
    }

    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Состояние BLE сканера",
            tint = tint,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun ScannerStatusRow(
    hidState: ScannerState,
    bleState: NewlandConnectionState,
    bleDeviceName: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // HID статус
        StatusChip(
            icon = Icons.Filled.Keyboard,
            text = "HID: ${if (hidState == ScannerState.CONNECTED) "Готов" else "Отключен"}",
            isConnected = hidState == ScannerState.CONNECTED
        )

        // BLE статус
        StatusChip(
            icon = Icons.Filled.Bluetooth,
            text = "BLE: ${
                when (bleState) {
                    NewlandConnectionState.CONNECTED -> bleDeviceName ?: "Готов"
                    NewlandConnectionState.CONNECTING -> "Подключение"
                    else -> "Отключен"
                }
            }",
            isConnected = bleState == NewlandConnectionState.CONNECTED
        )
    }
}

@Composable
fun StatusChip(
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