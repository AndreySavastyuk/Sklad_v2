package com.example.myprinterapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import android.util.Log

/**
 * Поле ввода, оптимизированное для работы с Bluetooth-сканером
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onScanComplete: (String) -> Unit,
    label: String = "Сканируйте штрих-код",
    placeholder: String = "Наведите сканер и нажмите кнопку",
    isConnected: Boolean = false,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
    clearAfterScan: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    // Упрощенная обработка ввода: если значение не пустое, ждем небольшую задержку.
    // Если значение не изменилось за это время, считаем сканирование завершенным.
    LaunchedEffect(value) {
        if (value.isNotEmpty()) {
            val capturedValue = value
            delay(150) // Задержка для определения конца сканирования (можно настроить)

            if (value == capturedValue && value.isNotEmpty()) {
                Log.d("ScannerInput", "Scan complete detected: '$value'")
                onScanComplete(value.trim()) // Вызываем onScanComplete с очищенным значением
                if (clearAfterScan) {
                    onValueChange("") // Очищаем поле ввода
                }
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange, // Передаем изменения напрямую
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(label)
                if (isConnected) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.BluetoothConnected,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Сканер подключен",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        },
        placeholder = {
            Text(
                if (isConnected) placeholder else "Подключите Bluetooth-сканер"
            )
        },
        leadingIcon = {
            Icon(
                if (isConnected) Icons.Filled.QrCodeScanner else Icons.Filled.BluetoothDisabled,
                contentDescription = null,
                tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) { // Просто очищаем поле
                    Icon(Icons.Filled.Clear, contentDescription = "Очистить")
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text, // Используйте KeyboardType.Text для общей совместимости
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                // При нажатии Done на клавиатуре также считаем ввод завершенным
                if (value.isNotEmpty()) {
                    onScanComplete(value.trim())
                    if (clearAfterScan) {
                        onValueChange("")
                    }
                }
            }
        ),
        singleLine = true
    )
}

