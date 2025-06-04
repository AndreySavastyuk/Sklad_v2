# Реализация асинхронного подключения к принтеру

## Обзор

Реализовано полностью асинхронное подключение к термопринтеру согласно рекомендациям документации Android POS SDK. Основные изменения:

## 1. PrinterConnection.kt - Асинхронное подключение

### Ключевые особенности:
- **Полностью асинхронное подключение** без использования корутин
- **Callback-based API** для обработки результатов подключения
- **Правильная обработка всех статусов** подключения (SUCCESS, FAIL, INTERRUPT)
- **Автоматическое создание TSPLPrinter** после успешного подключения
- **Детальные сообщения об ошибках** с пользовательскими объяснениями

### Основные методы:
```kotlin
fun connectAsync(
    macAddress: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
)
```

### Обработка статусов:
- `POSConnect.CONNECT_SUCCESS` - создание TSPLPrinter и вызов onSuccess
- `POSConnect.CONNECT_FAIL` - анализ ошибки и пользовательское сообщение
- `POSConnect.CONNECT_INTERRUPT` - обработка прерывания соединения

## 2. PrinterService.kt - Обновленная интеграция

### Изменения:
- **Убран suspend** из метода `connect()` 
- **Callback-based подключение** через PrinterConnection
- **Автоматическое обновление состояния** через StateFlow
- **Тестовый звуковой сигнал** при успешном подключении

### Состояния подключения:
```kotlin
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING, 
    CONNECTED
}
```

## 3. SettingsViewModel.kt - Реактивное UI

### Особенности:
- **Наблюдение за connectionState** для автоматического обновления UI
- **Немедленный возврат** из connectPrinterInternal()
- **Асинхронное обновление состояния** через Flow

### Логика обновления UI:
```kotlin
viewModelScope.launch {
    connectionState.collect { state ->
        when (state) {
            ConnectionState.CONNECTED -> // Успешное подключение
            ConnectionState.CONNECTING -> // Показать загрузку
            ConnectionState.DISCONNECTED -> // Показать отключение
        }
    }
}
```

## 4. PrinterManager.kt - Реальная реализация

### RealPrinterManager:
- **Интеграция с PrinterService** для реальной печати
- **Управление состоянием устройств** через PrinterSettings
- **Обработка ошибок печати** с детальными сообщениями

## 5. Форматы этикеток - LabelFormat.kt

### Поддерживаемые форматы:
- **AcceptanceLabelFormat57x40** - этикетки приемки 57x40мм
- **PickingLabelFormat57x40** - этикетки комплектации 57x40мм
- **UTF-8 поддержка** для QR-кодов с кириллицей
- **Автоматическая генерация QR-кодов** с обработкой ошибок

## Преимущества асинхронного подхода

### 1. Производительность:
- ✅ Не блокирует UI поток
- ✅ Быстрый отклик интерфейса
- ✅ Нет необходимости в корутинах для базовых операций

### 2. Надежность:
- ✅ Правильная обработка всех статусов подключения
- ✅ Детальные сообщения об ошибках
- ✅ Автоматическое управление ресурсами

### 3. Простота использования:
- ✅ Callback-based API легко понимать
- ✅ Автоматическое обновление UI состояния
- ✅ Минимальный boilerplate код

## Использование

### Подключение к принтеру:
```kotlin
// В SettingsViewModel
fun connectPrinter() {
    printerService.connect(macAddress) // Немедленный возврат
    // UI обновляется автоматически через connectionState
}
```

### Печать этикетки:
```kotlin
// В AcceptViewModel через UseCase
val result = printLabelUseCase(PrintLabelUseCase.Params(labelData))
```

### Тестирование соединения:
```kotlin
printerService.testConnection(
    onSuccess = { /* Соединение работает */ },
    onFailure = { error -> /* Обработка ошибки */ }
)
```

## Совместимость

- ✅ Android POS SDK рекомендации
- ✅ Полная поддержка UTF-8/кириллицы
- ✅ Обратная совместимость с существующим кодом
- ✅ Dagger Hilt dependency injection

## Диагностика

### Логи подключения:
- `PrinterConnection` - детальные логи подключения
- `PrinterService` - состояния сервиса
- `RealPrinterManager` - операции печати

### Обработка ошибок:
- Timeout - "Превышено время ожидания..."
- Not found - "Принтер не найден..."
- Busy - "Принтер занят другим устройством"
- Generic - "Ошибка подключения: [детали]" 