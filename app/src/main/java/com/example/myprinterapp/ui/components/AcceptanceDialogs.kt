package com.example.myprinterapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun QuantityInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantity by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Numbers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Количество",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Отображение введенного значения
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    placeholder = {
                        Text(
                            "0",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                )
                
                // Цифровая клавиатура
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Цифры 1-9
                    items((1..9).toList()) { digit ->
                        DigitButton(
                            digit = digit.toString(),
                            onClick = { quantity += digit.toString() }
                        )
                    }
                    
                    // Кнопка очистки
                    item {
                        Button(
                            onClick = { quantity = "" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Очистить",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Цифра 0
                    item {
                        DigitButton(
                            digit = "0",
                            onClick = { quantity += "0" }
                        )
                    }
                    
                    // Кнопка удаления последней цифры
                    item {
                        Button(
                            onClick = { 
                                if (quantity.isNotEmpty()) {
                                    quantity = quantity.dropLast(1)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.Backspace,
                                contentDescription = "Удалить",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                
                // Кнопки управления
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("Отмена", fontSize = 18.sp)
                    }
                    
                    Button(
                        onClick = {
                            val qty = quantity.toIntOrNull() ?: 0
                            if (qty > 0) {
                                onConfirm(qty)
                            }
                        },
                        enabled = quantity.isNotEmpty() && quantity.toIntOrNull() != null && quantity.toInt() > 0,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("ОК", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CellCodeInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var cellCode by remember { mutableStateOf("") }
    var isNumericMode by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Номер ячейки",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Отображение введенного значения
                OutlinedTextField(
                    value = cellCode,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    placeholder = {
                        Text(
                            "A1B2",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                )
                
                // Переключатель режима
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        onClick = { isNumericMode = false },
                        label = { Text("ABC", fontSize = 16.sp) },
                        selected = !isNumericMode,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        onClick = { isNumericMode = true },
                        label = { Text("123", fontSize = 16.sp) },
                        selected = isNumericMode,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Клавиатура
                if (isNumericMode) {
                    // Цифровая клавиатура
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items((1..9).toList()) { digit ->
                            CellKeyButton(
                                text = digit.toString(),
                                onClick = { 
                                    if (cellCode.length < 4) {
                                        cellCode += digit.toString()
                                    }
                                }
                            )
                        }
                        
                        item { Spacer(modifier = Modifier) } // Пустое место
                        
                        item {
                            CellKeyButton(
                                text = "0",
                                onClick = { 
                                    if (cellCode.length < 4) {
                                        cellCode += "0"
                                    }
                                }
                            )
                        }
                        
                        item {
                            Button(
                                onClick = { 
                                    if (cellCode.isNotEmpty()) {
                                        cellCode = cellCode.dropLast(1)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Backspace,
                                    contentDescription = "Удалить",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Буквенная клавиатура
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toList()
                        items(letters) { letter ->
                            CellKeyButton(
                                text = letter.toString(),
                                onClick = { 
                                    if (cellCode.length < 4) {
                                        cellCode += letter.toString()
                                    }
                                }
                            )
                        }
                        
                        // Два пустых места
                        item { Spacer(modifier = Modifier) }
                        item { Spacer(modifier = Modifier) }
                        
                        // Кнопка удаления
                        item {
                            Button(
                                onClick = { 
                                    if (cellCode.isNotEmpty()) {
                                        cellCode = cellCode.dropLast(1)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Backspace,
                                    contentDescription = "Удалить",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        // Кнопка очистки
                        item {
                            Button(
                                onClick = { cellCode = "" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = "Очистить",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                
                // Кнопки управления
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("Отмена", fontSize = 18.sp)
                    }
                    
                    Button(
                        onClick = {
                            if (cellCode.isNotEmpty()) {
                                onConfirm(cellCode)
                            }
                        },
                        enabled = cellCode.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("ОК", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitButton(
    digit: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = digit,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun CellKeyButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
} 