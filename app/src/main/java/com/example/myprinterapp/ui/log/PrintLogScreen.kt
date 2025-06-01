package com.example.myprinterapp.ui.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myprinterapp.viewmodel.PrintLogUiState
import com.example.myprinterapp.viewmodel.PrintLogViewModel
import com.example.myprinterapp.ui.components.ReprintDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintLogScreen(
    onBack: () -> Unit,
    viewModel: PrintLogViewModel = hiltViewModel()
) {
    val printHistory by viewModel.printHistory.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var reprintEntry by remember { mutableStateOf<com.example.myprinterapp.data.db.PrintLogEntry?>(null) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Журнал печати") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is PrintLogUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is PrintLogUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Ошибка загрузки данных",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (uiState as PrintLogUiState.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.retry() }
                        ) {
                            Text("Повторить")
                        }
                    }
                }
                is PrintLogUiState.Success -> {
                    if (printHistory.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Журнал пуст",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Здесь будут отображаться записи о печати этикеток",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(printHistory) { entry ->
                                PrintLogItem(
                                    entry = entry,
                                    onReprintClick = { 
                                        reprintEntry = entry
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Диалог перепечати
    reprintEntry?.let { entry ->
        ReprintDialog(
            entry = entry,
            onDismiss = { reprintEntry = null },
            onConfirm = { quantity, cellCode ->
                viewModel.reprintLabelWithParams(entry, quantity, cellCode)
                reprintEntry = null
            }
        )
    }
}

@Composable
private fun PrintLogItem(
    entry: com.example.myprinterapp.data.db.PrintLogEntry,
    onReprintClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Деталь: ${entry.partNumber}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (entry.partName.isNotBlank()) {
                        Text(
                            text = entry.partName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Количество: ${entry.quantity}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Ячейка: ${entry.location}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (entry.orderNumber?.isNotBlank() == true) {
                        Text(
                            text = "Заказ: ${entry.orderNumber}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Время: ${formatTimestamp(entry.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Статус печати
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Статус: ",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = when (entry.printerStatus) {
                                "SUCCESS" -> "Успешно"
                                "FAILED" -> "Ошибка"
                                else -> entry.printerStatus
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (entry.printerStatus) {
                                "SUCCESS" -> MaterialTheme.colorScheme.primary
                                "FAILED" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    
                    if (entry.errorMessage != null) {
                        Text(
                            text = "Ошибка: ${entry.errorMessage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                Button(
                    onClick = onReprintClick,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Перепечать")
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: java.time.OffsetDateTime): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    return timestamp.format(formatter)
} 