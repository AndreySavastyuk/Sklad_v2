package com.example.myprinterapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.myprinterapp.data.db.PrintLogEntry

@Composable
fun ReprintDialog(
    entry: PrintLogEntry,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, cellCode: String) -> Unit
) {
    var quantity by remember { mutableStateOf(entry.quantity.toString()) }
    var cellCode by remember { mutableStateOf(entry.location) }
    var quantityError by remember { mutableStateOf(false) }
    var cellCodeError by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Print,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Перепечать этикетку",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Divider()
                
                // Информация об оригинальной записи
                InfoSection(
                    title = "Оригинальные данные",
                    content = {
                        InfoRow("Деталь:", entry.partNumber)
                        InfoRow("Название:", entry.partName)
                        if (entry.orderNumber?.isNotBlank() == true) {
                            InfoRow("Заказ:", entry.orderNumber)
                        }
                        InfoRow("Количество:", entry.quantity.toString())
                        InfoRow("Ячейка:", entry.location)
                    }
                )
                
                Divider()
                
                // Поля для редактирования
                InfoSection(
                    title = "Новые параметры",
                    content = {
                        // Поле количества
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { newValue ->
                                quantity = newValue
                                quantityError = newValue.toIntOrNull()?.let { it <= 0 } ?: true
                            },
                            label = { Text("Количество") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = quantityError,
                            supportingText = if (quantityError) {
                                { Text("Введите корректное количество") }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Поле ячейки
                        OutlinedTextField(
                            value = cellCode,
                            onValueChange = { newValue ->
                                cellCode = newValue
                                cellCodeError = newValue.isBlank()
                            },
                            label = { Text("Код ячейки") },
                            isError = cellCodeError,
                            supportingText = if (cellCodeError) {
                                { Text("Введите код ячейки") }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
                
                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    
                    Button(
                        onClick = {
                            val qty = quantity.toIntOrNull()
                            if (qty != null && qty > 0 && cellCode.isNotBlank()) {
                                onConfirm(qty, cellCode)
                            } else {
                                quantityError = qty == null || qty <= 0
                                cellCodeError = cellCode.isBlank()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !quantityError && !cellCodeError && quantity.isNotBlank() && cellCode.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Печать")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Диалог подтверждения для критических операций
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Подтвердить",
    dismissText: String = "Отмена",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

/**
 * Простой диалог с информацией
 */
@Composable
fun InfoDialog(
    title: String,
    message: String,
    buttonText: String = "ОК",
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(buttonText)
            }
        }
    )
} 