package com.example.myprinterapp.ui.transfer

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myprinterapp.data.PickTask
import com.example.myprinterapp.network.ServerState
import com.example.myprinterapp.network.TransferEvent
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiTransferScreen(
    viewModel: WiFiTransferViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onTasksImported: (List<PickTask>) -> Unit
) {
    val serverState by viewModel.serverState.collectAsState()
    val deviceIp by viewModel.deviceIpAddress.collectAsState()
    val receivedTasks by viewModel.receivedTasks.collectAsState()
    val events by viewModel.events.collectAsState()

    // Автоматически показываем сообщения о событиях
    LaunchedEffect(events) {
        events?.let {
            delay(3000)
            viewModel.clearEvent()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Передача заданий по WiFi",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                actions = {
                    // Индикатор состояния сервера
                    ServerStatusChip(serverState)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Карточка с информацией о подключении
            ConnectionInfoCard(
                serverState = serverState,
                deviceIp = deviceIp,
                onStartServer = { viewModel.startServer() },
                onStopServer = { viewModel.stopServer() }
            )

            // Инструкции для пользователя
            InstructionsCard()

            // Полученные задания
            if (receivedTasks.isNotEmpty()) {
                ReceivedTasksSection(
                    tasks = receivedTasks,
                    onImport = {
                        onTasksImported(receivedTasks)
                        viewModel.clearReceivedTasks()
                    },
                    onClear = { viewModel.clearReceivedTasks() }
                )
            }

            // Уведомления о событиях
            AnimatedVisibility(
                visible = events != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                events?.let { event ->
                    EventNotification(event)
                }
            }
        }
    }
}

@Composable
private fun ConnectionInfoCard(
    serverState: ServerState,
    deviceIp: String,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit
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
                    Icons.Filled.Wifi,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Подключение",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider()

            // IP адрес
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "IP адрес терминала:",
                    style = MaterialTheme.typography.bodyLarge
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (deviceIp.isNotEmpty()) deviceIp else "Не определен",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Порт
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Порт:",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "8888",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    fontWeight = FontWeight.Medium
                )
            }

            // Кнопка управления сервером
            Button(
                onClick = {
                    when (serverState) {
                        ServerState.STOPPED -> onStartServer()
                        ServerState.RUNNING -> onStopServer()
                        else -> {}
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = serverState != ServerState.STARTING
            ) {
                when (serverState) {
                    ServerState.STOPPED -> {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Запустить сервер")
                    }
                    ServerState.STARTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Запускается...")
                    }
                    ServerState.RUNNING -> {
                        Icon(Icons.Filled.Stop, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Остановить сервер")
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    "Инструкция",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "1. Убедитесь, что терминал и ПК находятся в одной WiFi сети",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "2. Запустите сервер нажатием кнопки выше",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "3. На ПК используйте программу передачи заданий",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "4. Введите IP адрес терминала и порт 8888",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "5. Отправьте задания с ПК",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ReceivedTasksSection(
    tasks: List<PickTask>,
    onImport: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ),
        border = BorderStroke(2.dp, Color(0xFF4CAF50))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF4CAF50)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Получено заданий: ${tasks.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }

            // Список заданий
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(tasks) { task ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "№${task.id} - ${task.description}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${task.details.size} поз.",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Clear, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Отклонить")
                }

                Button(
                    onClick = onImport,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Save, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Импортировать")
                }
            }
        }
    }
}

@Composable
private fun EventNotification(event: TransferEvent) {
    val (icon, message, containerColor) = when (event) {
        is TransferEvent.ClientConnected -> Triple(
            Icons.Filled.Computer,
            "Подключен клиент: ${event.address}",
            MaterialTheme.colorScheme.primaryContainer
        )
        is TransferEvent.TasksReceived -> Triple(
            Icons.Filled.CloudDownload,
            "Получено заданий: ${event.count}",
            Color(0xFF4CAF50).copy(alpha = 0.2f)
        )
        is TransferEvent.Error -> Triple(
            Icons.Filled.Error,
            event.message,
            MaterialTheme.colorScheme.errorContainer
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ServerStatusChip(state: ServerState) {
    val (icon, text, color) = when (state) {
        ServerState.STOPPED -> Triple(
            Icons.Filled.WifiOff,
            "Остановлен",
            MaterialTheme.colorScheme.error
        )
        ServerState.STARTING -> Triple(
            Icons.Filled.Sync,
            "Запускается",
            MaterialTheme.colorScheme.primary
        )
        ServerState.RUNNING -> Triple(
            Icons.Filled.Wifi,
            "Работает",
            Color(0xFF4CAF50)
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