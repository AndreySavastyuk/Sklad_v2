package com.example.myprinterapp.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myprinterapp.scanner.BleConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScannerPairingScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BleScannerPairingViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val pairingQrCode by viewModel.pairingQrCode.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    
    LaunchedEffect(connectionState) {
        if (connectionState == BleConnectionState.CONNECTED) {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сопряжение BLE сканера") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (connectionState) {
                BleConnectionState.DISCONNECTED -> {
                    DisconnectedContent(
                        onStartPairing = { viewModel.startPairing() }
                    )
                }
                
                BleConnectionState.CONNECTING -> {
                    LoadingContent(
                        title = "Подключение",
                        subtitle = "Подключение к BLE сканеру..."
                    )
                }
                
                BleConnectionState.GENERATING_QR -> {
                    LoadingContent(
                        title = "Генерация QR-кода",
                        subtitle = "Подготовка к сопряжению..."
                    )
                }
                
                BleConnectionState.WAITING_FOR_SCAN -> {
                    pairingQrCode?.let { qrBitmap ->
                        QrCodeContent(
                            qrBitmap = qrBitmap,
                            onCancel = { viewModel.stopPairing() },
                            onRefresh = { viewModel.refreshPairing() }
                        )
                    }
                }
                
                BleConnectionState.CONNECTED -> {
                    ConnectedContent(
                        device = connectedDevice,
                        onDisconnect = { viewModel.disconnectScanner() }
                    )
                }
                
                BleConnectionState.ERROR -> {
                    ErrorContent(
                        onRetry = { viewModel.startPairing() },
                        onCancel = { viewModel.stopPairing() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DisconnectedContent(
    onStartPairing: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.QrCode,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Сопряжение BLE сканера",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Для подключения BLE сканера необходимо выполнить сопряжение:\n\n" +
                  "1. Нажмите кнопку \"Начать сопряжение\"\n" +
                  "2. На экране появится QR-код\n" +
                  "3. Отсканируйте QR-код вашим Newland сканером\n" +
                  "4. Сканер автоматически подключится",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onStartPairing,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Icon(Icons.Default.QrCode, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Начать сопряжение")
        }
    }
}

@Composable
private fun LoadingContent(
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp)
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QrCodeContent(
    qrBitmap: Bitmap,
    onCancel: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Отсканируйте QR-код сканером",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Card(
            modifier = Modifier.size(280.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
            text = "Включите сканер и отсканируйте QR-код с экрана.\n" +
                  "Сканер автоматически переключится в BLE-режим и подключится.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
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

@Composable
private fun ConnectedContent(
    device: com.example.myprinterapp.data.models.DeviceInfo?,
    onDisconnect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Сканер подключен!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        if (device != null) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Информация об устройстве",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    InfoRow("Название", device.name)
                    InfoRow("Адрес", device.address)
                    device.batteryLevel?.let { battery ->
                        InfoRow("Заряд батареи", "$battery%")
                    }
                }
            }
        }
        
        Text(
            text = "Сканер готов к работе. Теперь вы можете использовать его для сканирования QR-кодов в приложении.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onDisconnect,
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Отключить сканер")
        }
    }
}

@Composable
private fun ErrorContent(
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = "Ошибка сопряжения",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = "Не удалось выполнить сопряжение со сканером.\n\n" +
                  "Возможные причины:\n" +
                  "• Сканер выключен или разряжен\n" +
                  "• Нет разрешений Bluetooth\n" +
                  "• Сканер уже подключен к другому устройству",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Отмена")
            }
            
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            ) {
                Text("Повторить")
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
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
            style = MaterialTheme.typography.bodyMedium
        )
    }
} 