package com.example.myprinterapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprinterapp.ui.theme.WarmYellow

// Утилита для затемнения цвета
private fun Color.darker(factor: Float = 0.8f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}

/**
 * Экран "Комплектация":
 * - Сканирование QR или ввод кода детали
 * - Указание количества для комплектации
 * - Подтверждение комплектации (с сохранением в журнал)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickScreen(
    scannedValue: String?,
    quantityToPick: String,
    onScanWithScanner: () -> Unit,
    onScanWithCamera: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onConfirmPick: () -> Unit,
    onBack: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline.darker(0.8f)
    val buttonBorder = BorderStroke(1.dp, borderColor)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Комплектация",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            "Назад",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Выберите способ сканирования:",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp
            )

            // Способы сканирования
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onScanWithScanner,
                    modifier = Modifier.weight(1f).height(100.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    border = buttonBorder
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.BluetoothSearching,
                            "Сканер Bluetooth",
                            modifier = Modifier.size(40.dp)
                        )
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
                        Icon(
                            Icons.Filled.CameraAlt,
                            "Камера",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Камера", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Отображение кода детали
            OutlinedTextField(
                value = scannedValue ?: "QR не отсканирован",
                onValueChange = {},
                label = { Text("Код детали", fontSize = 18.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.QrCodeScanner,
                        "QR-код",
                        modifier = Modifier.size(32.dp)
                    )
                },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                minLines = 1,
                maxLines = 2,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    unfocusedBorderColor = borderColor,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Ввод количества
            OutlinedTextField(
                value = quantityToPick,
                onValueChange = { new ->
                    if (new.all { it.isDigit() }) onQuantityChange(new)
                },
                label = { Text("Количество к комплектованию", fontSize = 18.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Numbers,
                        "Количество",
                        modifier = Modifier.size(32.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    unfocusedBorderColor = borderColor,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(60.dp),
                    border = buttonBorder
                ) {
                    Icon(Icons.Filled.ArrowBack, null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Назад", fontSize = 18.sp)
                }
                Button(
                    onClick = onConfirmPick,
                    enabled = scannedValue != null && quantityToPick.isNotBlank(),
                    modifier = Modifier.weight(1f).height(60.dp),
                    border = buttonBorder
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Подтвердить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun PickScreenPreview() {
    MaterialTheme {
        PickScreen(
            scannedValue = "PN-APPLE-01",
            quantityToPick = "5",
            onScanWithScanner = {},
            onScanWithCamera = {},
            onQuantityChange = {},
            onConfirmPick = {},
            onBack = {}
        )
    }
}