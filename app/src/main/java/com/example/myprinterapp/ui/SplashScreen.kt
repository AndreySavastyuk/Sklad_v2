package com.example.myprinterapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3000) // Показываем splash 3 секунды
        onNavigateToMain()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1565C0), // Ярко-синий
                        Color(0xFF1976D2), // Синий
                        Color(0xFF2196F3), // Материал синий
                        Color(0xFF42A5F5), // Светло-синий
                        Color(0xFF90CAF9)  // Очень светло-синий
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Анимированные частицы на фоне
        AnimatedParticles(startAnimation = startAnimation)
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Анимированный логотип
            AnimatedLogo(startAnimation = startAnimation)
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Название приложения
            AnimatedVisibility(
                visible = startAnimation,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(1200, delayMillis = 500)
                ) + fadeIn(animationSpec = tween(1200, delayMillis = 500))
            ) {
                Text(
                    text = "Управление складом",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Подзаголовок
            AnimatedVisibility(
                visible = startAnimation,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(1200, delayMillis = 800)
                ) + fadeIn(animationSpec = tween(1200, delayMillis = 800))
            ) {
                Text(
                    text = "Система учета и маркировки товаров",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(60.dp))
            
            // Анимированный индикатор загрузки
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 1200))
            ) {
                LoadingIndicator()
            }
        }
        
        // Версия в углу
        AnimatedVisibility(
            visible = startAnimation,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(1000, delayMillis = 1500)
            ) + fadeIn(animationSpec = tween(1000, delayMillis = 1500)),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Card(
                modifier = Modifier.padding(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "v1.0.0",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedLogo(startAnimation: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (startAnimation) 0f else -360f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing)
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(220.dp)
            .scale(scale)
            .rotate(rotation)
    ) {
        // Внешний светящийся круг
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.7f),
                            Color.White.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        radius = 300f
                    )
                )
        )
        
        // Внутренний круг с градиентом
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF5F5F5),
                            Color(0xFFE3F2FD)
                        )
                    )
                )
        )
        
        // Главная иконка - забавный склад
        WarehouseIcon()
    }
}

@Composable
private fun WarehouseIcon() {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Анимация покачивания для коробок
    val boxRotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Анимация подпрыгивания
    val jumpOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutBounce),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.offset(y = jumpOffset.dp)
    ) {
        // Основное здание склада
        Icon(
            imageVector = Icons.Filled.Warehouse,
            contentDescription = "Склад",
            modifier = Modifier.size(90.dp),
            tint = Color(0xFF1976D2)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Движущиеся коробки
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.rotate(boxRotation)
        ) {
            Icon(
                imageVector = Icons.Filled.Inventory,
                contentDescription = "Коробка",
                modifier = Modifier.size(28.dp),
                tint = Color(0xFFFF9800)
            )
            Icon(
                imageVector = Icons.Filled.Inventory2,
                contentDescription = "Коробка",
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF4CAF50)
            )
            Icon(
                imageVector = Icons.Filled.Archive,
                contentDescription = "Коробка",
                modifier = Modifier.size(26.dp),
                tint = Color(0xFFE91E63)
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Кастомный прогресс-бар с градиентом
        Box(
            modifier = Modifier
                .width(250.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFEB3B),
                                Color(0xFFFF9800),
                                Color(0xFFFF5722),
                                Color(0xFFE91E63)
                            )
                        )
                    )
            )
        }
        
        // Текст загрузки с тенью
        Text(
            text = "Инициализация системы...",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )
        
        // Анимированные точки
        LoadingDots()
    }
}

@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition()
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, delayMillis = index * 300),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, delayMillis = index * 300),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun AnimatedParticles(startAnimation: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Создаем несколько плавающих частиц
    repeat(8) { index ->
        val offsetX by infiniteTransition.animateFloat(
            initialValue = (-50).dp.value,
            targetValue = 50.dp.value,
            animationSpec = infiniteRepeatable(
                animation = tween((3000..5000).random(), easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        
        val offsetY by infiniteTransition.animateFloat(
            initialValue = (-30).dp.value,
            targetValue = 30.dp.value,
            animationSpec = infiniteRepeatable(
                animation = tween((2000..4000).random(), easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween((1500..3000).random()),
                repeatMode = RepeatMode.Reverse
            )
        )
        
        Box(
            modifier = Modifier
                .offset(
                    x = offsetX.dp + (index * 100).dp,
                    y = offsetY.dp + (index * 150).dp
                )
                .size((20..40).random().dp)
                .clip(CircleShape)
                .background(
                    Color.White.copy(alpha = if (startAnimation) alpha else 0f)
                )
        )
    }
} 