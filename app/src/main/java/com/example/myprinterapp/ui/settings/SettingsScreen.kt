package com.example.myprinterapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val printerName by viewModel.printerName.collectAsStateWithLifecycle()
    val printerMac by viewModel.printerMac.collectAsStateWithLifecycle()
    
    var showPrinterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Настройки системы") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)  // Увеличенные отступы для планшета
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)  // Больше пространства между секциями
        ) {
            // Настройки принтера
            SettingsCard(
                title = "Принтер",
                icon = Icons.Filled.Print
            ) {
                PrinterSettingsContent(
                    uiState = uiState,
                    connectionState = connectionState,
                    printerName = printerName,
                    printerMac = printerMac,
                    onConnectPrinter = viewModel::connectPrinter,
                    onDisconnectPrinter = viewModel::disconnectPrinter,
                    onTestPrint = viewModel::printTestLabel,
                    onSelectPrinter = { showPrinterDialog = true }
                )
            }
            
            // Настройки сканера
            SettingsCard(
                title = "Сканер", 
                icon = Icons.Filled.QrCodeScanner
            ) {
                ScannerSettingsContent()
            }
            
            // Сетевая синхронизация
            SettingsCard(
                title = "Синхронизация",
                icon = Icons.Filled.Sync
            ) {
                NetworkSyncSettingsContent()
            }
            
            // Настройки приложения (упрощенные)
            SettingsCard(
                title = "Приложение",
                icon = Icons.Filled.Settings
            ) {
                AppSettingsContent()
            }
        }
    }
    
    // Диалог выбора принтера
    if (showPrinterDialog) {
        PrinterSelectionDialog(
            onDismiss = { showPrinterDialog = false },
            onPrinterSelected = { mac, name ->
                viewModel.selectPrinterManually(mac, name)
                showPrinterDialog = false
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)  // Увеличенные отступы
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)  // Большие иконки для планшета
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun PrinterSettingsContent(
    uiState: SettingsUiState,
    connectionState: com.example.myprinterapp.printer.ConnectionState,
    printerName: String,
    printerMac: String,
    onConnectPrinter: () -> Unit,
    onDisconnectPrinter: () -> Unit,
    onTestPrint: () -> Unit,
    onSelectPrinter: () -> Unit
) {
    var autoConnect by remember { mutableStateOf(true) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Принтер по умолчанию
        OutlinedTextField(
            value = printerName,
            onValueChange = { },
            label = { Text("Текущий принтер") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                when (connectionState) {
                    com.example.myprinterapp.printer.ConnectionState.CONNECTED -> {
                        Icon(Icons.Filled.CheckCircle, "Подключен", tint = MaterialTheme.colorScheme.primary)
                    }
                    com.example.myprinterapp.printer.ConnectionState.CONNECTING -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                    com.example.myprinterapp.printer.ConnectionState.DISCONNECTED -> {
                        Icon(Icons.Filled.Error, "Отключен", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )

        // Адрес принтера
        if (printerMac.isNotEmpty()) {
            OutlinedTextField(
                value = printerMac,
                onValueChange = { },
                label = { Text("MAC-адрес") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Управление подключением
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (connectionState == com.example.myprinterapp.printer.ConnectionState.CONNECTED) {
                Button(
                    onClick = onDisconnectPrinter,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.BluetoothDisabled, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Отключить")
                }
            } else {
                Button(
                    onClick = onConnectPrinter,
                    modifier = Modifier.weight(1f),
                    enabled = printerMac.isNotEmpty() && connectionState != com.example.myprinterapp.printer.ConnectionState.CONNECTING
                ) {
                    if (connectionState == com.example.myprinterapp.printer.ConnectionState.CONNECTING) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Bluetooth, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (connectionState == com.example.myprinterapp.printer.ConnectionState.CONNECTING) "Подключение..." else "Подключить")
                }
            }

            OutlinedButton(
                onClick = onSelectPrinter,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Выбрать")
            }
        }

        // Сообщения об ошибках
        when (uiState) {
            is SettingsUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            is SettingsUiState.Loading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выполняется операция...")
                }
            }
            else -> { /* Success state, no additional UI needed */ }
        }

        // Информация о настройках печати (только информационно)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Параметры печати",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                InfoRow("Плотность печати:", "8 (оптимальная)")
                InfoRow("Скорость печати:", "2.0 (средняя)")
                InfoRow("Размер этикетки:", "57x40 мм")
                InfoRow("Разрешение:", "203 DPI")
            }
        }
        
        // Автоподключение
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Автоматическое подключение", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = autoConnect,
                onCheckedChange = { autoConnect = it }
            )
        }
        
        // Тест подключения
        Button(
            onClick = { 
                onTestPrint()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(Icons.Filled.Print, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Тестовая печать")
        }
    }
}

@Composable
private fun ScannerSettingsContent() {
    var autoConnect by remember { mutableStateOf(true) }
    var beepOnScan by remember { mutableStateOf(true) }
    var vibrationOnScan by remember { mutableStateOf(true) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Текущий сканер
        OutlinedTextField(
            value = "Встроенная камера",
            onValueChange = { },
            label = { Text("Текущий сканер") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { }) {
                    Icon(Icons.Filled.QrCode2, "Сменить")
                }
            }
        )
        
        // Автоподключение
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Автоматическое подключение", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = autoConnect,
                onCheckedChange = { autoConnect = it }
            )
        }
        
        // Звук при сканировании
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Звуковой сигнал", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = beepOnScan,
                onCheckedChange = { beepOnScan = it }
            )
        }
        
        // Вибрация
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Вибрация", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = vibrationOnScan,
                onCheckedChange = { vibrationOnScan = it }
            )
        }
        
        // Тест сканера
        Button(
            onClick = { 
                android.util.Log.d("Settings", "Тест сканера")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Тест сканирования")
        }
    }
}

@Composable
private fun AppSettingsContent() {
    var confirmationDialogs by remember { mutableStateOf(true) }
    var debugMode by remember { mutableStateOf(false) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Диалоги подтверждения
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Диалоги подтверждения", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = confirmationDialogs,
                onCheckedChange = { confirmationDialogs = it }
            )
        }
        
        // Режим отладки
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Режим отладки", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = debugMode,
                onCheckedChange = { debugMode = it }
            )
        }
        
        // Информация о приложении
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Информация о системе",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                InfoRow("Версия:", "1.0.0")
                InfoRow("Сборка:", "Release")
                InfoRow("Дата:", "2024-01-15")
            }
        }
        
        // Действия
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { 
                    android.util.Log.d("Settings", "Экспорт данных")
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Экспорт")
            }
            
            OutlinedButton(
                onClick = { 
                    android.util.Log.d("Settings", "Сброс настроек")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Сброс")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NetworkSyncSettingsContent() {
    var syncEnabled by remember { mutableStateOf(false) }
    var wifiOnly by remember { mutableStateOf(true) }
    var syncInterval by remember { mutableIntStateOf(60) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Включение синхронизации
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Синхронизация с сервером", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = syncEnabled,
                onCheckedChange = { syncEnabled = it }
            )
        }
        
        AnimatedVisibility(visible = syncEnabled) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // URL сервера
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("URL сервера") },
                    placeholder = { Text("https://example.com/api") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Интервал синхронизации
                Column {
                    Text("Интервал синхронизации: $syncInterval минут", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = syncInterval.toFloat(),
                        onValueChange = { syncInterval = it.toInt() },
                        valueRange = 15f..180f,
                        steps = 10,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                // Только по Wi-Fi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Только по Wi-Fi", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = wifiOnly,
                        onCheckedChange = { wifiOnly = it }
                    )
                }
            }
        }
        
        // Статус последней синхронизации
        if (syncEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Статус синхронизации",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Последняя синхронизация: Никогда",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PrinterSelectionDialog(
    onDismiss: () -> Unit,
    onPrinterSelected: (String, String) -> Unit
) {
    var macAddress by remember { mutableStateOf("") }
    var printerName by remember { mutableStateOf("Термопринтер") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройка принтера") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Введите MAC-адрес вашего термопринтера в формате AA:BB:CC:DD:EE:FF",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = macAddress,
                    onValueChange = { macAddress = it.uppercase() },
                    label = { Text("MAC-адрес") },
                    placeholder = { Text("например: 12:34:56:78:9A:BC") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = printerName,
                    onValueChange = { printerName = it },
                    label = { Text("Название принтера") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    "Для поиска MAC-адреса:\n• Включите принтер\n• Напечатайте тестовую страницу\n• Найдите раздел 'Network' или 'Bluetooth'",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (macAddress.isNotBlank()) {
                        onPrinterSelected(macAddress, printerName)
                    }
                },
                enabled = macAddress.isNotBlank()
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