package com.example.myprinterapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ArchiveRecord(
    val id: String,
    val date: String,
    val operation: String,
    val partNumber: String,
    val partName: String,
    val quantity: Int,
    val operator: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedOperation by remember { mutableStateOf("Все") }
    var selectedOperator by remember { mutableStateOf("Все") }
    
    // Демонстрационные данные
    val archiveRecords = remember {
        listOf(
            ArchiveRecord("1", "12.01.2024", "Приемка", "PN-APPLE-01", "Яблоки красные", 50, "Иванов И.И.", "Завершено"),
            ArchiveRecord("2", "11.01.2024", "Отгрузка", "PN-BANANA-02", "Бананы желтые", 25, "Петров П.П.", "Завершено"),
            ArchiveRecord("3", "10.01.2024", "Приемка", "PN-ORANGE-03", "Апельсины", 30, "Сидоров С.С.", "Завершено"),
            ArchiveRecord("4", "09.01.2024", "Отгрузка", "PN-GRAPE-04", "Виноград зеленый", 15, "Иванов И.И.", "Завершено"),
            ArchiveRecord("5", "08.01.2024", "Приемка", "PN-LEMON-05", "Лимоны", 40, "Петров П.П.", "Завершено"),
            ArchiveRecord("6", "07.01.2024", "Комплектация", "PN-TOMATO-06", "Томаты спелые", 20, "Сидоров С.С.", "Завершено"),
            ArchiveRecord("7", "06.01.2024", "Инвентаризация", "PN-CARROT-07", "Морковь крупная", 35, "Иванов И.И.", "Завершено")
        )
    }
    
    // Фильтрация записей
    val filteredRecords = remember(archiveRecords, searchQuery, selectedOperation, selectedOperator) {
        archiveRecords.filter { record ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                record.partNumber.contains(searchQuery, ignoreCase = true) ||
                record.partName.contains(searchQuery, ignoreCase = true) ||
                record.operation.contains(searchQuery, ignoreCase = true) ||
                record.operator.contains(searchQuery, ignoreCase = true)
            }
            
            val matchesOperation = when (selectedOperation) {
                "Приемка" -> record.operation == "Приемка"
                "Отгрузка" -> record.operation == "Отгрузка"
                "Комплектация" -> record.operation == "Комплектация"
                "Инвентаризация" -> record.operation == "Инвентаризация"
                else -> true
            }
            
            val matchesOperator = when (selectedOperator) {
                "Иванов И.И." -> record.operator == "Иванов И.И."
                "Петров П.П." -> record.operator == "Петров П.П."
                "Сидоров С.С." -> record.operator == "Сидоров С.С."
                else -> true
            }
            
            matchesSearch && matchesOperation && matchesOperator
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "🗂️ Архив",
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF9C27B0).copy(alpha = 0.8f)
                )
            )
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
                label = { Text("Поиск по артикулу, операции или оператору") },
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
                        
                        // Фильтр по операции
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Операция:",
                                modifier = Modifier.width(80.dp),
                                fontWeight = FontWeight.Medium
                            )
                            
                            val operationOptions = listOf("Все", "Приемка", "Отгрузка", "Комплектация", "Инвентаризация")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(operationOptions) { option ->
                                    FilterChip(
                                        onClick = { selectedOperation = option },
                                        label = { Text(option, fontSize = 11.sp) },
                                        selected = selectedOperation == option
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Фильтр по оператору
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Оператор:",
                                modifier = Modifier.width(80.dp),
                                fontWeight = FontWeight.Medium
                            )
                            
                            val operatorOptions = listOf("Все", "Иванов И.И.", "Петров П.П.", "Сидоров С.С.")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(operatorOptions) { option ->
                                    FilterChip(
                                        onClick = { selectedOperator = option },
                                        label = { Text(option, fontSize = 11.sp) },
                                        selected = selectedOperator == option
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            filteredRecords.size.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Операций",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Category,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            filteredRecords.sumOf { it.quantity }.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Обработано",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            filteredRecords.distinctBy { it.operator }.size.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Операторов",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
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
                        "Дата",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp
                    )
                    Text(
                        "Операция",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp
                    )
                    Text(
                        "Товар",
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
                        "Оператор",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.2f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Список архивных записей
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredRecords) { record ->
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
                                    record.date,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    record.operation,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = if (record.operation == "Приемка") Color(0xFF4CAF50) else Color(0xFF2196F3)
                                )
                                Column(
                                    modifier = Modifier.weight(2f)
                                ) {
                                    Text(
                                        record.partNumber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        record.partName,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "${record.quantity} шт",
                                    modifier = Modifier.weight(0.8f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    record.operator,
                                    modifier = Modifier.weight(1.2f),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Статус
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    record.status,
                                    fontSize = 12.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 