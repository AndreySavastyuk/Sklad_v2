package com.example.myprinterapp.viewmodel

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Типы уведомлений
 */
sealed class NotificationType {
    data class Toast(val message: String, val duration: ToastDuration = ToastDuration.SHORT) : NotificationType()
    data class Snackbar(
        val message: String, 
        val actionLabel: String? = null,
        val action: (() -> Unit)? = null,
        val duration: SnackbarDuration = SnackbarDuration.SHORT
    ) : NotificationType()
    data class Success(val message: String) : NotificationType()
    data class Error(val message: String) : NotificationType()
    data class Warning(val message: String) : NotificationType()
    data class Info(val message: String) : NotificationType()
}

enum class ToastDuration { SHORT, LONG }

enum class SnackbarDuration { SHORT, LONG, INDEFINITE }

/**
 * Глобальный менеджер уведомлений
 */
@Singleton
class NotificationManager @Inject constructor() {
    
    private val _notifications = MutableSharedFlow<NotificationType>()
    val notifications: SharedFlow<NotificationType> = _notifications.asSharedFlow()
    
    suspend fun showToast(message: String, duration: ToastDuration = ToastDuration.SHORT) {
        _notifications.emit(NotificationType.Toast(message, duration))
    }
    
    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        action: (() -> Unit)? = null,
        duration: SnackbarDuration = SnackbarDuration.SHORT
    ) {
        _notifications.emit(NotificationType.Snackbar(message, actionLabel, action, duration))
    }
    
    suspend fun showSuccess(message: String) {
        _notifications.emit(NotificationType.Success(message))
    }
    
    suspend fun showError(message: String) {
        _notifications.emit(NotificationType.Error(message))
    }
    
    suspend fun showWarning(message: String) {
        _notifications.emit(NotificationType.Warning(message))
    }
    
    suspend fun showInfo(message: String) {
        _notifications.emit(NotificationType.Info(message))
    }
}

/**
 * Базовый ViewModel с поддержкой уведомлений
 */
abstract class BaseNotificationViewModel(
    protected val notificationManager: NotificationManager
) {
    
    // Локальные уведомления для конкретного экрана
    private val _localNotifications = MutableSharedFlow<NotificationType>()
    val localNotifications: SharedFlow<NotificationType> = _localNotifications.asSharedFlow()
    
    protected suspend fun showToast(message: String, duration: ToastDuration = ToastDuration.SHORT) {
        _localNotifications.emit(NotificationType.Toast(message, duration))
    }
    
    protected suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        action: (() -> Unit)? = null,
        duration: SnackbarDuration = SnackbarDuration.SHORT
    ) {
        _localNotifications.emit(NotificationType.Snackbar(message, actionLabel, action, duration))
    }
    
    protected suspend fun showSuccess(message: String) {
        _localNotifications.emit(NotificationType.Success(message))
    }
    
    protected suspend fun showError(message: String) {
        _localNotifications.emit(NotificationType.Error(message))
    }
    
    protected suspend fun showWarning(message: String) {
        _localNotifications.emit(NotificationType.Warning(message))
    }
    
    protected suspend fun showInfo(message: String) {
        _localNotifications.emit(NotificationType.Info(message))
    }
    
    // Глобальные уведомления
    protected suspend fun showGlobalToast(message: String, duration: ToastDuration = ToastDuration.SHORT) {
        notificationManager.showToast(message, duration)
    }
    
    protected suspend fun showGlobalSnackbar(
        message: String,
        actionLabel: String? = null,
        action: (() -> Unit)? = null,
        duration: SnackbarDuration = SnackbarDuration.SHORT
    ) {
        notificationManager.showSnackbar(message, actionLabel, action, duration)
    }
    
    protected suspend fun showGlobalSuccess(message: String) {
        notificationManager.showSuccess(message)
    }
    
    protected suspend fun showGlobalError(message: String) {
        notificationManager.showError(message)
    }
    
    protected suspend fun showGlobalWarning(message: String) {
        notificationManager.showWarning(message)
    }
    
    protected suspend fun showGlobalInfo(message: String) {
        notificationManager.showInfo(message)
    }
} 