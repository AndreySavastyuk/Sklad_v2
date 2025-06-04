package com.example.myprinterapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.myprinterapp.scanner.BleConnectionState
import com.example.myprinterapp.scanner.BleScannerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel для компонента интеграции BLE сканера
 */
@HiltViewModel
class BleScannerIntegrationViewModel @Inject constructor(
    val bleScannerManager: BleScannerManager
) : ViewModel()

/**
 * Компонент для интеграции BLE сканера в экраны приложения
 */
@Composable
fun BleScannerIntegration(
    onScanDataReceived: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BleScannerIntegrationViewModel = hiltViewModel()
) {
    val connectionState by viewModel.bleScannerManager.connectionState.collectAsState()
    val connectedDevice by viewModel.bleScannerManager.connectedDevice.collectAsState()
    val scanResult by viewModel.bleScannerManager.scanResult.collectAsState()
    
    // Обрабатываем результаты сканирования
    LaunchedEffect(scanResult) {
        scanResult?.let { result ->
            onScanDataReceived(result.data)
            viewModel.bleScannerManager.clearScanResult()
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                BleConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                BleConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
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
                imageVector = when (connectionState) {
                    BleConnectionState.CONNECTED -> Icons.Default.Bluetooth
                    BleConnectionState.GENERATING_QR, 
                    BleConnectionState.WAITING_FOR_SCAN -> Icons.Default.BluetoothSearching
                    else -> Icons.Default.BluetoothDisabled
                },
                contentDescription = null,
                tint = when (connectionState) {
                    BleConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                    BleConnectionState.ERROR -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (connectionState) {
                        BleConnectionState.CONNECTED -> "BLE сканер подключен"
                        BleConnectionState.GENERATING_QR -> "Подключение сканера..."
                        BleConnectionState.WAITING_FOR_SCAN -> "Ожидание сканера"
                        BleConnectionState.CONNECTING -> "Подключение сканера..."
                        BleConnectionState.ERROR -> "Ошибка сканера"
                        BleConnectionState.DISCONNECTED -> "Сканер не подключен"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                connectedDevice?.let { device ->
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (connectionState == BleConnectionState.GENERATING_QR || 
                connectionState == BleConnectionState.WAITING_FOR_SCAN) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
} 