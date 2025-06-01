#!/bin/bash

# Скрипт для настройки среды разработки MyPrinterApp
# Использование: ./scripts/setup_dev.sh

echo "🚀 Настройка среды разработки MyPrinterApp..."

# Проверка наличия необходимых инструментов
echo "📋 Проверка системы..."

# Проверка Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f 2)
    echo "✅ Java: $JAVA_VERSION"
else
    echo "❌ Java не найдена. Установите JDK 17+"
    exit 1
fi

# Проверка Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  ANDROID_HOME не установлена"
    echo "💡 Добавьте в ~/.bashrc или ~/.zshrc:"
    echo "   export ANDROID_HOME=\$HOME/Android/Sdk"
    echo "   export PATH=\$PATH:\$ANDROID_HOME/tools:\$ANDROID_HOME/platform-tools"
else
    echo "✅ Android SDK: $ANDROID_HOME"
fi

# Проверка ADB
if command -v adb &> /dev/null; then
    echo "✅ ADB доступен"
else
    echo "❌ ADB не найден в PATH"
fi

# Создание необходимых директорий
echo "📁 Создание директорий..."
mkdir -p app/libs
mkdir -p scripts
mkdir -p docs

# Установка прав на gradlew
echo "🔧 Настройка Gradle..."
chmod +x gradlew

# Очистка и синхронизация проекта
echo "🧹 Очистка проекта..."
./gradlew clean

echo "📦 Синхронизация зависимостей..."
./gradlew build --dry-run

# Создание файлов конфигурации (если не существуют)
if [ ! -f "local.properties" ]; then
    echo "📝 Создание local.properties..."
    if [ ! -z "$ANDROID_HOME" ]; then
        echo "sdk.dir=$ANDROID_HOME" > local.properties
    else
        echo "sdk.dir=/путь/к/android/sdk" > local.properties
        echo "⚠️  Отредактируйте local.properties с правильным путем к Android SDK"
    fi
fi

# Создание базового keystore для debug (если не существует)
if [ ! -f "app/debug.keystore" ]; then
    echo "🔑 Создание debug keystore..."
    keytool -genkey -v -keystore app/debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null || echo "⚠️  Не удалось создать keystore"
fi

# Проверка lint
echo "🔍 Запуск анализа кода..."
./gradlew lintDebug || echo "⚠️  Обнаружены проблемы в коде"

# Запуск тестов
echo "🧪 Запуск unit тестов..."
./gradlew test || echo "⚠️  Некоторые тесты не прошли"

echo ""
echo "✅ Настройка завершена!"
echo ""
echo "📚 Полезные команды:"
echo "  ./gradlew assembleDebug     - Сборка debug APK"
echo "  ./gradlew test              - Запуск unit тестов"
echo "  ./gradlew connectedAndroidTest - Запуск UI тестов"
echo "  ./gradlew lint              - Анализ кода"
echo "  ./gradlew clean             - Очистка проекта"
echo ""
echo "🔧 Следующие шаги:"
echo "1. Откройте проект в Android Studio"
echo "2. Синхронизируйте Gradle файлы"
echo "3. Подключите библиотеки принтеров в app/libs/ (при наличии)"
echo "4. Запустите приложение на устройстве или эмуляторе"
echo "" 