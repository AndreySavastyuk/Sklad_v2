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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.example.myprinterapp.data.models.ConnectionState
import com.example.myprinterapp.data.models.AcceptanceOperation
import com.example.myprinterapp.ui.theme.WarmYellow
import com.example.myprinterapp.ui.components.ScannerPairingDialog
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

// Состояния для совместимости
enum class ScannerState { CONNECTED, DISCONNECTED }
enum class NewlandConnectionState { CONNECTED, CONNECTING, DISCONNECTED, ERROR }
sealed class AcceptUiState {
    object Idle : AcceptUiState()
    object Printing : AcceptUiState()
    data class Success(val message: String) : AcceptUiState()
    data class Error(val message: String) : AcceptUiState()
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
    onNavigateToSettings: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val parsedData = remember(scannedValue) { parseFixedQrValue(scannedValue) }

    // Моковые состояния для BLE сканера
    val bleConnectionState = NewlandConnectionState.DISCONNECTED
    val bleConnectedDevice: String? = null
    val showBlePairingDialog = false

    var showScannerSetup by remember { mutableStateOf(false) }
    var scannerInputValue by remember { mutableStateOf("") }

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
                    // Индикатор состояния принтера
                    PrinterStatusIndicator(
                        connectionState = printerConnectionState,
                        onClick = onNavigateToSettings
                    )
                }
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

                // Поле для ввода со сканера (только если подключен внешний сканер)
                if (scannerConnectionState == ScannerState.CONNECTED) {
                    ScannerInputField(
                        value = scannerInputValue,
                        onValueChange = { scannerInputValue = it },
                        onScanComplete = { code ->
                            onScanWithScanner(code)
                            scannerInputValue = ""
                        },
                        label = "Сканируйте QR-код внешним сканером",
                        placeholder = "Используйте подключенный сканер",
                        isConnected = true,
                        autoFocus = bleConnectionState != NewlandConnectionState.CONNECTED,
                        clearAfterScan = true
                    )
                }

                // Кнопки для сканирования (адаптированные для планшета)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp), 
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Внешний сканер (с QR сопряжения)
                    Button(
                        onClick = { showScannerSetup = true },
                        modifier = Modifier.weight(1f).height(120.dp), // Увеличенная высота для планшета
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(vertical = 16.dp),
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
                                    Icons.Filled.QrCodeScanner
                                else Icons.Filled.QrCode2,
                                "Внешний сканер",
                                modifier = Modifier.size(48.dp) // Большие иконки для планшета
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (scannerConnectionState == ScannerState.CONNECTED)
                                    "Сканер готов"
                                else "Подключить сканер",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Камера
                    Button(
                        onClick = onScanWithCamera,
                        modifier = Modifier.weight(1f).height(120.dp), // Увеличенная высота
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmYellow.darker(0.9f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        border = buttonBorder,
                        enabled = uiState !is AcceptUiState.Printing
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.CameraAlt, "Камера", modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Камера планшета", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Статус подключенного сканера
                if (scannerConnectionState == ScannerState.CONNECTED) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Внешний сканер подключен и готов к работе",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Поле отображения отсканированного QR-кода
                OutlinedTextField(
                    value = scannedValue ?: "QR не отсканирован",
                    onValueChange = {},
                    label = { Text("Содержимое QR-кода", fontSize = 18.sp) },
                    leadingIcon = { Icon(Icons.Filled.QrCodeScanner, "QR-код", modifier = Modifier.size(32.dp)) },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                    minLines = 1,
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = borderColor,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Поля ввода количества и ячейки (увеличенные для планшета)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp), // Больше пространства
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LargeInputTextField(
                        value = quantity,
                        onValueChange = onQuantityChange,
                        label = "Количество",
                        icon = Icons.Filled.Numbers,
                        labelFontSize = 28.sp, // Увеличенный шрифт для планшета
                        valueFontSize = 48.sp, // Еще больше для удобства ввода
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        borderColor = borderColor,
                        labelTextAlign = TextAlign.Center,
                        enabled = uiState !is AcceptUiState.Printing
                    )
                    LargeInputTextField(
                        value = cellCode,
                        onValueChange = { newRaw ->
                            val new = newRaw.filter { it.isLetterOrDigit() }
                            if (new.length <= 4) {
                                onCellCodeChange(new.uppercase())
                            }
                        },
                        label = "Ячейка хранения",
                        icon = Icons.Filled.Inventory2,
                        labelFontSize = 28.sp, // Увеличенный шрифт
                        valueFontSize = 42.sp, // Большой шрифт для удобства
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.weight(1f),
                        borderColor = borderColor,
                        labelTextAlign = TextAlign.Center,
                        enabled = uiState !is AcceptUiState.Printing
                    )
                }

                // Кнопка сброса (увеличенная для планшета)
                Button(
                    onClick = {
                        onResetInputFields()
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.fillMaxWidth().height(70.dp), // Увеличенная высота
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = buttonBorder,
                    enabled = uiState !is AcceptUiState.Printing
                ) {
                    Icon(Icons.Filled.Clear, "Сброс", modifier = Modifier.size(36.dp)) // Большие иконки
                    Spacer(Modifier.width(12.dp))
                    Text("Сброс полей", fontSize = 20.sp) // Больший шрифт
                }

                // Детализация QR-кода
                if (parsedData.isNotEmpty()) {
                    Text(
                        "Детализация QR-кода:",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(parsedData) { dataItem ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.darker(0.7f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = dataItem.icon,
                                        contentDescription = dataItem.key,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = dataItem.key,
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = dataItem.value,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f, fill = parsedData.isEmpty()))

                // Кнопка печати (увеличенная для планшета)
                Button(
                    onClick = onPrintLabel,
                    enabled = scannedValue != null &&
                            quantity.isNotBlank() &&
                            cellCode.length > 0 &&
                            cellCode.length <= 4 &&
                            printerConnectionState == ConnectionState.CONNECTED &&
                            uiState !is AcceptUiState.Printing,
                    modifier = Modifier.fillMaxWidth().height(120.dp), // Увеличенная высота для планшета
                    shape = MaterialTheme.shapes.large,
                    border = buttonBorder
                ) {
                    when (uiState) {
                        is AcceptUiState.Printing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp), // Больший индикатор
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(20.dp))
                            Text("Печать этикетки...", fontSize = 28.sp, fontWeight = FontWeight.Bold) // Больший шрифт
                        }
                        else -> {
                            Icon(Icons.Filled.Print, "Печать бирки", modifier = Modifier.size(48.dp)) // Большая иконка
                            Spacer(Modifier.width(20.dp))
                            Text("Печать этикетки", fontSize = 28.sp, fontWeight = FontWeight.Bold) // Больший шрифт
                        }
                    }
                }
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
    
    // Диалог сопряжения сканера
    if (showScannerSetup) {
        ScannerPairingDialog(
            onDismiss = { showScannerSetup = false },
            onConnectQr = {
                // TODO: Реализовать QR подключение сканера
                android.util.Log.d("AcceptScreen", "QR подключение сканера")
                showScannerSetup = false
            },
            onConnectManual = {
                // TODO: Реализовать ручное подключение сканера
                android.util.Log.d("AcceptScreen", "Ручное подключение сканера")
                showScannerSetup = false
            }
        )
    }
}

@Composable
fun PrinterStatusIndicator(
    connectionState: ConnectionState,
    onClick: () -> Unit
) {
    val (icon, tint) = when (connectionState) {
        ConnectionState.CONNECTED -> Icons.Filled.Print to Color(0xFF4CAF50)
        ConnectionState.CONNECTING -> Icons.Filled.Sync to MaterialTheme.colorScheme.primary
        ConnectionState.DISCONNECTED -> Icons.Filled.PrintDisabled to MaterialTheme.colorScheme.error
        else -> Icons.Filled.ErrorOutline to MaterialTheme.colorScheme.error
    }

    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Состояние принтера",
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
            text = "BLE: Отключен",
            isConnected = false
        )
    }
}

@Composable
fun StatusChip(
    icon: ImageVector,
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

@Composable
fun SuccessMessage(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Error,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeInputTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    fieldHeight: Dp = 120.dp,
    labelFontSize: TextUnit = 25.sp,
    valueFontSize: TextUnit = 40.sp,
    borderColor: Color,
    labelTextAlign: TextAlign = TextAlign.Start,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(fieldHeight).fillMaxWidth(),
        label = {
            if (value.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(label, fontSize = labelFontSize, textAlign = labelTextAlign)
                }
            } else {
                Text(label, fontSize = MaterialTheme.typography.bodySmall.fontSize)
            }
        },
        leadingIcon = { Icon(icon, label, modifier = Modifier.size(30.dp)) },
        textStyle = TextStyle(
            fontSize = valueFontSize,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = borderColor,
        )
    )
}

@Composable
fun ScannerInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onScanComplete: (String) -> Unit,
    label: String,
    placeholder: String,
    isConnected: Boolean,
    autoFocus: Boolean,
    clearAfterScan: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue)
            if (newValue.isNotBlank() && newValue.endsWith('\n')) {
                onScanComplete(newValue.trim())
                if (clearAfterScan) {
                    onValueChange("")
                }
            }
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        enabled = isConnected
    )
}