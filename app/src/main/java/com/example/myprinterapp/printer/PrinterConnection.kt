package com.example.myprinterapp.printer

import android.content.Context
import android.util.Log
import net.posprinter.POSConnect
import net.posprinter.IDeviceConnection
import net.posprinter.IConnectListener
import net.posprinter.TSPLPrinter
import kotlin.Unit
import kotlin.String
import kotlin.Int
import kotlin.Exception

class PrinterConnection(private val context: Context) {
    companion object {
        private const val TAG = "PrinterConnection"
    }

    private var connection: IDeviceConnection? = null
    private var printer: TSPLPrinter? = null
    
    // Колбэки для обработки событий подключения
    private var onConnectionSuccess: (() -> Unit)? = null
    private var onConnectionFailed: ((String) -> Unit)? = null
    
    /**
     * Инициализация SDK
     */
    fun init() {
        try {
            POSConnect.init(context.applicationContext)
            Log.d(TAG, "POSConnect SDK initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize POSConnect SDK", e)
        }
    }

    /**
     * Асинхронное подключение к принтеру (РЕКОМЕНДУЕМЫЙ МЕТОД)
     * @param macAddress MAC-адрес принтера в формате "12:34:56:78:9A:BC"
     * @param onSuccess колбэк вызывается при успешном подключении
     * @param onFailure колбэк вызывается при ошибке подключения с сообщением об ошибке
     */
    fun connectAsync(
        macAddress: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        Log.d(TAG, "Starting async connection to: $macAddress")

        // Закрываем предыдущее соединение, если есть
        closeConnection()

        // Сохраняем колбэки
        onConnectionSuccess = onSuccess
        onConnectionFailed = onFailure

        try {
            // Создаем новое соединение
            connection = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)

            if (connection == null) {
                Log.e(TAG, "Failed to create device connection")
                onFailure("Не удалось создать подключение к устройству")
                return
            }

            // Асинхронное подключение с обработчиком состояния
            connection?.connect(macAddress, object : IConnectListener {
                override fun onStatus(code: Int, connectInfo: String?, message: String?) {
                    Log.d(TAG, "Connection status: code=$code, info=$connectInfo, msg=$message")

                    when (code) {
                        POSConnect.CONNECT_SUCCESS -> {
                            Log.i(TAG, "✅ Successfully connected to printer: $macAddress")
                            
                            // Создаем объект принтера после успешного подключения
                            connection?.let { deviceConnection ->
                                try {
                                    printer = TSPLPrinter(deviceConnection)
                                    Log.d(TAG, "TSPLPrinter created successfully")
                                    
                                    // Вызываем колбэк успеха
                                    onConnectionSuccess?.invoke()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to create TSPLPrinter", e)
                                    onConnectionFailed?.invoke("Ошибка инициализации принтера: ${e.message}")
                                }
                            } ?: run {
                                onConnectionFailed?.invoke("Соединение потеряно после подключения")
                            }
                        }
                        
                        POSConnect.CONNECT_FAIL -> {
                            val errorMsg = message ?: "Неизвестная ошибка подключения"
                            Log.e(TAG, "❌ Failed to connect to printer: $macAddress - $errorMsg")
                            
                            val userFriendlyMessage = when {
                                errorMsg.contains("timeout", ignoreCase = true) -> 
                                    "Превышено время ожидания. Проверьте что принтер включен и находится рядом."
                                errorMsg.contains("not found", ignoreCase = true) -> 
                                    "Принтер не найден. Проверьте MAC-адрес: $macAddress"
                                errorMsg.contains("busy", ignoreCase = true) -> 
                                    "Принтер занят другим устройством"
                                else -> 
                                    "Ошибка подключения: $errorMsg"
                            }
                            
                            onConnectionFailed?.invoke(userFriendlyMessage)
                        }
                        
                        POSConnect.CONNECT_INTERRUPT -> {
                            Log.w(TAG, "⚠️ Connection interrupted: $macAddress")
                            onConnectionFailed?.invoke("Соединение прервано")
                        }
                        
                        else -> {
                            Log.w(TAG, "Unknown connection status code: $code")
                            onConnectionFailed?.invoke("Неизвестный статус подключения: $code")
                        }
                    }
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "Exception during connection attempt", e)
            onFailure("Ошибка при попытке подключения: ${e.message}")
        }
    }

    /**
     * Закрытие соединения (асинхронное)
     */
    fun disconnect() {
        try {
            Log.d(TAG, "Disconnecting from printer...")
            
            // Очищаем принтер
            printer = null
            
            // Закрываем соединение асинхронно
            connection?.close()
            Log.d(TAG, "Connection closed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            connection = null
            // Очищаем колбэки
            onConnectionSuccess = null
            onConnectionFailed = null
        }
    }

    /**
     * Принудительное закрытие соединения (для внутреннего использования)
     */
    private fun closeConnection() {
        try {
            printer = null
            connection?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing previous connection", e)
        } finally {
            connection = null
        }
    }

    /**
     * Получение текущего соединения для низкоуровневых операций
     */
    fun getConnection(): IDeviceConnection? = connection

    /**
     * Получение объекта принтера для операций печати
     */
    fun getPrinter(): TSPLPrinter? = printer

    /**
     * Проверка состояния подключения
     */
    fun isConnected(): Boolean {
        return connection != null && printer != null
    }

    /**
     * Отправка тестовой команды для проверки соединения
     */
    fun testConnection(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val currentPrinter = printer
        if (currentPrinter == null) {
            onFailure("Принтер не подключен")
            return
        }

        try {
            // Простая команда для проверки связи
            currentPrinter.cls()
            currentPrinter.sound(1, 100) // Короткий звук
            
            Log.d(TAG, "Test connection successful")
            onSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Test connection failed", e)
            onFailure("Ошибка проверки соединения: ${e.message}")
        }
    }
}