package com.example.myprinterapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.viewmodel.AcceptanceRecord
import java.time.format.DateTimeFormatter

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
            Icon(
                Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Редактирование приемки",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Информация о детали
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row {
                            Icon(
                                Icons.Filled.DataObject,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                record.partNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            record.partName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row {
                            Icon(
                                Icons.Filled.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Заказ: ${record.orderNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            Icon(
                                Icons.Filled.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Принято: ${record.acceptedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Поля для редактирования
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

                // Предупреждение
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "После сохранения изменений будет напечатана новая этикетка с обновленными данными",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toIntOrNull()
                    if (qty != null && qty > 0 && cellCode.isNotEmpty()) {
                        onConfirm(qty, cellCode)
                    }
                },
                enabled = quantity.toIntOrNull()?.let { it > 0 } == true && cellCode.isNotEmpty()
            ) {
                Icon(Icons.Filled.Print, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Сохранить и печать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}