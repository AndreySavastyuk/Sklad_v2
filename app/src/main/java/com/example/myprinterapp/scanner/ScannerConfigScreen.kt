package com.example.myprinterapp.ui.scanner

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myprinterapp.scanner.ScannerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Экран настройки и тестирования режимов сканера HR32-BT
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerConfigScreen(
    onBack: () -> Unit,
    viewModel: ScannerConfigViewModel = hiltViewModel()
) {
    val scannerState by viewModel.scannerState.collectAsState()
    val currentMode by viewModel.currentScannerMode.collectAsState()
    val testResults by viewModel.testResults.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()

    var selectedMode by remember { mutableStateOf(currentMode) }
    var showTestDialog by remember { mutableStateOf(false) }
    var testInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Настройка сканера HR32-BT",
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
                    ConnectionStatusChip(scannerState)
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
                    scannerState = scannerState,
                    connectedDevice = connectedDevice,
                    onRefresh = { viewModel.refreshConnection() }
                )
            }

            // Выбор режима сканера
            item {
                ScannerModeSelector(
                    currentMode = selectedMode,
                    onModeSelected = { mode ->
                        selectedMode = mode
                    }
                )
            }

            // Инструкции по настройке
            item {
                ConfigInstructionsCard(
                    mode = selectedMode,
                    onShowQrCodes = {
                        // TODO: Показать QR коды для настройки
                    }
                )
            }

            // Тестирование режима
            item {
                TestModeCard(
                    onStartTest = { showTestDialog = true }
                )
            }

            // Результаты тестов
            if (testResults.isNotEmpty()) {
                item {
                    TestResultsCard(testResults)
                }
            }

            // Дополнительные настройки
            item {
                AdvancedSettingsCard()
            }

            // Кнопка применения настроек
            item {
                Button(
                    onClick = {
                        viewModel.applyScannerMode(selectedMode)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = scannerState == ScannerState.CONNECTED && selectedMode != currentMode
                ) {
                    Icon(Icons.Filled.Save, null, Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Применить настройки", fontSize = 18.sp)
                }
            }
        }
    }

    // Диалог тестирования
    if (showTestDialog) {
        TestScannerDialog(
            onDismiss = { showTestDialog = false },
            onTest = { input ->
                viewModel.testScannerInput(input)
                testInput = input
                showTestDialog = false
            }
        )
    }
}

@Composable
private fun ConnectionInfoCard(
    scannerState: ScannerState,
    connectedDevice: String?,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (scannerState) {
                ScannerState.CONNECTED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Состояние подключения",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (connectedDevice != null) {
                        Text(
                            connectedDevice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, "Обновить")
                }
            }

            when (scannerState) {
                ScannerState.CONNECTED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Сканер подключен и готов к работе",
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                ScannerState.DISCONNECTED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Сканер не подключен",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = { /* TODO: Открыть настройки Bluetooth */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Bluetooth, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Подключить сканер")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerModeSelector(
    currentMode: ScannerMode,
    onModeSelected: (ScannerMode) -> Unit
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
                    Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Режим работы сканера",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider()

            ScannerMode.values().forEach { mode ->
                ModeSelectionItem(
                    mode = mode,
                    isSelected = mode == currentMode,
                    onClick = { onModeSelected(mode) }
                )
                if (mode != ScannerMode.values().last()) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ModeSelectionItem(
    mode: ScannerMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mode.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (mode.features.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    mode.features.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                if (feature.supported) Icons.Filled.Check else Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (feature.supported) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                feature.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigInstructionsCard(
    mode: ScannerMode,
    onShowQrCodes: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
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
                    Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Инструкция по настройке",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "Для режима \"${mode.displayName}\":",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            mode.setupSteps.forEachIndexed { index, step ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                (index + 1).toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        step,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Button(
                onClick = onShowQrCodes,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.QrCode, null)
                Spacer(Modifier.width(8.dp))
                Text("Показать QR-коды настройки")
            }
        }
    }
}

@Composable
private fun TestModeCard(
    onStartTest: () -> Unit
) {
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
                    Icons.Filled.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Тестирование режима",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "Проверьте работу сканера в выбранном режиме",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onStartTest,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Scanner, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Тест сканера")
                }

                OutlinedButton(
                    onClick = { /* TODO: Генерация тестовых QR */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.QrCode2, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Тестовые QR")
                }
            }
        }
    }
}

@Composable
private fun TestResultsCard(results: List<TestResult>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Результаты тестирования",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            results.forEach { result ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                result.timestamp,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                result.mode.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            "Входные данные:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                result.input,
                                modifier = Modifier.padding(8.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            "Результат декодирования:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (result.success) {
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                result.output,
                                modifier = Modifier.padding(8.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        if (result.details != null) {
                            Text(
                                "Детали: ${result.details}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedSettingsCard() {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Дополнительные настройки",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Суффикс/префикс
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            label = { Text("Префикс") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            label = { Text("Суффикс") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Опции
                    LabeledSwitch(
                        label = "Звуковой сигнал",
                        checked = true,
                        onCheckedChange = {}
                    )
                    LabeledSwitch(
                        label = "Автоматическая отправка Enter",
                        checked = false,
                        onCheckedChange = {}
                    )
                    LabeledSwitch(
                        label = "Преобразовывать в верхний регистр",
                        checked = false,
                        onCheckedChange = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun TestScannerDialog(
    onDismiss: () -> Unit,
    onTest: (String) -> Unit
) {
    var manualInput by remember { mutableStateOf("") }
    var scannerInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Scanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Тестирование сканера")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Отсканируйте QR-код или введите данные вручную для тестирования",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Поле для сканера
                OutlinedTextField(
                    value = scannerInput,
                    onValueChange = {
                        scannerInput = it
                        // Автоматически запускаем тест при вводе со сканера
                        if (it.endsWith('\n') || it.endsWith('\r')) {
                            onTest(it.trim())
                        }
                    },
                    label = { Text("Ввод со сканера") },
                    placeholder = { Text("Сканируйте QR-код...") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Filled.QrCodeScanner, null)
                    },
                    singleLine = true
                )

                Divider()

                // Ручной ввод
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text("Ручной ввод") },
                    placeholder = { Text("Введите тестовые данные") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Filled.Keyboard, null)
                    }
                )

                // Примеры тестовых данных
                Text(
                    "Примеры тестовых данных:",
                    style = MaterialTheme.typography.labelMedium
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TestDataChip(
                        "Обычный текст",
                        "test=2024/001=PART-123=Деталь",
                        onClick = { manualInput = it }
                    )
                    TestDataChip(
                        "С кириллицей",
                        "тест=2024/001=ДЕТАЛЬ-123=Тестовая деталь",
                        onClick = { manualInput = it }
                    )
                    TestDataChip(
                        "HEX формат",
                        "\\xD2\\xE5\\xF1\\xF2",
                        onClick = { manualInput = it }
                    )
                    TestDataChip(
                        "С AIM ID",
                        "]Q0test=2024/001=PART-123",
                        onClick = { manualInput = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val input = if (scannerInput.isNotEmpty()) scannerInput else manualInput
                    if (input.isNotEmpty()) {
                        onTest(input)
                    }
                },
                enabled = scannerInput.isNotEmpty() || manualInput.isNotEmpty()
            ) {
                Text("Тестировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun TestDataChip(
    label: String,
    data: String,
    onClick: (String) -> Unit
) {
    AssistChip(
        onClick = { onClick(data) },
        label = { Text(label, fontSize = 12.sp) },
        modifier = Modifier.height(32.dp)
    )
}

@Composable
private fun ConnectionStatusChip(state: ScannerState) {
    val (icon, text, color) = when (state) {
        ScannerState.CONNECTED -> Triple(
            Icons.Filled.BluetoothConnected,
            "Подключен",
            Color(0xFF4CAF50)
        )
        ScannerState.DISCONNECTED -> Triple(
            Icons.Filled.BluetoothDisabled,
            "Отключен",
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

// Модели данных
enum class ScannerMode(
    val displayName: String,
    val description: String,
    val setupSteps: List<String>,
    val features: List<Feature> = emptyList()
) {
    HID_STANDARD(
        displayName = "HID стандартный",
        description = "Базовый режим HID без дополнительных идентификаторов",
        setupSteps = listOf(
            "Отсканируйте 'Enter Setup'",
            "Отсканируйте 'Bluetooth HID Mode'",
            "Отсканируйте 'Standard Format'",
            "Отсканируйте 'Exit Setup'"
        ),
        features = listOf(
            Feature("Простая настройка", true),
            Feature("Кириллица", false),
            Feature("Идентификация типа кода", false)
        )
    ),
    HID_WITH_AIM_ID(
        displayName = "HID с AIM ID",
        description = "HID режим с идентификатором типа штрих-кода (]X0)",
        setupSteps = listOf(
            "Отсканируйте 'Enter Setup'",
            "Отсканируйте 'Bluetooth HID Mode'",
            "Отсканируйте 'AIM ID' → 'Enable'",
            "Отсканируйте 'Exit Setup'"
        ),
        features = listOf(
            Feature("Определение типа кода", true),
            Feature("Стандарт ISO/IEC", true),
            Feature("Кириллица", false)
        )
    ),
    HID_WITH_CODE_ID(
        displayName = "HID с Code ID",
        description = "HID режим с простым идентификатором кода",
        setupSteps = listOf(
            "Отсканируйте 'Enter Setup'",
            "Отсканируйте 'Bluetooth HID Mode'",
            "Отсканируйте 'Code ID' → 'Enable'",
            "Отсканируйте 'Exit Setup'"
        ),
        features = listOf(
            Feature("Компактный ID", true),
            Feature("Быстрая обработка", true),
            Feature("Кириллица", false)
        )
    ),
    HID_HEX_MODE(
        displayName = "HID HEX режим",
        description = "Передача данных в HEX формате для поддержки кириллицы",
        setupSteps = listOf(
            "Отсканируйте 'Enter Setup'",
            "Отсканируйте 'Bluetooth HID Mode'",
            "Отсканируйте 'Data Format' → 'Hex String'",
            "Отсканируйте 'Prefix' → '\\x'",
            "Отсканируйте 'Exit Setup'"
        ),
        features = listOf(
            Feature("Поддержка кириллицы", true),
            Feature("Универсальная кодировка", true),
            Feature("Увеличенный размер данных", false)
        )
    ),
    SPP_MODE(
        displayName = "SPP режим",
        description = "Serial Port Profile для полной поддержки всех символов",
        setupSteps = listOf(
            "Отсканируйте 'Enter Setup'",
            "Отсканируйте 'SPP Mode'",
            "Отсканируйте 'Character Set' → 'UTF-8'",
            "Отсканируйте 'Exit Setup'"
        ),
        features = listOf(
            Feature("Полная поддержка UTF-8", true),
            Feature("Кириллица", true),
            Feature("Требует SPP подключение", false),
            Feature("Не работает как клавиатура", false)
        )
    )
}

data class Feature(
    val name: String,
    val supported: Boolean
)

data class TestResult(
    val timestamp: String,
    val mode: ScannerMode,
    val input: String,
    val output: String,
    val success: Boolean,
    val details: String? = null
)