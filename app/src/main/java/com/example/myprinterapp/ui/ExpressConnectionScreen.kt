package com.example.myprinterapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myprinterapp.scanner.BleConnectionState
import com.example.myprinterapp.ui.settings.BleScannerSettingsViewModel
import com.example.myprinterapp.ui.settings.SettingsViewModel
import com.example.myprinterapp.ui.settings.SettingsUiState
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressConnectionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    scannerViewModel: BleScannerSettingsViewModel = hiltViewModel()
) {
    val printerUiState by settingsViewModel.uiState.collectAsState()
    val scannerConnectionState by scannerViewModel.connectionState.collectAsState()
    val connectedDevice by scannerViewModel.connectedDevice.collectAsState()
    val pairingQrCode by scannerViewModel.pairingQrCode.collectAsState()
    
    var showSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showErrorMessage by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    
    // ИСПРАВЛЕНИЕ: Автоматически запускаем подключение сканера при входе на экран
    LaunchedEffect(Unit) {
        if (scannerConnectionState == BleConnectionState.DISCONNECTED) {
            scannerViewModel.connectScanner { success, message ->
                if (!success) {
                    showErrorMessage = message ?: "Ошибка автоматического подключения сканера"
                }
            }
        }
    }
    
    // Автоматически скрываем сообщения через 3 секунды
    LaunchedEffect(showSuccessMessage, showErrorMessage) {
        if (showSuccessMessage != null || showErrorMessage != null) {
            delay(3000)
            showSuccessMessage = null
            showErrorMessage = null
        }
    }

    // Показываем QR диалог при ожидании сканирования
    LaunchedEffect(scannerConnectionState) {
        when (scannerConnectionState) {
            BleConnectionState.WAITING_FOR_SCAN -> {
                // Добавляем небольшую задержку для стабильности
                delay(100)
                showQrDialog = pairingQrCode != null
            }
            BleConnectionState.CONNECTED -> {
                showQrDialog = false
            }
            else -> {
                // Не закрываем диалог для других состояний
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Экспресс-подключение",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Заголовок
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(1000)
                    ) + fadeIn(animationSpec = tween(1000))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "Быстрое подключение устройств",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Подключите принтер и сканер одним нажатием",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Карточка принтера
                ExpressDeviceCard(
                    title = "Термопринтер",
                    subtitle = when (val state = printerUiState) {
                        is SettingsUiState.Success -> if (state.isConnected) "Подключен" else "Не подключен"
                        is SettingsUiState.Loading -> "Подключение..."
                        is SettingsUiState.Error -> "Ошибка подключения"
                        is SettingsUiState.PermissionRequired -> "Нужны разрешения"
                        // Компилятор требует эту ветку, хотя она теоретически недостижима
                        else -> "Неизвестный статус"
                    },
                    icon = Icons.Default.Print,
                    isConnected = (printerUiState as? SettingsUiState.Success)?.isConnected == true,
                    isLoading = printerUiState is SettingsUiState.Loading,
                    onConnect = {
                        settingsViewModel.connectPrinter()
                    },
                    onDisconnect = {
                        settingsViewModel.disconnectPrinter()
                    },
                    onTest = {
                        settingsViewModel.printTestLabel()
                    }
                )

                // Карточка сканера
                ExpressDeviceCard(
                    title = "BLE Сканер",
                    subtitle = when (scannerConnectionState) {
                        BleConnectionState.CONNECTED -> "Подключен: ${connectedDevice?.name ?: "Неизвестно"}"
                        BleConnectionState.GENERATING_QR -> "Генерация QR-кода..."
                        BleConnectionState.WAITING_FOR_SCAN -> "Ожидание сканирования"
                        BleConnectionState.ERROR -> "Ошибка подключения"
                        BleConnectionState.DISCONNECTED -> "Не подключен"
                        BleConnectionState.CONNECTING -> "Подключение..."
                        else -> "Неизвестный статус"
                    },
                    icon = Icons.Default.QrCodeScanner,
                    isConnected = scannerConnectionState == BleConnectionState.CONNECTED,
                    isLoading = scannerConnectionState == BleConnectionState.GENERATING_QR || 
                               scannerConnectionState == BleConnectionState.WAITING_FOR_SCAN,
                    onConnect = {
                        scannerViewModel.connectScanner { success, message ->
                            if (success) {
                                showSuccessMessage = "Сканер подключен успешно"
                            } else {
                                showErrorMessage = message ?: "Ошибка подключения сканера"
                            }
                        }
                    },
                    onDisconnect = {
                        scannerViewModel.disconnectScanner()
                    },
                    onTest = {
                        scannerViewModel.testConnection { success, message ->
                            if (success) {
                                showSuccessMessage = message
                            } else {
                                showErrorMessage = message
                            }
                        }
                    }
                )

                // Кнопка "Подключить всё"
                AnimatedVisibility(
                    visible = !isConnecting,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(1000, delayMillis = 500)
                    ) + fadeIn(animationSpec = tween(1000, delayMillis = 500))
                ) {
                    Button(
                        onClick = {
                            isConnecting = true
                            // Подключаем принтер
                            when (val state = printerUiState) {
                                is SettingsUiState.Success -> {
                                    if (!state.isConnected) {
                                        settingsViewModel.connectPrinter()
                                    }
                                }
                                is SettingsUiState.Loading -> {
                                    // Принтер уже подключается, ничего не делаем
                                }
                                is SettingsUiState.Error -> {
                                    settingsViewModel.connectPrinter()
                                }
                                is SettingsUiState.PermissionRequired -> {
                                    settingsViewModel.connectPrinter()
                                }
                            }
                            // Подключаем сканер
                            if (scannerConnectionState != BleConnectionState.CONNECTED) {
                                scannerViewModel.connectScanner { success, message ->
                                    if (success) {
                                        showSuccessMessage = "Все устройства подключены"
                                    } else {
                                        showErrorMessage = "Ошибка подключения: $message"
                                    }
                                    isConnecting = false
                                }
                            } else {
                                isConnecting = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !isConnecting
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        } else {
                            Icon(
                                Icons.Default.FlashOn,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = if (isConnecting) "Подключение..." else "Подключить всё сразу",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Уведомления
            showSuccessMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    ExpressNotification(
                        message = message,
                        isError = false
                    )
                }
            }

            showErrorMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    ExpressNotification(
                        message = message,
                        isError = true
                    )
                }
            }
        }

        // QR диалог для сопряжения сканера
        if (showQrDialog && pairingQrCode != null) {
            QrPairingDialog(
                qrBitmap = pairingQrCode!!,
                onDismiss = { 
                    showQrDialog = false
                    scannerViewModel.disconnectScanner()
                },
                onRefresh = {
                    scannerViewModel.disconnectScanner()
                    scannerViewModel.connectScanner { success, message ->
                        if (!success) {
                            showErrorMessage = message ?: "Ошибка обновления QR-кода"
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ExpressDeviceCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isConnected: Boolean,
    isLoading: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isConnected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedScale),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isConnected) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isConnected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (isConnected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isConnected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                        }
                        
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isConnected) {
                                Color(0xFF4CAF50)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isConnected) {
                    Button(
                        onClick = onTest,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Тест")
                    }
                    
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.LinkOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Отключить")
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isLoading) "Подключение..." else "Подключить")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressNotification(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isError) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QrPairingDialog(
    qrBitmap: android.graphics.Bitmap,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Сопряжение сканера",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Отсканируйте QR-код с экрана вашим BLE сканером",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Card(
                    modifier = Modifier.size(250.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR-код для сопряжения",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
                
                Text(
                    text = "После сканирования сканер автоматически переключится в BLE-режим и подключится к приложению",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Обновить")
                    }
                }
            }
        }
    }
} 