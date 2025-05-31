package com.example.myprinterapp.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myprinterapp.scanner.BleConnectionState

/**
 * Диалог для сопряжения BLE сканера с отображением QR-кода
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BlePairingDialog(
    connectionState: BleConnectionState,
    qrBitmap: Bitmap?,
    onDismiss: () -> Unit,
    onStartPairing: () -> Unit,
    onStopPairing: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Сопряжение BLE сканера",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider()

                // Контент в зависимости от состояния
                AnimatedContent(
                    targetState = connectionState,
                    transitionSpec = {
                        fadeIn() + expandVertically() with fadeOut() + shrinkVertically()
                    }
                ) { state ->
                    when (state) {
                        BleConnectionState.DISCONNECTED -> {
                            DisconnectedContent(onStartPairing)
                        }

                        BleConnectionState.PAIRING -> {
                            PairingContent(qrBitmap)
                        }

                        BleConnectionState.CONNECTING -> {
                            ConnectingContent()
                        }

                        BleConnectionState.CONNECTED -> {
                            ConnectedContent(onDismiss)
                        }

                        BleConnectionState.ERROR -> {
                            ErrorContent(onStartPairing)
                        }
                    }
                }

                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (connectionState == BleConnectionState.PAIRING) {
                        OutlinedButton(
                            onClick = onStopPairing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Cancel, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Отмена")
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = connectionState != BleConnectionState.CONNECTING
                    ) {
                        Text(
                            if (connectionState == BleConnectionState.CONNECTED)
                                "Готово"
                            else
                                "Закрыть"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisconnectedContent(onStartPairing: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Filled.BluetoothDisabled,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            "Сканер не подключен",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Text(
            "Для работы с Newland HR32-BT в BLE режиме необходимо выполнить сопряжение",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onStartPairing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.QrCode2, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Начать сопряжение")
        }
    }
}

@Composable
private fun PairingContent(qrBitmap: Bitmap?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (qrBitmap != null) {
            // QR-код
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR-код для сопряжения",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Инструкции
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InstructionStep(1, "Включите сканер HR32-BT")
                    InstructionStep(2, "Наведите сканер на QR-код")
                    InstructionStep(3, "Нажмите кнопку сканирования")
                    InstructionStep(4, "Дождитесь подключения")
                }
            }

            // Индикатор ожидания
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Ожидание подключения сканера...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Загрузка QR-кода
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Text(
                "Генерация QR-кода...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ConnectingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 4.dp
        )

        Text(
            "Подключение к сканеру...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Text(
            "Пожалуйста, подождите",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConnectedContent(onDismiss: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF4CAF50)
        )

        Text(
            "Сканер успешно подключен!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )

        Text(
            "Теперь вы можете использовать сканер для считывания QR-кодов",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Тестовая зона
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Попробуйте отсканировать любой QR-код",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            "Ошибка подключения",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.error
        )

        Text(
            "Не удалось подключиться к сканеру. Убедитесь, что сканер включен и находится рядом.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Filled.Refresh, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Повторить попытку")
        }
    }
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
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
                    number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}