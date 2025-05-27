package com.example.myprinterapp.ui.demo

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myprinterapp.viewmodel.AcceptViewModel
import com.example.myprinterapp.scanner.ScannerState
import com.example.myprinterapp.ui.components.ScannerInputField
import com.example.myprinterapp.ui.components.ScannerSetupInstructions
import com.example.myprinterapp.ui.components.ScannerDebugDialog
import java.text.SimpleDateFormat
import java.util.*

/**
 * Демонстрационный экран для тестирования функций сканера HR32-BT
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerTestDemo(
    onBack: () -> Unit,
    viewModel: AcceptViewModel = hiltViewModel()
) {
    val scannerState by viewModel.scannerConnectionState.collectAsState()
    val focusManager = LocalFocusManager.current

    var scannerInput by remember { mutableStateOf("") }
    var testResults by remember { mutableStateOf(listOf<TestScanResult>()) }
    var showSetupDialog by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf(false) }

    // Счетчики для статистики
    var totalScans by remember { mutableStateOf(0) }
    var successfulScans by remember { mutableStateOf(0) }
    var failedScans by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Тестирование сканера HR32-BT",
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
                    // Статус подключения
                    ScannerConnectionChip(scannerState)

                    // Меню действий
                    var showMenu by remember { mutableStateOf(false) }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, "Меню")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Настройка сканера") },
                                onClick = {
                                    showSetupDialog = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Settings, null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Отладка") },
                                onClick = {
                                    showDebugDialog = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.BugReport, null) }
                            )

                            Divider()

                            DropdownMenuItem(
                                text = { Text("Очистить результаты") },
                                onClick = {
                                    testResults = emptyList()
                                    totalScans = 0
                                    successfulScans = 0
                                    failedScans = 0
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Clear, null) }
                            )
                        }
                    }
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
            // Статистика
            item {
                StatisticsCard(
                    totalScans = totalScans,
                    successfulScans = successfulScans,
                    failedScans = failedScans
                )
            }

            // Поле ввода для сканера
            item {
                ScanTestInputCard(
                    scannerState = scannerState,
                    scannerInput = scannerInput,
                    onScannerInputChange = { scannerInput = it },
                    onScanComplete = { scannedData ->
                        // Обрабатываем отсканированные данные
                        processScanResult(
                            input = scannedData,
                            onResult = { result ->
                                testResults = listOf(result) + testResults.take(9) // Последние 10
                                totalScans++
                                if (result.success) successfulScans++ else failedScans++
                            }
                        )
                        scannerInput = ""
                        focusManager.clearFocus()
                    }
                )
            }

            // Ручной ввод для тестирования
            item {
                ManualTestCard(
                    onManualTest = { input ->
                        processScanResult(
                            input = input,
                            onResult = { result ->
                                testResults = listOf(result) + testResults.take(9)
                                totalScans++
                                if (result.success) successfulScans++ else failedScans++
                            }
                        )
                    }
                )
            }

            // Результаты тестирования
            if (testResults.isNotEmpty()) {
                item {
                    Text(
                        "Результаты тестирования",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(testResults) { result ->
                    TestResultCard(result)
                }
            }
        }
    }

    // Диалоги
    if (showSetupDialog) {
        ScannerSetupInstructions(
            onDismiss = { showSetupDialog = false }
        )
    }

    if (showDebugDialog) {
        ScannerDebugDialog(
            scannerService = viewModel.scannerService,
            onDismiss = { showDebugDialog = false }
        )
    }
}

@Composable
private fun StatisticsCard(
    totalScans: Int,
    successfulScans: Int,
    failedScans: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                    Icons.Filled.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Статистика тестирования",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Всего", totalScans.toString(), MaterialTheme.colorScheme.onSurface)
                StatItem("Успешно", successfulScans.toString(), Color(0xFF4CAF50))
                StatItem("Ошибок", failedScans.toString(), MaterialTheme.colorScheme.error)
                StatItem(
                    "Успех",
                    if (totalScans > 0) "${(successfulScans * 100 / totalScans)}%" else "0%",
                    MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScanTestInputCard(
    scannerState: ScannerState,
    scannerInput: String,
    onScannerInputChange: (String) -> Unit,
    onScanComplete: (String) -> Unit
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
                    Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Тестирование сканера",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            ScannerInputField(
                value = scannerInput,
                onValueChange = onScannerInputChange,
                onScanComplete = onScanComplete,
                label = "Сканируйте любой QR-код или штрих-код",
                placeholder = "Наведите сканер HR32-BT и нажмите кнопку",
                isConnected = scannerState == ScannerState.CONNECTED,
                autoFocus = true,
                clearAfterScan = true
            )

            if (scannerState == ScannerState.DISCONNECTED) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Сканер HR32-BT не подключен. Используйте ручной ввод или подключите сканер.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualTestCard(
    onManualTest: (String) -> Unit
) {
    var manualInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Keyboard,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Ручной ввод тестовых данных",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedTextField(
                value = manualInput,
                onValueChange = { manualInput = it },
                label = { Text("Введите тестовые данные") },
                placeholder = { Text("test=2024/001=PART-123=Тестовая деталь") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (manualInput.isNotEmpty()) {
                            onManualTest(manualInput)
                            manualInput = ""
                        }
                    }
                )
            )

            // Быстрые тестовые примеры
            Text(
                "Быстрые тесты:",
                style = MaterialTheme.typography.labelMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        manualInput = "test=2024/001=PART-123=Test Part"
                    },
                    label = { Text("Латиница", fontSize = 12.sp) }
                )
                AssistChip(
                    onClick = {
                        manualInput = "тест=2024/001=ДЕТАЛЬ-123=Тестовая деталь"
                    },
                    label = { Text("Кириллица", fontSize = 12.sp) }
                )
                AssistChip(
                    onClick = {
                        manualInput = "]Q0test=2024/001=PART-123=Test"
                    },
                    label = { Text("AIM ID", fontSize = 12.sp) }
                )
            }

            Button(
                onClick = {
                    if (manualInput.isNotEmpty()) {
                        onManualTest(manualInput)
                        manualInput = ""
                    }
                },
                enabled = manualInput.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Выполнить тест")
            }
        }
    }
}

@Composable
private fun TestResultCard(result: TestScanResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (result.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (result.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                        contentDescription = null,
                        tint = if (result.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        if (result.success) "Успешно" else "Ошибка",
                        fontWeight = FontWeight.Bold,
                        color = if (result.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    result.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Входные данные
            Text(
                "Вход:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    result.input,
                    modifier = Modifier.padding(8.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }

            // Результат
            Text(
                "Результат:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    result.output,
                    modifier = Modifier.padding(8.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }

            // Детали
            if (result.details.isNotEmpty()) {
                Text(
                    "Детали: ${result.details}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScannerConnectionChip(state: ScannerState) {
    val (icon, text, color) = when (state) {
        ScannerState.CONNECTED -> Triple(
            Icons.Filled.BluetoothConnected,
            "HR32-BT",
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
        color = color.copy(alpha = 0.2f),
        modifier = Modifier.padding(end = 8.dp)
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

// Функция обработки результатов сканирования
private fun processScanResult(
    input: String,
    onResult: (TestScanResult) -> Unit
) {
    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    try {
        // Простая обработка для демонстрации
        val result = when {
            input.isEmpty() -> TestScanResult(
                timestamp = timestamp,
                input = input,
                output = "Пустая строка",
                success = false,
                details = "Входные данные отсутствуют"
            )

            input.startsWith("]") -> {
                val aimId = input.take(3)
                val data = input.drop(3)
                TestScanResult(
                    timestamp = timestamp,
                    input = input,
                    output = "AIM ID: $aimId, Данные: $data",
                    success = true,
                    details = "Обнаружен AIM ID формат"
                )
            }

            input.contains("=") -> {
                val parts = input.split("=")
                TestScanResult(
                    timestamp = timestamp,
                    input = input,
                    output = "Структурированные данные: ${parts.size} частей",
                    success = true,
                    details = "Части: ${parts.joinToString(", ")}"
                )
            }

            input.any { it in '\u0400'..'\u04FF' } -> {
                TestScanResult(
                    timestamp = timestamp,
                    input = input,
                    output = "Кириллица обнаружена успешно",
                    success = true,
                    details = "UTF-8 декодирование работает корректно"
                )
            }

            else -> TestScanResult(
                timestamp = timestamp,
                input = input,
                output = "Обычный текст: $input",
                success = true,
                details = "Базовое ASCII декодирование"
            )
        }

        onResult(result)

    } catch (e: Exception) {
        onResult(
            TestScanResult(
                timestamp = timestamp,
                input = input,
                output = "Ошибка обработки: ${e.message}",
                success = false,
                details = e.javaClass.simpleName
            )
        )
    }
}

// Модель данных для результата теста
data class TestScanResult(
    val timestamp: String,
    val input: String,
    val output: String,
    val success: Boolean,
    val details: String
)