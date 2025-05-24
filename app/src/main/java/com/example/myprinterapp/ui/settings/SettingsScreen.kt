package com.example.myprinterapp.ui.settings

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myprinterapp.printer.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val printerName by viewModel.printerName.collectAsState(initial = "Не выбран")
    val printerMac by viewModel.printerMac.collectAsState(initial = "")
    val connectionState by viewModel.connectionState.collectAsState()

    var showPrinterDialog by remember { mutableStateOf(false) }
    var showTestPrintDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    // Bluetooth permissions
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            showPrinterDialog = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Настройки",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Секция принтера
            item {
                SettingsSection(
                    title = "Принтер",
                    icon = Icons.Filled.Print
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Информация о принтере
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Устройство:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    printerName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                if (printerMac.isNotEmpty()) {
                                    Text(
                                        printerMac,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Статус подключения
                            ConnectionStatusChip(connectionState)
                        }

                        // Кнопки управления
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Выбрать принтер
                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        bluetoothPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.BLUETOOTH_SCAN,
                                                Manifest.permission.BLUETOOTH_CONNECT
                                            )
                                        )
                                    } else {
                                        showPrinterDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Filled.BluetoothSearching,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Выбрать")
                            }

                            // Подключить/Отключить
                            if (printerMac.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        if (connectionState == ConnectionState.CONNECTED) {
                                            viewModel.disconnectPrinter()
                                        } else {
                                            viewModel.connectPrinter()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = uiState !is SettingsUiState.Loading
                                ) {
                                    when (connectionState) {
                                        ConnectionState.CONNECTED -> {
                                            Icon(Icons.Filled.BluetoothDisabled, null, Modifier.size(20.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Отключить")
                                        }
                                        ConnectionState.CONNECTING -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Подключение...")
                                        }
                                        else -> {
                                            Icon(Icons.Filled.BluetoothConnected, null, Modifier.size(20.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Подключить")
                                        }
                                    }
                                }
                            }
                        }

                        // Тестовая печать
                        if (connectionState == ConnectionState.CONNECTED) {
                            Button(
                                onClick = { showTestPrintDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Filled.Print, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Тестовая печать")
                            }
                        }
                    }
                }
            }

            // Секция параметров печати
            item {
                SettingsSection(
                    title = "Параметры печати",
                    icon = Icons.Filled.Settings
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PrintSettingsItem(
                            title = "Плотность печати",
                            value = "8 (стандарт)",
                            icon = Icons.Filled.Tune
                        )

                        PrintSettingsItem(
                            title = "Скорость печати",
                            value = "2.0 дюйм/сек",
                            icon = Icons.Filled.Speed
                        )

                        PrintSettingsItem(
                            title = "Размер этикетки",
                            value = "57 x 40 мм",
                            icon = Icons.Filled.CropFree
                        )

                        PrintSettingsItem(
                            title = "Кодировка QR",
                            value = "UTF-8",
                            icon = Icons.Filled.QrCode
                        )
                    }
                }
            }

            // Секция данных и хранения
            item {
                SettingsSection(
                    title = "Данные и хранение",
                    icon = Icons.Filled.Storage
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsActionItem(
                            title = "Экспорт журнала",
                            subtitle = "Сохранить журнал операций в файл",
                            icon = Icons.Filled.FileDownload,
                            onClick = { /* TODO: Реализовать экспорт */ }
                        )

                        SettingsActionItem(
                            title = "Синхронизация данных",
                            subtitle = "Синхронизировать с сервером",
                            icon = Icons.Filled.Sync,
                            onClick = { /* TODO: Реализовать синхронизацию */ }
                        )

                        SettingsActionItem(
                            title = "Очистить данные",
                            subtitle = "Удалить все записи журнала",
                            icon = Icons.Filled.DeleteSweep,
                            onClick = { showClearDataDialog = true },
                            isDestructive = true
                        )
                    }
                }
            }

            // Секция системы
            item {
                SettingsSection(
                    title = "Система",
                    icon = Icons.Filled.Info
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsActionItem(
                            title = "О приложении",
                            subtitle = "Версия, лицензии, информация",
                            icon = Icons.Filled.Info,
                            onClick = { showAboutDialog = true }
                        )

                        SettingsActionItem(
                            title = "Проверить обновления",
                            subtitle = "Поиск новых версий приложения",
                            icon = Icons.Filled.SystemUpdate,
                            onClick = { /* TODO: Реализовать проверку обновлений */ }
                        )

                        SettingsActionItem(
                            title = "Диагностика",
                            subtitle = "Состояние системы и диагностика",
                            icon = Icons.Filled.BugReport,
                            onClick = { /* TODO: Реализовать диагностику */ }
                        )
                    }
                }
            }

            // Сообщения об ошибках
            if (uiState is SettingsUiState.Error) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                (uiState as SettingsUiState.Error).message,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }

    // Диалоги
    if (showPrinterDialog) {
        PrinterSelectionDialog(
            onDismiss = { showPrinterDialog = false },
            onPrinterSelected = { device ->
                viewModel.selectPrinter(device)
                showPrinterDialog = false
            }
        )
    }

    if (showTestPrintDialog) {
        TestPrintDialog(
            onDismiss = { showTestPrintDialog = false },
            onPrint = {
                viewModel.printTestLabel()
                showTestPrintDialog = false
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    if (showClearDataDialog) {
        ClearDataDialog(
            onDismiss = { showClearDataDialog = false },
            onConfirm = {
                // TODO: Вызвать метод очистки данных
                showClearDataDialog = false
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            content()
        }
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("О приложении") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "MyPrinterApp",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text("Версия: 1.0.0")
                Text("Сборка: 2025.05.24")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Приложение для управления складскими операциями с печатью этикеток.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "© 2025 Все права защищены",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
fun ClearDataDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Очистить данные") },
        text = {
            Text(
                "Это действие удалит все записи из журнала операций. " +
                        "Данные нельзя будет восстановить.\n\n" +
                        "Вы уверены, что хотите продолжить?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

// Остальные компоненты остаются без изменений...
@Composable
fun ConnectionStatusChip(state: ConnectionState) {
    val (icon, text, containerColor, contentColor) = when (state) {
        ConnectionState.CONNECTED -> {
            Tuple4(
                Icons.Filled.CheckCircle,
                "Подключен",
                Color(0xFF4CAF50),
                Color.White
            )
        }
        ConnectionState.CONNECTING -> {
            Tuple4(
                Icons.Filled.Sync,
                "Подключение",
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        else -> {
            Tuple4(
                Icons.Filled.Cancel,
                "Отключен",
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

@Composable
fun PrintSettingsItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PrinterSelectionDialog(
    onDismiss: () -> Unit,
    onPrinterSelected: (BluetoothDevice) -> Unit
) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter

    val pairedDevices = remember {
        try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Print,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Выберите принтер")
            }
        },
        text = {
            if (pairedDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.BluetoothDisabled,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Нет сопряженных устройств",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Сначала выполните сопряжение с принтером в настройках Bluetooth",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(pairedDevices) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPrinterSelected(device) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = try { device.name ?: "Неизвестное устройство" } catch (e: SecurityException) { "Неизвестное устройство" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = device.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun TestPrintDialog(
    onDismiss: () -> Unit,
    onPrint: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Print,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("Тестовая печать") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Будет напечатана тестовая этикетка со следующими данными:")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Part: TEST-001", style = MaterialTheme.typography.bodyMedium)
                        Text("Тестовая деталь", style = MaterialTheme.typography.bodyMedium)
                        Text("Order: 2024/TEST", style = MaterialTheme.typography.bodyMedium)
                        Text("Loc: A1", style = MaterialTheme.typography.bodyMedium)
                        Text("Qty: 10", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Text(
                    "Убедитесь, что в принтере есть этикетки размером 57x40 мм",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onPrint) {
                Text("Печать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

// Вспомогательный класс для группировки данных
data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)