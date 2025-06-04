# Исправление проблемы с разрешениями Bluetooth

## Проблема
Приложение выдавало ошибку:
```
java.lang.SecurityException: Need android.permission.BLUETOOTH_SCAN permission
```

## Решение

### 1. Обновлены разрешения в AndroidManifest.xml

Добавлены все необходимые разрешения для разных версий Android:

```xml
<!-- Для Android 12+ (API 31+) -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" android:required="false" />

<!-- Для Android 11 и ниже -->
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />

<!-- Для поиска устройств на Android 10-11 -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" android:maxSdkVersion="30" />

<!-- USB разрешения для принтеров -->
<uses-permission android:name="android.permission.USB_PERMISSION" />
```

### 2. Добавлена логика запроса разрешений в MainActivity

#### Ключевые изменения:
- **ActivityResultContracts**: Современный способ запроса разрешений
- **Runtime проверка**: Разрешения проверяются перед каждой операцией с принтером
- **Версионная совместимость**: Разные разрешения для разных версий Android

#### Основные методы:
```kotlin
// Проверка наличия разрешений
private fun hasBluetoothPermissions(): Boolean

// Запрос разрешений с колбэками
private fun requestBluetoothPermissionsIfNeeded(
    onGranted: () -> Unit,
    onDenied: (List<String>) -> Unit
)

// Получение списка необходимых разрешений для текущей версии
private fun getRequiredPermissions(): List<String>
```

### 3. Интеграция с UI через SettingsViewModel

#### Новые состояния UI:
- `PermissionRequired` - показывает объяснение необходимых разрешений
- Обработка колбэков разрешений через MainActivity

#### Автоматическая проверка:
- Перед подключением к принтеру
- Перед печатью тестовой этикетки
- При любых операциях с принтером

### 4. Пользовательский интерфейс для разрешений

#### Диалог объяснения разрешений:
- Показывает какие разрешения требуются и зачем
- Предлагает открыть настройки приложения
- Понятные объяснения для пользователя

#### Визуальные индикаторы:
- Иконка безопасности для разрешений
- Цветовое кодирование ошибок
- Информативные сообщения

## Архитектура решения

```
MainActivity
├── ActivityResultContracts для запроса разрешений
├── Проверка разрешений по версиям Android
└── Колбэки для SettingsViewModel

SettingsViewModel
├── Состояния UI включая PermissionRequired
├── Интеграция с MainActivity через колбэки
└── Автоматическая проверка перед операциями

SettingsScreen
├── Диалог объяснения разрешений
├── Визуальные индикаторы состояния
└── Автоматическое отображение требований
```

## Поддерживаемые версии Android

### Android 12+ (API 31+)
- `BLUETOOTH_SCAN` - поиск устройств
- `BLUETOOTH_CONNECT` - подключение к устройствам
- `BLUETOOTH_ADVERTISE` - рекламирование (опционально)

### Android 6-11 (API 23-30)
- `ACCESS_FINE_LOCATION` - требуется для поиска Bluetooth устройств
- Старые Bluetooth разрешения (`maxSdkVersion="30"`)

### Android < 6.0 (API < 23)
- Разрешения предоставляются при установке
- Не требуется runtime запрос

## Безопасность

- `usesPermissionFlags="neverForLocation"` для BLUETOOTH_SCAN предотвращает использование для геолокации
- Четкие объяснения пользователю о назначении каждого разрешения
- Минимальный набор необходимых разрешений

## Использование

1. **Автоматическая проверка**: Разрешения проверяются автоматически при попытке подключения
2. **Понятные сообщения**: Пользователь видит, какие разрешения нужны и зачем
3. **Graceful degradation**: При отказе в разрешениях показываются информативные сообщения
4. **Открытие настроек**: Возможность перейти в настройки приложения для предоставления разрешений

## Результат

- ✅ Исправлена ошибка `SecurityException: Need android.permission.BLUETOOTH_SCAN`
- ✅ Поддержка всех версий Android (6.0+)
- ✅ Понятный UX для запроса разрешений
- ✅ Graceful handling отказа в разрешениях
- ✅ Минимальный набор необходимых разрешений
- ✅ Безопасная работа с Bluetooth без доступа к геолокации 