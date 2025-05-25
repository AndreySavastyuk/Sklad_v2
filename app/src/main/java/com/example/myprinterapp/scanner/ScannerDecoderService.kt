package com.example.myprinterapp.scanner

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис для декодирования данных со сканера
 * Решает проблему с передачей кириллицы через HID
 */
@Singleton
class ScannerDecoderService @Inject constructor() {

    /**
     * Декодирует строку, полученную со сканера
     * Поддерживает различные форматы кодирования
     */
    fun decode(input: String): String {
        return when {
            // Если строка содержит HEX-коды (например: \x041F\x0440\x0438\x0432\x0435\x0442)
            input.contains("\\x") -> decodeHexString(input)

            // Если строка в Base64
            isBase64(input) -> decodeBase64(input)

            // Если строка содержит URL-кодирование
            input.contains("%") -> decodeUrlEncoded(input)

            // Если строка содержит Unicode escape sequences (\u041F\u0440\u0438\u0432\u0435\u0442)
            input.contains("\\u") -> decodeUnicodeEscapes(input)

            // Попытка исправить неправильную кодировку
            isProbablyMisencoded(input) -> tryFixEncoding(input)

            // Иначе возвращаем как есть
            else -> input
        }
    }

    /**
     * Декодирует HEX-строку
     */
    private fun decodeHexString(input: String): String {
        return try {
            val pattern = """\\x([0-9A-Fa-f]{2,4})""".toRegex()
            pattern.replace(input) { matchResult ->
                val hex = matchResult.groupValues[1]
                val code = hex.toInt(16)
                code.toChar().toString()
            }
        } catch (e: Exception) {
            input
        }
    }

    /**
     * Декодирует Base64
     */
    private fun decodeBase64(input: String): String {
        return try {
            val bytes = android.util.Base64.decode(input, android.util.Base64.DEFAULT)
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            input
        }
    }

    /**
     * Проверяет, является ли строка Base64
     */
    private fun isBase64(input: String): Boolean {
        return input.matches(Regex("^[A-Za-z0-9+/]+=*$")) && input.length % 4 == 0
    }

    /**
     * Декодирует URL-encoded строку
     */
    private fun decodeUrlEncoded(input: String): String {
        return try {
            java.net.URLDecoder.decode(input, "UTF-8")
        } catch (e: Exception) {
            input
        }
    }

    /**
     * Декодирует Unicode escape sequences
     */
    private fun decodeUnicodeEscapes(input: String): String {
        return try {
            val pattern = """\\u([0-9A-Fa-f]{4})""".toRegex()
            pattern.replace(input) { matchResult ->
                val hex = matchResult.groupValues[1]
                val code = hex.toInt(16)
                code.toChar().toString()
            }
        } catch (e: Exception) {
            input
        }
    }

    /**
     * Проверяет, возможно ли строка неправильно закодирована
     */
    private fun isProbablyMisencoded(input: String): Boolean {
        // Проверяем на наличие типичных признаков неправильной кодировки
        return input.contains("Ð") || input.contains("Ñ") || input.contains("â")
    }

    /**
     * Пытается исправить неправильную кодировку
     */
    private fun tryFixEncoding(input: String): String {
        return try {
            // Попытка 1: Windows-1251 интерпретированная как UTF-8
            val bytes = input.toByteArray(Charsets.ISO_8859_1)
            String(bytes, charset("Windows-1251"))
        } catch (e: Exception) {
            try {
                // Попытка 2: UTF-8 интерпретированная как Windows-1251
                val bytes = input.toByteArray(charset("Windows-1251"))
                String(bytes, Charsets.UTF_8)
            } catch (e2: Exception) {
                input // Возвращаем оригинал если не удалось
            }
        }
    }

    /**
     * Конвертирует транслит в кириллицу
     * Используется как запасной вариант
     */
    fun translitToCyrillic(input: String): String {
        val translitMap = mapOf(
            "a" to "а", "b" to "б", "v" to "в", "g" to "г", "d" to "д",
            "e" to "е", "zh" to "ж", "z" to "з", "i" to "и", "j" to "й",
            "k" to "к", "l" to "л", "m" to "м", "n" to "н", "o" to "о",
            "p" to "п", "r" to "р", "s" to "с", "t" to "т", "u" to "у",
            "f" to "ф", "h" to "х", "c" to "ц", "ch" to "ч", "sh" to "ш",
            "sch" to "щ", "'" to "ъ", "y" to "ы", "'" to "ь", "e" to "э",
            "yu" to "ю", "ya" to "я",
            // Заглавные
            "A" to "А", "B" to "Б", "V" to "В", "G" to "Г", "D" to "Д",
            "E" to "Е", "Zh" to "Ж", "Z" to "З", "I" to "И", "J" to "Й",
            "K" to "К", "L" to "Л", "M" to "М", "N" to "Н", "O" to "О",
            "P" to "П", "R" to "Р", "S" to "С", "T" to "Т", "U" to "У",
            "F" to "Ф", "H" to "Х", "C" to "Ц", "Ch" to "Ч", "Sh" to "Ш",
            "Sch" to "Щ", "Y" to "Ы", "E" to "Э", "Yu" to "Ю", "Ya" to "Я"
        )

        var result = input
        // Сначала заменяем длинные комбинации
        translitMap.entries
            .sortedByDescending { it.key.length }
            .forEach { (latin, cyrillic) ->
                result = result.replace(latin, cyrillic)
            }

        return result
    }
}

/**
 * Альтернативное решение: Настройки сканера для поддержки кириллицы
 */
class ScannerCyrillicSetup {

    /**
     * Инструкции по настройке HR32-BT для кириллицы
     */
    fun getSetupInstructions(): String {
        return """
            НАСТРОЙКА СКАНЕРА ДЛЯ КИРИЛЛИЦЫ:
            
            Вариант 1: Режим передачи данных (Data Mode)
            1. Отсканируйте "Enter Setup"
            2. Отсканируйте "SPP Mode" или "Serial Port Mode"
            3. Отсканируйте "Character Set" → "UTF-8"
            4. Отсканируйте "Exit Setup"
            
            Вариант 2: Передача в HEX формате
            1. Отсканируйте "Enter Setup"  
            2. Отсканируйте "Data Format" → "Hex String"
            3. Отсканируйте "Prefix" → "\x"
            4. Отсканируйте "Exit Setup"
            
            Вариант 3: Использование AIM ID
            1. Отсканируйте "Enter Setup"
            2. Отсканируйте "AIM ID" → "Enable"
            3. Отсканируйте "Code ID" → "Enable"
            4. Отсканируйте "Exit Setup"
            
            После этого приложение будет декодировать данные автоматически.
        """.trimIndent()
    }

    /**
     * Альтернативные штрих-коды для настройки
     */
    fun getConfigBarcodes(): Map<String, String> {
        return mapOf(
            "UTF-8 Mode" to "\\u0001\\u0002\\u00C8",
            "HEX Mode" to "\\u0001\\u0002\\u00H1",
            "Base64 Mode" to "\\u0001\\u0002\\u00B4",
            "Windows-1251" to "\\u0001\\u0002\\u0251"
        )
    }
}