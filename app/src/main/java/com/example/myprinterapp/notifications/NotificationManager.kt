package com.example.myprinterapp.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationMessage(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val operationId: String? = null,
    val partNumber: String? = null
)

enum class NotificationType {
    ERROR,      // Ошибка в операции
    WARNING,    // Предупреждение
    INFO,       // Информационное сообщение
    URGENT      // Срочное уведомление
}

@Singleton
class NotificationManager @Inject constructor() {
    
    private val _notifications = MutableStateFlow<List<NotificationMessage>>(emptyList())
    val notifications: StateFlow<List<NotificationMessage>> = _notifications.asStateFlow()
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    /**
     * Отправить уведомление инженеру ПДО об ошибке
     */
    fun notifyEngineerAboutError(
        operationId: String,
        partNumber: String,
        errorDescription: String
    ) {
        val notification = NotificationMessage(
            id = generateId(),
            title = "🚨 Ошибка в приемке",
            message = "Операция $operationId (артикул: $partNumber): $errorDescription",
            type = NotificationType.ERROR,
            operationId = operationId,
            partNumber = partNumber
        )
        
        addNotification(notification)
    }
    
    /**
     * Уведомление о незакрытых позициях в управляющей программе
     */
    fun notifyAboutUnclosedPositions(
        operationId: String,
        partNumber: String,
        unclosedCount: Int
    ) {
        val notification = NotificationMessage(
            id = generateId(),
            title = "⚠️ Незакрытые позиции",
            message = "В управляющей программе найдено $unclosedCount незакрытых позиций для операции $operationId ($partNumber)",
            type = NotificationType.WARNING,
            operationId = operationId,
            partNumber = partNumber
        )
        
        addNotification(notification)
    }
    
    /**
     * Уведомление о проблемах с печатью
     */
    fun notifyAboutPrintError(
        operationId: String,
        partNumber: String,
        printError: String
    ) {
        val notification = NotificationMessage(
            id = generateId(),
            title = "🖨️ Ошибка печати",
            message = "Не удалось напечатать этикетку для $partNumber: $printError",
            type = NotificationType.ERROR,
            operationId = operationId,
            partNumber = partNumber
        )
        
        addNotification(notification)
    }
    
    /**
     * Уведомление об успешной передаче данных на компьютер
     */
    fun notifyDataTransferred(
        operationId: String,
        partNumber: String
    ) {
        val notification = NotificationMessage(
            id = generateId(),
            title = "✅ Данные переданы",
            message = "Данные о приемке $partNumber успешно переданы в управляющую систему",
            type = NotificationType.INFO,
            operationId = operationId,
            partNumber = partNumber
        )
        
        addNotification(notification)
    }
    
    /**
     * Срочное уведомление о критической ошибке
     */
    fun notifyUrgentIssue(
        title: String,
        message: String,
        operationId: String? = null
    ) {
        val notification = NotificationMessage(
            id = generateId(),
            title = "🚨 $title",
            message = message,
            type = NotificationType.URGENT,
            operationId = operationId
        )
        
        addNotification(notification)
    }
    
    /**
     * Добавить уведомление в список
     */
    private fun addNotification(notification: NotificationMessage) {
        val currentList = _notifications.value.toMutableList()
        currentList.add(0, notification) // Добавляем в начало списка
        
        // Ограничиваем количество уведомлений до 50
        if (currentList.size > 50) {
            currentList.removeAt(currentList.size - 1)
        }
        
        _notifications.value = currentList
        updateUnreadCount()
    }
    
    /**
     * Отметить уведомление как прочитанное
     */
    fun markAsRead(notificationId: String) {
        val currentList = _notifications.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == notificationId }
        
        if (index != -1) {
            currentList[index] = currentList[index].copy(isRead = true)
            _notifications.value = currentList
            updateUnreadCount()
        }
    }
    
    /**
     * Отметить все уведомления как прочитанные
     */
    fun markAllAsRead() {
        val currentList = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = currentList
        updateUnreadCount()
    }
    
    /**
     * Удалить уведомление
     */
    fun removeNotification(notificationId: String) {
        val currentList = _notifications.value.toMutableList()
        currentList.removeAll { it.id == notificationId }
        _notifications.value = currentList
        updateUnreadCount()
    }
    
    /**
     * Очистить все уведомления
     */
    fun clearAllNotifications() {
        _notifications.value = emptyList()
        _unreadCount.value = 0
    }
    
    /**
     * Обновить счетчик непрочитанных уведомлений
     */
    private fun updateUnreadCount() {
        _unreadCount.value = _notifications.value.count { !it.isRead }
    }
    
    /**
     * Генерация уникального ID
     */
    private fun generateId(): String {
        return "notif_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * Получить уведомления по типу
     */
    fun getNotificationsByType(type: NotificationType): List<NotificationMessage> {
        return _notifications.value.filter { it.type == type }
    }
    
    /**
     * Получить непрочитанные уведомления
     */
    fun getUnreadNotifications(): List<NotificationMessage> {
        return _notifications.value.filter { !it.isRead }
    }
    
    /**
     * Получить уведомления по операции
     */
    fun getNotificationsByOperation(operationId: String): List<NotificationMessage> {
        return _notifications.value.filter { it.operationId == operationId }
    }
} 