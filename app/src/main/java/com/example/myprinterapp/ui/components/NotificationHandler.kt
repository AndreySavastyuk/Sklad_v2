package com.example.myprinterapp.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myprinterapp.viewmodel.*
import kotlinx.coroutines.flow.SharedFlow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

/**
 * Компонент для обработки уведомлений
 */
@Composable
fun NotificationHandler(
    notifications: SharedFlow<NotificationType>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    LaunchedEffect(notifications) {
        notifications.collect { notification ->
            when (notification) {
                is NotificationType.Toast -> {
                    val duration = when (notification.duration) {
                        ToastDuration.SHORT -> Toast.LENGTH_SHORT
                        ToastDuration.LONG -> Toast.LENGTH_LONG
                    }
                    Toast.makeText(context, notification.message, duration).show()
                }
                
                is NotificationType.Snackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = notification.message,
                        actionLabel = notification.actionLabel,
                        duration = when (notification.duration) {
                            com.example.myprinterapp.viewmodel.SnackbarDuration.SHORT -> androidx.compose.material3.SnackbarDuration.Short
                            com.example.myprinterapp.viewmodel.SnackbarDuration.LONG -> androidx.compose.material3.SnackbarDuration.Long
                            com.example.myprinterapp.viewmodel.SnackbarDuration.INDEFINITE -> androidx.compose.material3.SnackbarDuration.Indefinite
                        }
                    )
                    
                    if (result == SnackbarResult.ActionPerformed) {
                        notification.action?.invoke()
                    }
                }
                
                is NotificationType.Success -> {
                    snackbarHostState.showSnackbar(
                        message = "✅ ${notification.message}",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
                
                is NotificationType.Error -> {
                    snackbarHostState.showSnackbar(
                        message = "❌ ${notification.message}",
                        duration = androidx.compose.material3.SnackbarDuration.Long
                    )
                }
                
                is NotificationType.Warning -> {
                    snackbarHostState.showSnackbar(
                        message = "⚠️ ${notification.message}",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
                
                is NotificationType.Info -> {
                    snackbarHostState.showSnackbar(
                        message = "ℹ️ ${notification.message}",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
            }
        }
    }
}

/**
 * Обертка для Scaffold с поддержкой уведомлений
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScaffold(
    notifications: SharedFlow<NotificationType>? = null,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    CustomSnackbar(data = data)
                }
            )
        },
        content = content
    )
    
    // Обработка уведомлений
    notifications?.let { notificationFlow ->
        NotificationHandler(
            notifications = notificationFlow,
            snackbarHostState = snackbarHostState
        )
    }
}

/**
 * Кастомный Snackbar с улучшенным дизайном
 */
@Composable
private fun CustomSnackbar(
    data: SnackbarData,
    modifier: Modifier = Modifier
) {
    val message = data.visuals.message
    val isSuccess = message.startsWith("✅")
    val isError = message.startsWith("❌") 
    val isWarning = message.startsWith("⚠️")
    val isInfo = message.startsWith("ℹ️")
    
    val backgroundColor = when {
        isSuccess -> Color(0xFF4CAF50)
        isError -> MaterialTheme.colorScheme.error
        isWarning -> Color(0xFFFF9800)
        isInfo -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.inverseSurface
    }
    
    val contentColor = when {
        isSuccess -> Color.White
        isError -> MaterialTheme.colorScheme.onError
        isWarning -> Color.White
        isInfo -> MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.inverseOnSurface
    }
    
    Snackbar(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = backgroundColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        action = data.visuals.actionLabel?.let { actionLabel ->
            {
                TextButton(
                    onClick = { data.performAction() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = contentColor
                    )
                ) {
                    Text(actionLabel)
                }
            }
        }
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Хелпер для показа быстрых уведомлений
 */
object NotificationUtils {
    
    fun showQuickToast(context: android.content.Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    fun showLongToast(context: android.content.Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    fun showSuccessToast(context: android.content.Context, message: String) {
        Toast.makeText(context, "✅ $message", Toast.LENGTH_SHORT).show()
    }
    
    fun showErrorToast(context: android.content.Context, message: String) {
        Toast.makeText(context, "❌ $message", Toast.LENGTH_LONG).show()
    }
    
    fun showWarningToast(context: android.content.Context, message: String) {
        Toast.makeText(context, "⚠️ $message", Toast.LENGTH_SHORT).show()
    }
    
    fun showInfoToast(context: android.content.Context, message: String) {
        Toast.makeText(context, "ℹ️ $message", Toast.LENGTH_SHORT).show()
    }
} 