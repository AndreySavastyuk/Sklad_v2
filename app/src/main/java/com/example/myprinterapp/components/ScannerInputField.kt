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
import kotlinx.coroutines.delay
import android.util.Log

/**
 * Поле ввода, оптимизированное для работы с Bluetooth-сканером
 * с поддержкой кириллицы
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

    // Буфер для накопления символов
    var buffer by remember { mutableStateOf("") }
    var lastInputTime by remember { mutableStateOf(0L) }

    // Автофокус при первом показе
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    // Обработка ввода с учетом кириллицы и буферизации
    LaunchedEffect(value) {
        if (value.isNotEmpty() && value != buffer) {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastInput = currentTime - lastInputTime

            // Если прошло менее 50мс с последнего ввода, это часть одного сканирования
            if (timeSinceLastInput < 50) {
                buffer = value
                lastInputTime = currentTime

                // Ждем еще немного для получения полных данных
                delay(100)

                // Проверяем, не изменился ли buffer за это время
                if (buffer == value) {
                    // Обрабатываем накопленные данные
                    processScannedData(buffer, onScanComplete)
                    if (clearAfterScan) {
                        onValueChange("")
                        buffer = ""
                    }
                }
            } else {
                // Новое сканирование
                buffer = value
                lastInputTime = currentTime

                // Для коротких кодов может сработать сразу
                if (value.endsWith('\n') || value.endsWith('\r')) {
                    processScannedData(value, onScanComplete)
                    if (clearAfterScan) {
                        onValueChange("")
                        buffer = ""
                    }
                }
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            Log.d("ScannerInput", "Raw input: $newValue")
            Log.d("ScannerInput", "Bytes: ${newValue.toByteArray().contentToString()}")
            onValueChange(newValue)
        },
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
                IconButton(onClick = {
                    onValueChange("")
                    buffer = ""
                }) {
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
                    processScannedData(value, onScanComplete)
                    if (clearAfterScan) {
                        onValueChange("")
                        buffer = ""
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

/**
 * Обработка отсканированных данных с учетом кириллицы
 */
private fun processScannedData(data: String, onScanComplete: (String) -> Unit) {
    // Очищаем от управляющих символов
    val cleanedData = data.trim('\n', '\r', ' ')

    if (cleanedData.isNotEmpty()) {
        Log.d("ScannerInput", "Processing scanned data: $cleanedData")

        // Пытаемся декодировать кириллицу
        val decodedData = decodeCyrillicIfNeeded(cleanedData)
        Log.d("ScannerInput", "Decoded data: $decodedData")

        onScanComplete(decodedData)
    }
}

/**
 * Декодирование кириллицы, если необходимо
 */
private fun decodeCyrillicIfNeeded(input: String): String {
    // Проверяем, содержит ли строка кириллицу
    if (input.any { it in '\u0400'..'\u04FF' }) {
        // Уже содержит кириллицу, возвращаем как есть
        return input
    }

    // Проверяем на признаки неправильной кодировки
    if (input.contains("?") || input.any { it.code > 127 && it !in '\u0400'..'\u04FF' }) {
        try {
            // Попытка интерпретировать как Windows-1251
            val bytes = input.toByteArray(Charsets.ISO_8859_1)
            val decoded = String(bytes, charset("Windows-1251"))

            // Проверяем, стало ли лучше
            if (decoded.any { it in '\u0400'..'\u04FF' } && !decoded.contains("�")) {
                Log.d("ScannerInput", "Successfully decoded from Windows-1251")
                return decoded
            }
        } catch (e: Exception) {
            Log.e("ScannerInput", "Failed to decode as Windows-1251", e)
        }

        try {
            // Попытка интерпретировать как UTF-8
            val bytes = input.toByteArray()
            val decoded = String(bytes, Charsets.UTF_8)

            if (decoded.any { it in '\u0400'..'\u04FF' } && !decoded.contains("�")) {
                Log.d("ScannerInput", "Successfully decoded as UTF-8")
                return decoded
            }
        } catch (e: Exception) {
            Log.e("ScannerInput", "Failed to decode as UTF-8", e)
        }
    }

    // Возвращаем оригинал, если декодирование не помогло
    return input
}