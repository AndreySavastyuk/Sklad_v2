/* ui/log/PrintLogScreen.kt */
package com.example.myprinterapp.ui.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.format.DateTimeFormatter

@OptIn (ExperimentalMaterial3Api::class)
@Composable
fun PrintLogScreen(
    vm: PrintLogViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val list by vm.entries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Журнал печати") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padd ->
        LazyColumn(
            contentPadding = padd,
            modifier = Modifier.fillMaxSize()
        ) {
            items(list) { entry ->
                PrintLogRow(entry)
            }
        }
    }
}

@Composable
private fun PrintLogRow(entry: com.example.myprinterapp.data.db.PrintLogEntry) {
    val fmt = remember { DateTimeFormatter.ofPattern("dd.MM HH:mm") }
    Column(Modifier
        .fillMaxWidth()
        .padding(12.dp)) {

        Text("${entry.dateTime.format(fmt)}  •  ${entry.labelType}",
            style = MaterialTheme.typography.labelMedium)

        Text("${entry.partNumber}  x${entry.quantity ?: "?"}",
            style = MaterialTheme.typography.bodyLarge)

        entry.cellCode?.let { Text("Ячейка: $it") }
        entry.orderNumber?.let { Text("Заказ:  $it") }
    }
    Divider()
}
