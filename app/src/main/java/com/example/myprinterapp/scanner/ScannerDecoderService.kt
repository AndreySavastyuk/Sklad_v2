package com.example.myprinterapp.scanner

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис для декодирования данных со сканера
 * Решает проблему с передачей кириллицы через HID
 */
@Singleton
class ScannerDecoderService @Inject constructor() {

    companion object {
        private const val TAG = "ScannerDecoder"
    }

    /**
     * Структура данных HID POS
     */
    data class HidPosData(
        val symbology: String,      // Тип штрих-кода
        val length: Int,            // Длина данных
        val data: String,           // Сами данные
        val rawBytes: ByteArray?    // Сырые байты
    )

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
     * Декодирование данных из HID POS формата
     */
    fun decodeHidPosData(input: String): HidPosData? {
        Log.d(TAG, "Decoding HID POS input: $input")
        Log.d(TAG, "Input length: ${input.length}")
        Log.d(TAG, "Input bytes: ${input.toByteArray().contentToString()}")

        return try {
            // В HID POS режиме данные могут приходить с префиксами
            // Формат зависит от настроек сканера

            // Проверяем на наличие AIM ID (если включен)
            if (input.startsWith("]")) {
                return decodeWithAimId(input)
            }

            // Проверяем на наличие Code ID с разделителем GS (Group Separator)
            if (input.length > 2 && input.contains('\u001D')) {
                return decodeWithCodeId(input)
            }

            // Проверяем на простой Code ID (первый символ - идентификатор)
            if (input.length > 1 && !input[0].isLetterOrDigit() && input[0].code < 128) {
                return decodeWithSimpleCodeId(input)
            }

            // Если нет специальных идентификаторов, пробуем декодировать как обычные данные
            val decodedData = decode(input)
            HidPosData(
                symbology = "UNKNOWN",
                length = decodedData.length,
                data = decodedData,
                rawBytes = input.toByteArray()
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error decoding HID POS data", e)
            null
        }
    }

    /**
     * Декодирование с AIM ID
     * Формат: ]X0 данные, где X - тип кода
     */
    private fun decodeWithAimId(input: String): HidPosData {
        val aimId = if (input.length >= 3) input.substring(0, 3) else ""
        val rawData = if (input.length > 3) input.substring(3) else ""

        val symbology = when (aimId) {
            "]Q0", "]Q1", "]Q2", "]Q3", "]Q4", "]Q5" -> "QR Code"
            "]C0", "]C1", "]C2", "]C3", "]C4" -> "Code 128"
            "]E0", "]E1", "]E2", "]E3", "]E4" -> "EAN-13"
            "]A0", "]A1", "]A2", "]A3", "]A4" -> "Code 39"
            "]d0", "]d1", "]d2" -> "Data Matrix"
            "]I0", "]I1", "]I2", "]I3" -> "Code 93"
            "]G0", "]G1", "]G2", "]G3" -> "Code 11"
            "]X0" -> "Code 39 Full ASCII"
            "]U0", "]U1", "]U2", "]U3" -> "MaxiCode"
            else -> "Unknown ($aimId)"
        }

        Log.d(TAG, "AIM ID detected: $aimId = $symbology")

        val decodedData = decode(rawData)

        return HidPosData(
            symbology = symbology,
            length = decodedData.length,
            data = decodedData,
            rawBytes = rawData.toByteArray()
        )
    }

    /**
     * Декодирование с Code ID и разделителем GS
     */
    private fun decodeWithCodeId(input: String): HidPosData {
        val gsIndex = input.indexOf('\u001D')
        val codeId = if (gsIndex > 0) input.substring(0, gsIndex) else input[0].toString()
        val rawData = if (gsIndex > 0 && gsIndex < input.length - 1) {
            input.substring(gsIndex + 1)
        } else {
            input.substring(1)
        }

        val symbology = decodeSymbologyFromCodeId(codeId)

        Log.d(TAG, "Code ID detected: $codeId = $symbology")

        val decodedData = decode(rawData)

        return HidPosData(
            symbology = symbology,
            length = decodedData.length,
            data = decodedData,
            rawBytes = rawData.toByteArray()
        )
    }

    /**
     * Декодирование с простым Code ID (без разделителя)
     */
    private fun decodeWithSimpleCodeId(input: String): HidPosData {
        val codeId = input[0].toString()
        val rawData = input.substring(1)

        val symbology = decodeSymbologyFromCodeId(codeId)

        Log.d(TAG, "Simple Code ID detected: $codeId = $symbology")

        val decodedData = decode(rawData)

        return HidPosData(
            symbology = symbology,
            length = decodedData.length,
            data = decodedData,
            rawBytes = rawData.toByteArray()
        )
    }

    /**
     * Определение типа штрих-кода по Code ID
     */
    private fun decodeSymbologyFromCodeId(codeId: String): String {
        return when (codeId.firstOrNull()) {
            'q', 'Q' -> "QR Code"
            'j', 'J' -> "Code 128"
            'd', 'D' -> "EAN-13"
            'b', 'B' -> "Code 39"
            'u', 'U' -> "Data Matrix"
            'e', 'E' -> "EAN-8"
            'c', 'C' -> "UPC-A"
            'a', 'A' -> "UPC-E"
            'i', 'I' -> "Code 93"
            'g', 'G' -> "Code 11"
            'f', 'F' -> "Codabar"
            'p', 'P' -> "PDF417"
            'r', 'R' -> "GS1 DataBar"
            'm', 'M' -> "MSI"
            'z', 'Z' -> "Aztec Code"
            else -> "Unknown ($codeId)"
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
     * Проверка на HEX кодирование (улучшенная версия)
     */
    fun isHexEncoded(input: String): Boolean {
        // Проверяем различные форматы HEX
        return input.contains("\\x") ||
                input.contains("%") ||
                (input.length % 2 == 0 && input.length >= 4 &&
                        input.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' })
    }

    /**
     * Тестирование декодера HID POS
     */
    fun testHidPosDecoder() {
        val testCases = listOf(
            // Обычные данные
            "test=2024/001=PART-123=Деталь тестовая",
            // С AIM ID для QR
            "]Q0test=2024/001=PART-123=Деталь",
            // С Code ID и GS разделителем
            "q\u001Dtest=2024/001=PART-123=Test",
            // HEX формат
            "\\xD2\\xE5\\xF1\\xF2",
            // URL encoded
            "%D0%A2%D0%B5%D1%81%D1%82",
            // Простой Code ID
            "qTEST123"
        )

        Log.d(TAG, "=== HID POS DECODER TEST START ===")
        testCases.forEach { testCase ->
            Log.d(TAG, "Testing: $testCase")
            val result = decodeHidPosData(testCase)
            Log.d(TAG, "Result: $result")
        }
        Log.d(TAG, "=== HID POS DECODER TEST END ===")
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