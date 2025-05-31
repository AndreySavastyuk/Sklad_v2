package com.example.myprinterapp.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myprinterapp.scanner.NewlandConnectionState
import com.example.myprinterapp.scanner.QrGenerationState

/**
 * Диалог для сопряжения со сканером Newland HR32-BT через QR-код
 * Использует проверенный подход из мини-приложения
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ImprovedBlePairingDialog(
    qrBitmap: Bitmap?,
    qrGenerationState: QrGenerationState,
    connectionState: NewlandConnectionState,
    connectedDevice: String?,
    onGenerateQr: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            // Разрешаем закрытие только если не в процессе подключения
            if (connectionState != NewlandConnectionState.CONNECTING) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = connectionState != NewlandConnectionState.CONNECTING,
            dismissOnClickOutside = connectionState != NewlandConnectionState.CONNECTING
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.BluetoothSearching,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "BLE сопряжение",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Кнопка закрытия
                    if (connectionState != NewlandConnectionState.CONNECTING) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Закрыть",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Контент в зависимости от состояния
                AnimatedContent(
                    targetState = Pair(connectionState, qrGenerationState),
                    transitionSpec = {
                        slideInVertically { it } + fadeIn() with
                                slideOutVertically { -it } + fadeOut()
                    }
                ) { (connState, qrState) ->
                    when (connState) {
                        NewlandConnectionState.DISCONNECTED -> {
                            DisconnectedContent(
                                qrBitmap = qrBitmap,
                                qrGenerationState = qrState,
                                onGenerateQr = onGenerateQr
                            )
                        }

                        NewlandConnectionState.CONNECTING -> {
                            ConnectingContent()
                        }

                        NewlandConnectionState.CONNECTED -> {
                            ConnectedContent(
                                deviceName = connectedDevice ?: "HR32-BT",
                                onDismiss = onDismiss
                            )
                        }

                        NewlandConnectionState.ERROR -> {
                            ErrorContent(
                                onRetry = onGenerateQr
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisconnectedContent(
    qrBitmap: Bitmap?,
    qrGenerationState: QrGenerationState,
    onGenerateQr: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (qrGenerationState) {
            QrGenerationState.IDLE -> {
                // Инструкция и кнопка генерации
                Icon(
                    Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    "Подключение HR32-BT по BLE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

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
                        InstructionStep(2, "Убедитесь, что он в HID режиме")
                        InstructionStep(3, "Нажмите кнопку ниже")
                        InstructionStep(4, "Отсканируйте QR-код сканером")
                        InstructionStep(5, "Сканер переключится в BLE режим")
                    }
                }

                Button(
                    onClick = onGenerateQr,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Filled.QrCode2, null, Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Создать QR для сопряжения", fontSize = 16.sp)
                }
            }

            QrGenerationState.GENERATING -> {
                // Индикатор генерации
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "Генерация QR-кода...",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            QrGenerationState.READY -> {
                // Отображение QR-кода
                qrBitmap?.let { bitmap ->
                    Text(
                        "Отсканируйте QR-код сканером HR32-BT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // QR-код в рамке
                    Surface(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        color = Color.White,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR код для сопряжения HR32-BT",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Статус ожидания
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Scanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Ожидание сканирования...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Анимированный индикатор
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Подсказка
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "После сканирования сканер автоматически переключится в BLE режим",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2196F3)
                            )
                        }
                    }
                }
            }

            QrGenerationState.ERROR -> {
                ErrorContent(onRetry = onGenerateQr)
            }
        }
    }
}

@Composable
private fun ConnectingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 4.dp
        )

        Text(
            "Подключение к HR32-BT...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Text(
            "Не выключайте сканер",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConnectedContent(
    deviceName: String,
    onDismiss: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        // Анимированная галочка
        Surface(
            shape = CircleShape,
            color = Color(0xFF4CAF50),
            modifier = Modifier.size(80.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }
        }

        Text(
            "Сканер подключен!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.BluetoothConnected,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Column {
                        Text(
                            deviceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "BLE режим • Поддержка UTF-8",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                HorizontalDivider()

                // Преимущества BLE режима
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FeatureRow("✓ Поддержка кириллицы")
                    FeatureRow("✓ Стабильная передача данных")
                    FeatureRow("✓ Не блокирует клавиатуру")
                }
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Готово к работе")
        }
    }
}

@Composable
private fun ErrorContent(
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            "Ошибка подключения",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Text(
            "Не удалось подключиться к сканеру HR32-BT. Проверьте:",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("• Сканер включен и заряжен", style = MaterialTheme.typography.bodySmall)
                Text("• Сканер находится рядом (до 10м)", style = MaterialTheme.typography.bodySmall)
                Text("• QR-код полностью отсканирован", style = MaterialTheme.typography.bodySmall)
                Text("• Bluetooth включен на планшете", style = MaterialTheme.typography.bodySmall)
            }
        }

        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Refresh, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Попробовать снова")
        }
    }
}

@Composable
private fun InstructionStep(
    number: Int,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    number.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FeatureRow(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF4CAF50)
    )
}