package com.example.myprinterapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myprinterapp.scanner.BleConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToBlePairing: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val printerName by viewModel.printerName.collectAsState()
    val printerMac by viewModel.printerMac.collectAsState()
    
    // Получаем статус подключения из uiState
    val isConnected = (uiState as? SettingsUiState.Success)?.isConnected ?: false
    
    // Добавляем состояния для BLE сканера
    val bleScannerViewModel: BleScannerSettingsViewModel = hiltViewModel()
    val scannerConnectionState by bleScannerViewModel.connectionState.collectAsState()
    val connectedDevice by bleScannerViewModel.connectedDevice.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Принтер
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Принтер",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Зеленый индикатор если подключен
                        if (isConnected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.size(12.dp)
                            ) {}
                            Text(
                                text = "✓ Подключен",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    if (isConnected) {
                        Text(
                            text = "✓ Соединение установлено успешно",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "✓ Готов к печати этикеток",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Text(
                        text = "Название: $printerName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "MAC: $printerMac",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Плотность печати: 8 (оптимальная)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Скорость печати: 2.0 (средняя)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Кнопки управления принтером
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentState = uiState
                        when (currentState) {
                            is SettingsUiState.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            is SettingsUiState.Success -> {
                                if (currentState.isConnected) {
                                    Button(
                                        onClick = { viewModel.printTestLabel() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Тест печати")
                                    }
                                    
                                    OutlinedButton(
                                        onClick = { viewModel.disconnectPrinter() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Отключить")
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.connectPrinter() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Подключить")
                                    }
                                }
                            }
                            is SettingsUiState.Error -> {
                                Button(
                                    onClick = { viewModel.connectPrinter() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Переподключить")
                                }
                            }
                            is SettingsUiState.PermissionRequired -> {
                                Text(
                                    text = currentState.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            
            // Настройка BLE сканера
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (scannerConnectionState == BleConnectionState.CONNECTED) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (scannerConnectionState) {
                                BleConnectionState.CONNECTED -> Icons.Default.Scanner
                                else -> Icons.Default.Scanner
                            },
                            contentDescription = null,
                            tint = if (scannerConnectionState == BleConnectionState.CONNECTED) {
                                Color(0xFF4CAF50)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = "BLE Сканер",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Зеленый индикатор если подключен
                        if (scannerConnectionState == BleConnectionState.CONNECTED) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.size(12.dp)
                            ) {}
                            Text(
                                text = "✓ Подключен",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    if (scannerConnectionState == BleConnectionState.CONNECTED) {
                        Text(
                            text = "✓ BLE соединение активно",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "✓ Готов к сканированию QR-кодов",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                        
                        connectedDevice?.let { device ->
                            Text(
                                text = "Устройство: ${device.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Адрес: ${device.address}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Для настройки BLE сканера перейдите на отдельную страницу настроек",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = onNavigateToBlePairing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Настроить BLE сканер")
                    }
                }
            }
            
            // Дополнительные настройки
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Дополнительные параметры",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Автопечать после сканирования
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Автопечать этикеток")
                            Text(
                                "Печатать сразу после заполнения полей",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = false, // TODO: добавить в ViewModel
                            onCheckedChange = { /* TODO: viewModel.setAutoPrint(it) */ }
                        )
                    }
                    
                    Divider()
                    
                    // Звуковые сигналы
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Звуковые сигналы")
                            Text(
                                "Звук при успешных операциях",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = true, // TODO: добавить в ViewModel
                            onCheckedChange = { /* TODO: viewModel.setSoundEnabled(it) */ }
                        )
                    }
                    
                    Divider()
                    
                    // Размер шрифта этикеток
                    Column {
                        Text("Размер шрифта этикеток")
                        Slider(
                            value = 12f, // TODO: добавить в ViewModel
                            onValueChange = { /* TODO: viewModel.setLabelFontSize(it) */ },
                            valueRange = 8f..24f,
                            steps = 7,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Текущий размер: 12", // TODO: добавить в ViewModel
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Text(
                            text = "О приложении",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    InfoRow("Версия", "1.0.0") // TODO: BuildConfig.VERSION_NAME
                    InfoRow("Сборка", "1") // TODO: BuildConfig.VERSION_CODE.toString()
                    InfoRow("База данных", "SQLite v3.0") // TODO: database.version
                    
                    Button(
                        onClick = { /* TODO: viewModel.exportDatabase() */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Экспорт базы данных")
                    }
                }
            }
            
            // Отображение ошибок
            val currentState = uiState
            if (currentState is SettingsUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = currentState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
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