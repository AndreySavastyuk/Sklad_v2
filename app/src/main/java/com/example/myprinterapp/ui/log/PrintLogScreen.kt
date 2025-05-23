package com.example.myprinterapp.ui.log

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myprinterapp.data.db.PrintLogEntry
import com.example.myprinterapp.printer.ConnectionState
import com.example.myprinterapp.printer.LabelData
import com.example.myprinterapp.printer.PrinterService
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

// Утилита для затемнения цвета
private fun Color.darker(factor: Float = 0.8f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintLogScreen(
    vm: PrintLogViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val entries by vm.entries.collectAsState()
    val printerState by vm.printerConnectionState.collectAsState()
    val groupedByDate = remember(entries) {
        entries.groupBy {
            it.dateTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
        }
    }

    var selectedEntry by remember { mutableStateOf<PrintLogEntry?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    val filteredEntries = remember(entries, searchQuery, selectedFilter) {
        entries.filter { entry ->
            val matchesSearch = searchQuery.isEmpty() ||
                    entry.partNumber.contains(searchQuery, ignoreCase = true) ||
                    entry.orderNumber?.contains(searchQuery, ignoreCase = true) == true ||
                    entry.cellCode?.contains(searchQuery, ignoreCase = true) == true

            val matchesFilter = selectedFilter == null || entry.labelType == selectedFilter

            matchesSearch && matchesFilter
        }
    }

    val filteredGroupedByDate = remember(filteredEntries) {
        filteredEntries.groupBy {
            it.dateTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Журнал операций", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Всего записей: ${entries.size}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(36.dp))
                    }
                },
                actions = {
                    // Индикатор принтера
                    PrinterStatusChip(printerState)

                    // Кнопка очистки журнала
                    IconButton(
                        onClick = { /* TODO: Диалог очистки */ },
                        enabled = entries.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Filled.DeleteSweep,
                            contentDescription = "Очистить журнал",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Поиск и фильтры
            SearchAndFilterBar(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                selectedFilter = selectedFilter,
                onFilterChange = { selectedFilter = it },
                filterOptions = entries.map { it.labelType }.distinct()
            )

            if (filteredEntries.isEmpty()) {
                EmptyState(
                    message = if (searchQuery.isNotEmpty() || selectedFilter != null) {
                        "Нет записей, соответствующих критериям поиска"
                    } else {
                        "Журнал операций пуст"
                    },
                    icon = Icons.Filled.ReceiptLong
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    filteredGroupedByDate.forEach { (date, dayEntries) ->
                        @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                        stickyHeader {
                            DateHeader(date = date, count = dayEntries.size)
                        }

                        items(dayEntries, key = { it.id }) { entry ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                PrintLogCard(
                                    entry = entry,
                                    onClick = {
                                        selectedEntry = entry
                                        showEditDialog = true
                                    },
                                    printerConnected = printerState == ConnectionState.CONNECTED
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалог редактирования и печати
    if (showEditDialog && selectedEntry != null) {
        EditAndPrintDialog(
            entry = selectedEntry!!,
            onDismiss = {
                showEditDialog = false
                selectedEntry = null
            },
            onPrint = { newQuantity, newCellCode ->
                vm.reprintLabel(selectedEntry!!, newQuantity, newCellCode)
                showEditDialog = false
                selectedEntry = null
            }
        )
    }
}

@Composable
private fun SearchAndFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: String?,
    onFilterChange: (String?) -> Unit,
    filterOptions: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Поиск
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Поиск по артикулу, заказу, ячейке...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Очистить")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        // Фильтры
        if (filterOptions.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { onFilterChange(null) },
                        label = { Text("Все") },
                        leadingIcon = if (selectedFilter == null) {
                            { Icon(Icons.Filled.Done, contentDescription = null, Modifier.size(18.dp)) }
                        } else null
                    )
                }

                items(filterOptions) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter) },
                        leadingIcon = if (selectedFilter == filter) {
                            { Icon(Icons.Filled.Done, contentDescription = null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun DateHeader(date: String, count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$count операций",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrintLogCard(
    entry: PrintLogEntry,
    onClick: () -> Unit,
    printerConnected: Boolean
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val borderColor = MaterialTheme.colorScheme.outline.darker(0.8f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(enabled = printerConnected, onClick = onClick),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Основная информация
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.DataObject,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = entry.partNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(12.dp))
                    // Тип операции
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = getOperationTypeColor(entry.labelType).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = entry.labelType,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = getOperationTypeColor(entry.labelType)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Детали
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entry.orderNumber != null) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Заказ: ${entry.orderNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(16.dp))
                    }

                    if (entry.quantity != null) {
                        Icon(
                            Icons.Filled.Numbers,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${entry.quantity} шт",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (entry.cellCode != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Ячейка: ${entry.cellCode}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Время и действия
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(IntrinsicSize.Max)
            ) {
                Text(
                    text = entry.dateTime.format(timeFormatter),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (printerConnected) {
                    Spacer(Modifier.height(8.dp))
                    Icon(
                        Icons.Filled.Print,
                        contentDescription = "Печать",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EditAndPrintDialog(
    entry: PrintLogEntry,
    onDismiss: () -> Unit,
    onPrint: (quantity: Int, cellCode: String) -> Unit
) {
    var quantity by remember { mutableStateOf(entry.quantity?.toString() ?: "") }
    var cellCode by remember { mutableStateOf(entry.cellCode ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Print,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Повторная печать этикетки",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Информация о записи
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = entry.partNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (entry.orderNumber != null) {
                            Text(
                                text = "Заказ: ${entry.orderNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Дата: ${entry.dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    "Вы можете изменить количество и ячейку перед печатью:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Поля редактирования
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { new ->
                        if (new.all { it.isDigit() } || new.isEmpty()) {
                            quantity = new
                        }
                    },
                    label = { Text("Количество") },
                    leadingIcon = {
                        Icon(Icons.Filled.Numbers, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cellCode,
                    onValueChange = { new ->
                        val filtered = new.filter { it.isLetterOrDigit() }.take(4).uppercase()
                        cellCode = filtered
                    },
                    label = { Text("Ячейка хранения") },
                    leadingIcon = {
                        Icon(Icons.Filled.Inventory2, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: entry.quantity ?: 1
                    val cell = cellCode.ifEmpty { entry.cellCode ?: "A1" }
                    onPrint(qty, cell)
                }
            ) {
                Icon(Icons.Filled.Print, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Печать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun PrinterStatusChip(state: ConnectionState) {
    val (icon, text, color) = when (state) {
        ConnectionState.CONNECTED -> Triple(
            Icons.Filled.CheckCircle,
            "Принтер подключен",
            Color(0xFF4CAF50)
        )
        ConnectionState.CONNECTING -> Triple(
            Icons.Filled.Sync,
            "Подключение...",
            MaterialTheme.colorScheme.primary
        )
        ConnectionState.DISCONNECTED -> Triple(
            Icons.Filled.PrintDisabled,
            "Принтер отключен",
            MaterialTheme.colorScheme.error
        )
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f),
        modifier = Modifier.padding(end = 8.dp)
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

@Composable
private fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun getOperationTypeColor(type: String): Color {
    return when (type) {
        "Приемка" -> Color(0xFF4CAF50)
        "Приемка (изм.)" -> Color(0xFFFF9800)
        "Комплектация" -> Color(0xFF2196F3)
        "Тест" -> Color(0xFF9C27B0)
        else -> Color(0xFF757575)
    }
}