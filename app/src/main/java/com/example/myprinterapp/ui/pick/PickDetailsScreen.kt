package com.example.myprinterapp.ui.pick

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myprinterapp.data.PickDetail
import com.example.myprinterapp.data.PickTask
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickDetailsScreen(
    task: PickTask?,
    showQtyDialogFor: PickDetail?,
    onShowQtyDialog: (PickDetail) -> Unit,
    onDismissQtyDialog: () -> Unit,
    onSubmitQty: (detailId: Int, quantity: Int) -> Unit,
    onScanAnyCode: (String) -> Unit,
    onBack: () -> Unit,
    onSubmitPickedQty: (detailId: Int, quantity: Int) -> Unit,
    scannedQr: String?
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Детали задания №${task?.id.orEmpty()}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { /* запустить общий скан */ onScanAnyCode.invoke("") }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Сканировать")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (task == null || task.details.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет деталей для отображения.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(task.details, key = { it.id }) { detail ->
                        PickDetailItem(
                            detail = detail,
                            onManualEnterQtyClick = { onShowQtyDialog(detail) }
                        )
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }

            showQtyDialogFor?.let { detailToUpdate ->
                var quantityInput by remember { mutableStateOf(detailToUpdate.picked.toString()) }
                AlertDialog(
                    onDismissRequest = onDismissQtyDialog,
                    title = { Text("Собрать: ${detailToUpdate.partName}") },
                    text = {
                        Column {
                            Text("Артикул: ${detailToUpdate.partNumber}")
                            Text("Ячейка: ${detailToUpdate.location}")
                            Text("Нужно собрать: ${detailToUpdate.quantityToPick}")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = quantityInput,
                                onValueChange = { new -> if (new.all { it.isDigit() }) quantityInput = new },
                                label = { Text("Собранное количество") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val enteredQty = quantityInput.toIntOrNull() ?: 0
                            onSubmitQty(detailToUpdate.id, enteredQty)
                            onDismissQtyDialog()
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismissQtyDialog) { Text("Отмена") }
                    }
                )
            }
        }
    }
}

@Composable
fun PickDetailItem(
    detail: PickDetail,
    onManualEnterQtyClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detail.partName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Артикул: ${detail.partNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Нужно: ${detail.quantityToPick}, Собрано: ${detail.picked}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Ячейка: ${detail.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onManualEnterQtyClick,
            modifier = Modifier.defaultMinSize(minWidth = 100.dp)
        ) {
            Text("Ввод")
        }
    }
}

// Пустой preview можно оставить или удалить по желанию
@Preview(showBackground = true)
@Composable
fun PickDetailsScreenPreview() {
    PickDetailsScreen(
        task = null,
        showQtyDialogFor = null,
        onShowQtyDialog = {},
        onDismissQtyDialog = {},
        onSubmitQty = { _, _ -> },
        onScanAnyCode = {},
        onBack = {},
        onSubmitPickedQty = { _, _ -> },
        scannedQr = null
    )
}
