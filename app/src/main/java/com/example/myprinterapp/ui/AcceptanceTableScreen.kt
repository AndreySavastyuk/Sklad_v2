package com.example.myprinterapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.viewmodel.AcceptanceRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptanceTableScreen(
    acceptanceOperations: List<AcceptanceRecord>,
    onBack: () -> Unit,
    onNavigateToAccept: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedDateRange by remember { mutableStateOf("Все") }
    var selectedStatus by remember { mutableStateOf("Все") }
    
    // Фильтрация операций
    val filteredOperations = remember(acceptanceOperations, searchQuery, selectedDateRange, selectedStatus) {
        acceptanceOperations.filter { operation ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                operation.partNumber.contains(searchQuery, ignoreCase = true) ||
                operation.partName.contains(searchQuery, ignoreCase = true) ||
                operation.orderNumber.contains(searchQuery, ignoreCase = true) ||
                operation.cellCode.contains(searchQuery, ignoreCase = true)
            }
            
            val matchesDate = when (selectedDateRange) {
                "Сегодня" -> {
                    val today = System.currentTimeMillis()
                    val dayStart = today - (today % (24 * 60 * 60 * 1000))
                    operation.timestamp >= dayStart
                }
                "Неделя" -> operation.timestamp >= (System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)
                "Месяц" -> operation.timestamp >= (System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000)
                else -> true
            }
            
            matchesSearch && matchesDate
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "📥 Журнал приемки",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            "Назад",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                actions = {
                    // Кнопка фильтров
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            Icons.Filled.FilterList,
                            "Фильтры",
                            modifier = Modifier.size(28.dp),
                            tint = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Кнопка добавления новой приемки
                    IconButton(onClick = onNavigateToAccept) {
                        Icon(
                            Icons.Filled.Add,
                            "Новая приемка",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3).copy(alpha = 0.8f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAccept,
                containerColor = Color(0xFF2196F3)
            ) {
                Icon(
                    Icons.Filled.QrCodeScanner,
                    contentDescription = "Сканировать QR",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Поиск
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск по артикулу, названию, заказу или ячейке") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Поиск")
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Очистить")
                        }
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Панель фильтров
            if (showFilters) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Фильтры",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Фильтр по дате
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Период:",
                                modifier = Modifier.width(80.dp),
                                fontWeight = FontWeight.Medium
                            )
                            
                            val dateOptions = listOf("Все", "Сегодня", "Неделя", "Месяц")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                dateOptions.forEach { option ->
                                    FilterChip(
                                        onClick = { selectedDateRange = option },
                                        label = { Text(option, fontSize = 12.sp) },
                                        selected = selectedDateRange == option
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Статистика
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatisticItem(
                        value = filteredOperations.size.toString(),
                        label = "Операций",
                        icon = Icons.Filled.Receipt
                    )
                    StatisticItem(
                        value = filteredOperations.sumOf { it.quantity }.toString(),
                        label = "Единиц",
                        icon = Icons.Filled.Inventory
                    )
                    StatisticItem(
                        value = filteredOperations.distinctBy { it.orderNumber }.size.toString(),
                        label = "Заказов",
                        icon = Icons.Filled.Assignment
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredOperations.isEmpty()) {
                // Пустое состояние
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (searchQuery.isNotEmpty()) "Не найдено операций по запросу" else "Нет операций приемки",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Нажмите кнопку сканера для добавления",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Заголовок таблицы
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Время",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp
                        )
                        Text(
                            "Артикул",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.2f),
                            fontSize = 14.sp
                        )
                        Text(
                            "Наименование",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f),
                            fontSize = 14.sp
                        )
                        Text(
                            "Кол-во",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp
                        )
                        Text(
                            "Ячейка",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp
                        )
                        Text(
                            "Статус",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Список операций приемки
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredOperations.sortedByDescending { it.timestamp }) { operation ->
                        AcceptanceOperationCard(operation = operation)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AcceptanceOperationCard(
    operation: AcceptanceRecord
) {
    var showDetails by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(operation.timestamp)),
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    operation.partNumber,
                    modifier = Modifier.weight(1.2f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    operation.partName,
                    modifier = Modifier.weight(2f),
                    fontSize = 13.sp
                )
                Text(
                    "${operation.quantity} шт",
                    modifier = Modifier.weight(0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    operation.cellCode,
                    modifier = Modifier.weight(0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when {
                            hasError -> Icons.Filled.Error
                            isVerified -> Icons.Filled.CheckCircle
                            else -> Icons.Filled.Pending
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = when {
                            hasError -> Color(0xFFE91E63)
                            isVerified -> Color(0xFF4CAF50)
                            else -> Color(0xFFFF9800)
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        when {
                            hasError -> "Ошибка"
                            isVerified -> "Проверено"
                            else -> "Ожидает"
                        },
                        fontSize = 12.sp,
                        color = when {
                            hasError -> Color(0xFFE91E63)
                            isVerified -> Color(0xFF4CAF50)
                            else -> Color(0xFFFF9800)
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Кнопки управления для кладовщика
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Дополнительная информация
                Text(
                    "Заказ: ${operation.orderNumber}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Кнопки действий для кладовщика
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isVerified && !hasError) {
                        // Кнопка подтверждения
                        FilledTonalButton(
                            onClick = { isVerified = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Принять", fontSize = 12.sp)
                        }
                        
                        // Кнопка ошибки
                        OutlinedButton(
                            onClick = { hasError = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE91E63)
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ошибка", fontSize = 12.sp)
                        }
                    }
                    
                    // Кнопка деталей
                    IconButton(
                        onClick = { showDetails = !showDetails },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (showDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Детали",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Детальная информация
            if (showDetails) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            "Подробная информация:",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "ID операции: ${operation.id}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "QR данные: ${operation.qrData}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Время: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.getDefault())
                                .format(java.util.Date(operation.timestamp))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
} 