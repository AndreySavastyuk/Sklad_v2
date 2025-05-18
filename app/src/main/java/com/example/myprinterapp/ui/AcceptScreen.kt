package com.example.myprinterapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.ui.theme.WarmYellow

// Утилита для затемнения цвета (простой вариант)
private fun Color.darker(factor: Float = 0.8f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}

data class ParsedQrData(
    val key: String,
    val value: String,
    val icon: ImageVector
)

fun parseFixedQrValue(scannedValue: String?): List<ParsedQrData> {
    if (scannedValue.isNullOrBlank()) return emptyList()
    val parts = scannedValue.split('=')
    if (parts.size != 4) return emptyList() // Ожидаем ровно 4 части

    return listOf(
        ParsedQrData("Номер маршрутной карты", parts[0], Icons.Filled.ConfirmationNumber),
        ParsedQrData("Номер заказа", parts[1], Icons.Filled.ShoppingCart),
        ParsedQrData("Номер детали", parts[2], Icons.Filled.DataObject),
        ParsedQrData("Название детали", parts[3], Icons.Filled.LabelImportant)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptScreen(
    scannedValue: String?,
    quantity: String,
    cellCode: String,
    onScanWithScanner: () -> Unit,
    onScanWithCamera: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onCellCodeChange: (String) -> Unit,
    onPrintLabel: () -> Unit,
    onResetInputFields: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val parsedData = remember(scannedValue) { parseFixedQrValue(scannedValue) }

    val borderColor = MaterialTheme.colorScheme.outline.darker(0.8f)
    val buttonBorder = BorderStroke(1.dp, borderColor)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Приемка продукции", fontSize = 26.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.ArrowBack, "Назад", modifier = Modifier.size(36.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), // Немного уменьшим для компактности
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Выберите способ сканирования:", style = MaterialTheme.typography.titleLarge, fontSize = 22.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onScanWithScanner,
                    modifier = Modifier.weight(1f).height(100.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    border = buttonBorder
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.BluetoothSearching, "Сканер Bluetooth", modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Сканер BT", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Button(
                    onClick = onScanWithCamera,
                    modifier = Modifier.weight(1f).height(100.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmYellow.darker(0.9f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    border = buttonBorder
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CameraAlt, "Камера", modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Камера", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            OutlinedTextField(
                value = scannedValue ?: "QR не отсканирован",
                onValueChange = {},
                label = { Text("Содержимое QR-кода", fontSize = 18.sp) },
                leadingIcon = { Icon(Icons.Filled.QrCodeScanner, "QR-код", modifier = Modifier.size(32.dp)) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                minLines = 1,
                maxLines = 2,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    unfocusedBorderColor = borderColor,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically // Выравниваем по центру вертикали
            ) {
                LargeInputTextField(
                    value = quantity,
                    onValueChange = onQuantityChange,
                    label = "Кол-во",
                    icon = Icons.Filled.Numbers,
                    labelFontSize = 25.sp, // Уменьшен шрифт label
                    valueFontSize = 40.sp,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                    borderColor = borderColor,
                    labelTextAlign = TextAlign.Center // Выравнивание label по центру
                )
                LargeInputTextField(
                    value = cellCode,
                    onValueChange = { newRaw ->
                        val new = newRaw.filter { it.isLetterOrDigit() } // Фильтруем пробелы и знаки
                        if (new.length <= 4) {
                            onCellCodeChange(new.uppercase()) // Приводим к верхнему регистру для единообразия
                        }
                    },
                    label = "Ячейка хранения",
                    icon = Icons.Filled.Inventory2,
                    labelFontSize = 25.sp, // Уменьшен шрифт label
                    valueFontSize = 35.sp, // Можно сделать чуть меньше для текста
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text, // Обычная клавиатура
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Characters // Автоматически большие буквы
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.weight(1f),
                    borderColor = borderColor,
                    labelTextAlign = TextAlign.Center // Выравнивание label по центру
                )
            }

            Button(
                onClick = {
                    onResetInputFields()
                    focusManager.clearFocus()
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = buttonBorder
            ) {
                Icon(Icons.Filled.Clear, "Сброс", modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(8.dp))
                Text("Сброс", fontSize = 18.sp)
            }

            if (parsedData.isNotEmpty()) {
                Text(
                    "Детализация QR-кода:",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                    modifier = Modifier.padding(top = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(parsedData) { dataItem ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.darker(0.7f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = dataItem.icon,
                                    contentDescription = dataItem.key,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = dataItem.key,
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = dataItem.value,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = parsedData.isEmpty()))

            Button(
                onClick = onPrintLabel,
                enabled = scannedValue != null && quantity.isNotBlank() && cellCode.length > 0 && cellCode.length <= 4, // cellCode может быть не пустым
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = MaterialTheme.shapes.large,
                border = buttonBorder
            ) {
                Icon(Icons.Filled.Print, "Печать бирки", modifier = Modifier.size(42.dp))
                Spacer(Modifier.width(16.dp))
                Text("Печать бирки", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeInputTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    fieldHeight: Dp = 120.dp,
    labelFontSize: TextUnit = 25.sp, // Уменьшен по умолчанию
    valueFontSize: TextUnit = 40.sp,
    borderColor: Color,
    labelTextAlign: TextAlign = TextAlign.Start // Новый параметр для выравнивания label
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(fieldHeight).fillMaxWidth(),
        label = {
            // Обертка для выравнивания label по центру внутри доступного пространства
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(label, fontSize = labelFontSize, textAlign = labelTextAlign)
                } else {
                    // Когда значение есть, Material 3 перемещает label на рамку.
                    // Центральное выравнивание здесь может выглядеть странно,
                    // поэтому стандартное поведение TextField сохраняется.
                    Text(label, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                }
            }
        },
        leadingIcon = { Icon(icon, label, modifier = Modifier.size(30.dp)) },
        textStyle = TextStyle(fontSize = valueFontSize, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = borderColor,
        )
    )
}


@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
fun AcceptScreenPreview_WithChanges() {
    MaterialTheme {
        AcceptScreen(
            scannedValue = "2365=2025/005=НЗ.КШ.040.25.001-01=Корпус",
            quantity = "12",
            cellCode = "АБ12",
            onScanWithScanner = {},
            onScanWithCamera = {},
            onQuantityChange = {},
            onCellCodeChange = {},
            onPrintLabel = {},
            onResetInputFields = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
fun AcceptScreenPreview_Empty_WithChanges() {
    MaterialTheme {
        AcceptScreen(
            scannedValue = null,
            quantity = "",
            cellCode = "",
            onScanWithScanner = {},
            onScanWithCamera = {},
            onQuantityChange = {},
            onCellCodeChange = {},
            onPrintLabel = {},
            onResetInputFields = {},
            onBack = {}
        )
    }
}