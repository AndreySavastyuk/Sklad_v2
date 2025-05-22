package com.example.myprinterapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward // Для кнопок, если нужно
import androidx.compose.material.icons.filled.Inventory // Альтернативный лого
import androidx.compose.material.icons.filled.Warehouse // Для логотипа
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Утилита для затемнения цвета, если еще не определена глобально
// fun Color.darker(factor: Float = 0.8f): Color {
// return Color(
// red = this.red * factor,
// green = this.green * factor,
// blue = this.blue * factor,
// alpha = this.alpha
// )
// }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    onReceiveClick: () -> Unit,
    onPickClick: () -> Unit,
    onJournalClick: () -> Unit,
    onSettingsClick: () -> Unit,
    logoIcon: ImageVector = Icons.Filled.Warehouse // Параметр для логотипа по умолчанию
) {
    val borderColor = MaterialTheme.colorScheme.outline.darker(0.8f) // Используем функцию из AcceptScreen или определим здесь
    val buttonBorder = BorderStroke(1.dp, borderColor)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Управление складом", // Или название вашего приложения
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) // Слегка прозрачный TopAppBar
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface // Цвет фона Scaffold
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp), // Немного больше горизонтальный padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Центрируем содержимое вертикально
        ) {
            // Секция с логотипом
            Icon(
                imageVector = logoIcon,
                contentDescription = "Логотип приложения",
                modifier = Modifier
                    .size(160.dp) // Большой размер для логотипа
                    .padding(bottom = 48.dp), // Отступ снизу от логотипа
                tint = MaterialTheme.colorScheme.primary // Цвет логотипа
            )

            // Кнопки меню
            val buttonModifier = Modifier
                .fillMaxWidth()
                .height(72.dp) // Увеличенная высота кнопок

            StartScreenButton(
                text = "Приемка продукции",
                onClick = onReceiveClick,
                modifier = buttonModifier,
                border = buttonBorder
            )

            Spacer(modifier = Modifier.height(20.dp)) // Увеличенный отступ

            StartScreenButton(
                text = "Комплектация заказа",
                onClick = onPickClick,
                modifier = buttonModifier,
                border = buttonBorder
            )

            Spacer(modifier = Modifier.height(20.dp))

            StartScreenButton(
                text = "Журнал операций",
                onClick = onJournalClick,
                modifier = Modifier.fillMaxWidth(), //modifier = buttonModifier,
                border = buttonBorder,
                enabled = false // Пример неактивной кнопки
            )

            Spacer(modifier = Modifier.height(20.dp))

            StartScreenButton(
                text = "Настройки",
                onClick = onSettingsClick,
                modifier = buttonModifier,
                border = buttonBorder
            )
        }
    }
}

@Composable
private fun StartScreenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    enabled: Boolean = true,
    icon: ImageVector? = Icons.Filled.ArrowForward // Опциональная иконка для кнопки
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium, // Скругленные углы
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.darker(0.9f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp, // Увеличенный шрифт на кнопках
            fontWeight = FontWeight.Medium
        )
        if (icon != null) {
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// Функция для затемнения цвета (если она не в общем файле)
// Убедитесь, что эта функция доступна в области видимости StartScreen.kt
// Если она уже есть в AcceptScreen.kt и они в одном пакете, можно не дублировать.
// Для Preview может потребоваться ее здесь определить, если Preview изолирован.
private fun Color.darker(factor: Float = 0.8f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}


@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun StartScreenPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) { // Используем темную тему для контраста или вашу обычную
        StartScreen(
            onReceiveClick = {},
            onPickClick = {},
            onJournalClick = {},
            onSettingsClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun StartScreenPreview_CustomLogo() {
    MaterialTheme {
        StartScreen(
            onReceiveClick = {},
            onPickClick = {},
            onJournalClick = {},
            onSettingsClick = {},
            logoIcon = Icons.Filled.Inventory
        )

    }
}