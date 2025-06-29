package com.example.myprinterapp.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class InventoryItem(
    val id: String,
    val partNumber: String,
    val partName: String,
    val quantity: Int,
    val location: String,
    val lastUpdate: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllItemsScreen(
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf("Все") }
    var selectedQuantityRange by remember { mutableStateOf("Все") }
    
    // Демонстрационные данные
    val items = remember {
        listOf(
            InventoryItem("1", "PN-APPLE-01", "Яблоки красные", 150, "A-01", "12.01.2024"),
            InventoryItem("2", "PN-BANANA-02", "Бананы желтые", 89, "A-02", "11.01.2024"),
            InventoryItem("3", "PN-ORANGE-03", "Апельсины", 75, "B-01", "10.01.2024"),
            InventoryItem("4", "PN-GRAPE-04", "Виноград зеленый", 45, "B-02", "09.01.2024"),
            InventoryItem("5", "PN-LEMON-05", "Лимоны", 120, "C-01", "08.01.2024"),
            InventoryItem("6", "PN-TOMATO-06", "Томаты спелые", 30, "A-03", "11.01.2024"),
            InventoryItem("7", "PN-CARROT-07", "Морковь крупная", 200, "B-03", "10.01.2024"),
            InventoryItem("8", "PN-POTATO-08", "Картофель молодой", 300, "C-02", "09.01.2024")
        )
    }
    
    // Фильтрация позиций
    val filteredItems = remember(items, searchQuery, selectedLocation, selectedQuantityRange) {
        items.filter { item ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.partNumber.contains(searchQuery, ignoreCase = true) ||
                item.partName.contains(searchQuery, ignoreCase = true) ||
                item.location.contains(searchQuery, ignoreCase = true)
            }
            
            val matchesLocation = when (selectedLocation) {
                "Зона A" -> item.location.startsWith("A")
                "Зона B" -> item.location.startsWith("B")
                "Зона C" -> item.location.startsWith("C")
                else -> true
            }
            
            val matchesQuantity = when (selectedQuantityRange) {
                "Мало (0-50)" -> item.quantity in 0..50
                "Средне (51-100)" -> item.quantity in 51..100
                "Много (100+)" -> item.quantity > 100
                else -> true
            }
            
            matchesSearch && matchesLocation && matchesQuantity
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "📦 Все позиции",
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
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.8f)
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
                label = { Text("Поиск по артикулу, названию или месту") },
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
                        
                        // Фильтр по зоне
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Зона:",
                                modifier = Modifier.width(80.dp),
                                fontWeight = FontWeight.Medium
                            )
                            
                            val locationOptions = listOf("Все", "Зона A", "Зона B", "Зона C")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                locationOptions.forEach { option ->
                                    FilterChip(
                                        onClick = { selectedLocation = option },
                                        label = { Text(option, fontSize = 12.sp) },
                                        selected = selectedLocation == option
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Фильтр по количеству
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Количество:",
                                modifier = Modifier.width(80.dp),
                                fontWeight = FontWeight.Medium
                            )
                            
                            val quantityOptions = listOf("Все", "Мало (0-50)", "Средне (51-100)", "Много (100+)")
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    quantityOptions.take(2).forEach { option ->
                                        FilterChip(
                                            onClick = { selectedQuantityRange = option },
                                            label = { Text(option, fontSize = 11.sp) },
                                            selected = selectedQuantityRange == option
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    quantityOptions.drop(2).forEach { option ->
                                        FilterChip(
                                            onClick = { selectedQuantityRange = option },
                                            label = { Text(option, fontSize = 11.sp) },
                                            selected = selectedQuantityRange == option
                                        )
                                    }
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
                            Icons.Filled.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            filteredItems.size.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Позиций",
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
                            filteredItems.sumOf { it.quantity }.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Общее кол-во",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            filteredItems.distinctBy { it.location }.size.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Мест",
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
                        "Артикул",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
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
                        "Место",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Список позиций
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.partNumber,
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                item.partName,
                                modifier = Modifier.weight(2f),
                                fontSize = 13.sp
                            )
                            Text(
                                "${item.quantity} шт",
                                modifier = Modifier.weight(0.8f),
                                fontSize = 13.sp,
                                color = if (item.quantity > 50) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                            Text(
                                item.location,
                                modifier = Modifier.weight(0.8f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
} 