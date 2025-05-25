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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Поле ввода, оптимизированное для работы с Bluetooth-сканером
 *
 * Сканер HR32-BT в HID режиме эмулирует клавиатуру, поэтому
 * отсканированные данные приходят как обычный ввод с клавиатуры
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
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    // Автофокус при первом показе
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    // Обработка завершения сканирования
    // HR32-BT по умолчанию добавляет Enter после кода
    LaunchedEffect(value) {
        if (value.isNotEmpty() && value.endsWith('\n')) {
            val cleanedValue = value.trimEnd()
            if (cleanedValue.isNotEmpty()) {
                onScanComplete(cleanedValue)
                if (clearAfterScan) {
                    onValueChange("")
                }
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
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
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Очистить")
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                if (value.isNotEmpty()) {
                    onScanComplete(value)
                    if (clearAfterScan) {
                        onValueChange("")
                    }
                }
                focusManager.clearFocus()
            }
        ),
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = if (isConnected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error,
            unfocusedBorderColor = if (isConnected)
                MaterialTheme.colorScheme.outline
            else
                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        )
    )
}