package com.example.myprinterapp.printer

import android.content.Context
import android.util.Log
import net.posprinter.POSConnect
import net.posprinter.IDeviceConnection
import net.posprinter.IConnectListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PrinterConnection(private val context: Context) {
    companion object {
        private const val TAG = "PrinterConnection"
    }

    private var connection: IDeviceConnection? = null

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
     * Асинхронное подключение по MAC-адресу с использованием корутин
     */
    suspend fun connectAsync(mac: String): Boolean = suspendCancellableCoroutine { continuation ->
        Log.d(TAG, "Attempting to connect to: $mac")

        // Закрываем предыдущее соединение, если есть
        connection?.close()

        // Создаем новое соединение
        connection = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)

        connection?.connect(mac, object : IConnectListener {
            override fun onStatus(code: Int, connectInfo: String?, message: String?) {
                Log.d(TAG, "Connection status: code=$code, info=$connectInfo, msg=$message")

                when (code) {
                    POSConnect.CONNECT_SUCCESS -> {
                        Log.i(TAG, "Successfully connected to printer: $mac")
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                    }
                    POSConnect.CONNECT_FAIL -> {
                        Log.e(TAG, "Failed to connect to printer: $mac - $message")
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                    POSConnect.CONNECT_INTERRUPT -> {
                        Log.w(TAG, "Connection interrupted: $mac")
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                    else -> {
                        Log.w(TAG, "Unknown connection status code: $code")
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                }
            }
        }) ?: run {
            Log.e(TAG, "Failed to create connection")
            continuation.resume(false)
        }

        // Обработка отмены корутины
        continuation.invokeOnCancellation {
            Log.d(TAG, "Connection cancelled")
            connection?.close()
            connection = null
        }
    }

    /**
     * Закрытие соединения
     */
    fun close() {
        try {
            connection?.close()
            Log.d(TAG, "Connection closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing connection", e)
        } finally {
            connection = null
        }
    }

    /**
     * Получение текущего соединения
     */
    fun getConnection(): IDeviceConnection? = connection

    /**
     * Проверка состояния подключения
     */
    fun isConnected(): Boolean {
        return connection != null
    }
}