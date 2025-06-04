package com.example.myprinterapp.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.myprinterapp.data.models.ConnectionState
import com.example.myprinterapp.scanner.BleScannerManager
import com.example.myprinterapp.scanner.BleConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BleScannerSettingsViewModel @Inject constructor(
    private val bleScannerManager: BleScannerManager
) : ViewModel() {
    
    val connectionState = bleScannerManager.connectionState
    val pairingQrCode = bleScannerManager.pairingQrCode
    val connectedDevice = bleScannerManager.connectedDevice
    
    fun connectScanner(onResult: (Boolean, String?) -> Unit) {
        bleScannerManager.connectScanner(onResult)
    }
    
    fun disconnectScanner() {
        bleScannerManager.disconnectScanner()
    }
    
    fun testConnection(onResult: (Boolean, String) -> Unit) {
        bleScannerManager.testConnection(onResult)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScannerSettingsScreen(
    onBack: () -> Unit,
    onNavigateToBlePairing: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BleScannerSettingsViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val pairingQrCode by viewModel.pairingQrCode.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    
    var showTestResult by remember { mutableStateOf<String?>(null) }
    var isTestError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки BLE Сканера") },
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
            // BLE Сканер
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionState) {
                        BleConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                        BleConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (connectionState) {
                                BleConnectionState.CONNECTED -> Icons.Default.Bluetooth
                                BleConnectionState.CONNECTING,
                                BleConnectionState.GENERATING_QR, 
                                BleConnectionState.WAITING_FOR_SCAN -> Icons.Default.BluetoothSearching
                                else -> Icons.Default.BluetoothDisabled
                            },
                            contentDescription = null,
                            tint = when (connectionState) {
                                BleConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                                BleConnectionState.ERROR -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = "BLE Сканер",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Text(
                        text = when (connectionState) {
                            BleConnectionState.CONNECTED -> "Подключен и готов к работе"
                            BleConnectionState.CONNECTING -> "Подключение..."
                            BleConnectionState.GENERATING_QR -> "Генерация QR-кода..."
                            BleConnectionState.WAITING_FOR_SCAN -> "Ожидание сканирования QR-кода"
                            BleConnectionState.ERROR -> "Ошибка подключения"
                            BleConnectionState.DISCONNECTED -> "Не подключен"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (connectionState == BleConnectionState.ERROR) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    connectedDevice?.let { device ->
                        Text(
                            text = "${device.name} (${device.address})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // QR-код для сопряжения
                    pairingQrCode?.let { qrBitmap ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Отсканируйте QR-код сканером для подключения:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR-код для сопряжения",
                                    modifier = Modifier.size(200.dp)
                                )
                            }
                        }
                    }
                    
                    // Кнопки управления
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (connectionState) {
                            BleConnectionState.DISCONNECTED, BleConnectionState.ERROR -> {
                                Button(
                                    onClick = {
                                        viewModel.connectScanner { success, message ->
                                            showTestResult = if (success) {
                                                "Подключение установлено"
                                            } else {
                                                message ?: "Ошибка подключения"
                                            }
                                            isTestError = !success
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Подключить")
                                }
                            }
                            BleConnectionState.CONNECTED -> {
                                Button(
                                    onClick = {
                                        viewModel.testConnection { success, message ->
                                            showTestResult = message
                                            isTestError = !success
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Тест")
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        viewModel.disconnectScanner()
                                        showTestResult = "Отключено"
                                        isTestError = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Отключить")
                                }
                            }
                            else -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
            
            // Результат тестирования
            showTestResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTestError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isTestError) Icons.Default.Error else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isTestError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isTestError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { showTestResult = null }) {
                            Icon(Icons.Default.Close, "Закрыть")
                        }
                    }
                }
            }
        }
    }
} 