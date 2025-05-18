package com.example.myprinterapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Экран "Комплектация":
 * - Сканирование QR или ввод кода детали
 * - Указание количества для комплектации
 * - Подтверждение комплектации (с сохранением в журнал)
 */
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
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Комплектация", style = MaterialTheme.typography.headlineSmall)

            // Способы сканирования
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onScanWithScanner, modifier = Modifier.weight(1f)) {
                    Text("Сканер Bluetooth")
                }
                Button(onClick = onScanWithCamera, modifier = Modifier.weight(1f)) {
                    Text("Камера")
                }
            }

            // Отображение кода детали
            OutlinedTextField(
                value = scannedValue.orEmpty(),
                onValueChange = {},
                label = { Text("Код детали") },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )

            // Ввод количества
            OutlinedTextField(
                value = quantityToPick,
                onValueChange = { new -> if (new.all { it.isDigit() }) onQuantityChange(new) },
                label = { Text("Количество к комплектованию") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Назад")
                }
                Button(
                    onClick = onConfirmPick,
                    enabled = scannedValue != null && quantityToPick.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Подтвердить")
                }
            }
        }
    }
}