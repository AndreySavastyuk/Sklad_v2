package com.example.myprinterapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.system.exitProcess

// Утилита для затемнения цвета
private fun Color.darker(factor: Float = 0.8f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    onReceiveClick: () -> Unit,
    onPickClick: () -> Unit,
    onJournalClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExpressConnectionClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    val borderColor = MaterialTheme.colorScheme.outline.darker(0.8f)
    val buttonBorder = BorderStroke(1.dp, borderColor)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Управление складом",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Кнопка экспресс-подключения в углу
                    IconButton(
                        onClick = onExpressConnectionClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.Speed,
                            contentDescription = "Экспресс-подключение",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Кнопка выхода в углу
                    IconButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.ExitToApp,
                            contentDescription = "Выход",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Секция с логотипом
            Icon(
                imageVector = Icons.Filled.Warehouse,
                contentDescription = "Логотип приложения",
                modifier = Modifier
                    .size(160.dp)
                    .padding(bottom = 48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // Первая кнопка - Экспресс-подключение (выделенная)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                StartScreenButton(
                    text = "⚡ Экспресс-подключение",
                    onClick = onExpressConnectionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    border = null,
                    icon = Icons.Filled.Speed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопки меню
            val buttonModifier = Modifier
                .fillMaxWidth()
                .height(72.dp)

            StartScreenButton(
                text = "Приемка продукции",
                onClick = onReceiveClick,
                modifier = buttonModifier,
                border = buttonBorder,
                icon = Icons.Filled.Inventory
            )

            Spacer(modifier = Modifier.height(20.dp))

            StartScreenButton(
                text = "Комплектация заказа",
                onClick = onPickClick,
                modifier = buttonModifier,
                border = buttonBorder,
                icon = Icons.Filled.ShoppingCart
            )

            Spacer(modifier = Modifier.height(20.dp))

            StartScreenButton(
                text = "Журнал операций",
                onClick = onJournalClick,
                modifier = buttonModifier,
                border = buttonBorder,
                icon = Icons.Filled.History,
                enabled = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Вторая строка кнопок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StartScreenButton(
                    text = "Настройки",
                    onClick = onSettingsClick,
                    modifier = Modifier.weight(1f).height(72.dp),
                    border = buttonBorder,
                    icon = Icons.Filled.Settings
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Дополнительная кнопка выхода внизу
            OutlinedButton(
                onClick = { showExitDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Выход из приложения",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Диалог подтверждения выхода
    if (showExitDialog) {
        ExitAppDialog(
            onConfirm = {
                // Закрываем приложение
                (context as? android.app.Activity)?.finishAndRemoveTask()
                    ?: exitProcess(0)
            },
            onDismiss = { showExitDialog = false }
        )
    }
}

@Composable
private fun StartScreenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.darker(0.9f),
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = border,
        colors = colors,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun StartScreenPreview() {
    MaterialTheme {
        StartScreen(
            onReceiveClick = {},
            onPickClick = {},
            onJournalClick = {},
            onSettingsClick = {}
        )
    }
}