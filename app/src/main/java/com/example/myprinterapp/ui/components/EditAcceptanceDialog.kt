package com.example.myprinterapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.viewmodel.AcceptanceRecord

@Composable
fun EditAcceptanceDialog(
    record: AcceptanceRecord,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, cellCode: String) -> Unit
) {
    var quantity by remember { mutableStateOf(record.quantity.toString()) }
    var cellCode by remember { mutableStateOf(record.cellCode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.Edit, null, Modifier.size(32.dp))
        },
        title = { Text("Редактировать приемку") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "${record.partNumber} - ${record.partName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                    label = { Text("Количество") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cellCode,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isLetterOrDigit() }.take(4).uppercase()
                        cellCode = filtered
                    },
                    label = { Text("Ячейка") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: record.quantity
                    onConfirm(qty, cellCode)
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}