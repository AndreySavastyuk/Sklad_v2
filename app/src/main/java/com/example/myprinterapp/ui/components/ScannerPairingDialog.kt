package com.example.myprinterapp.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ScannerPairingDialog(
    onDismiss: () -> Unit,
    onConnectManual: () -> Unit = {},
    onConnectQr: () -> Unit = {}
) {
    var selectedMode by remember { mutableStateOf(ConnectionMode.QR) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Заголовок
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Подключение сканера",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Описание
                Text(
                    text = "Выберите способ подключения внешнего сканера к планшету",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Переключатель режимов
                SegmentedButtonRow(
                    selectedMode = selectedMode,
                    onModeChange = { selectedMode = it }
                )
                
                // Контент в зависимости от режима
                when (selectedMode) {
                    ConnectionMode.QR -> QrConnectionContent()
                    ConnectionMode.MANUAL -> ManualConnectionContent()
                }
                
                // Кнопки
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
                        onClick = {
                            when (selectedMode) {
                                ConnectionMode.QR -> onConnectQr()
                                ConnectionMode.MANUAL -> onConnectManual()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (selectedMode == ConnectionMode.QR) Icons.Filled.QrCode2 else Icons.Filled.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedMode == ConnectionMode.QR) "Готово" else "Подключить")
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedButtonRow(
    selectedMode: ConnectionMode,
    onModeChange: (ConnectionMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SegmentedButton(
            selected = selectedMode == ConnectionMode.QR,
            onClick = { onModeChange(ConnectionMode.QR) },
            icon = Icons.Filled.QrCode2,
            text = "QR сопряжение",
            modifier = Modifier.weight(1f)
        )
        
        SegmentedButton(
            selected = selectedMode == ConnectionMode.MANUAL,
            onClick = { onModeChange(ConnectionMode.MANUAL) },
            icon = Icons.Filled.Settings,
            text = "Ручная настройка",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 14.sp)
    }
}

@Composable
private fun QrConnectionContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // QR код область
        Card(
            modifier = Modifier.size(200.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Заглушка QR кода - в реальной версии здесь будет настоящий QR
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCode2,
                        contentDescription = "QR код",
                        modifier = Modifier.size(120.dp),
                        tint = Color.Black
                    )
                    Text(
                        "QR Код",
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }
            }
        }
        
        // Инструкции
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Инструкция по подключению:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                InstructionStep(
                    number = "1",
                    text = "Включите сканер и активируйте режим сопряжения"
                )
                
                InstructionStep(
                    number = "2", 
                    text = "Отсканируйте QR код выше сканером"
                )
                
                InstructionStep(
                    number = "3",
                    text = "Дождитесь звукового сигнала подтверждения"
                )
            }
        }
    }
}

@Composable
private fun ManualConnectionContent() {
    var scannerType by remember { mutableStateOf(ScannerType.NEWLAND) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Выберите тип сканера:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        // Типы сканеров
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScannerTypeOption(
                selected = scannerType == ScannerType.NEWLAND,
                onClick = { scannerType = ScannerType.NEWLAND },
                title = "Newland BLE",
                description = "Сканеры серии MT90, NLS-MT9X"
            )
            
            ScannerTypeOption(
                selected = scannerType == ScannerType.ONSEMI,
                onClick = { scannerType = ScannerType.ONSEMI },
                title = "OnSemi BLE",
                description = "Промышленные сканеры OnSemi"
            )
            
            ScannerTypeOption(
                selected = scannerType == ScannerType.GENERIC,
                onClick = { scannerType = ScannerType.GENERIC },
                title = "Универсальный",
                description = "Другие BLE сканеры"
            )
        }
        
        Text(
            "После выбора типа нажмите 'Подключить' для поиска доступных устройств",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InstructionStep(
    number: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ScannerTypeOption(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    description: String
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private enum class ConnectionMode {
    QR, MANUAL
}

private enum class ScannerType {
    NEWLAND, ONSEMI, GENERIC
} 