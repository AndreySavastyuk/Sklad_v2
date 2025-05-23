package com.example.myprinterapp.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.myprinterapp.data.PickTask
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис для передачи заданий по WiFi
 *
 * Принцип работы:
 * 1. Терминал запускает сервер на порту 8888
 * 2. ПК подключается к терминалу по IP и отправляет JSON с заданиями
 * 3. Терминал получает и обрабатывает задания
 *
 * Протокол обмена:
 * - ПК отправляет: {"command": "SEND_TASKS", "data": {...}}
 * - Терминал отвечает: {"status": "OK", "message": "Received X tasks"}
 */
@Singleton
class WiFiTransferService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "WiFiTransferService"
        private const val PORT = 8888
        private const val BUFFER_SIZE = 8192
    }

    private val gson = Gson()
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    // Состояние сервера
    private val _serverState = MutableStateFlow(ServerState.STOPPED)
    val serverState: StateFlow<ServerState> = _serverState

    // IP адрес устройства
    private val _deviceIpAddress = MutableStateFlow("")
    val deviceIpAddress: StateFlow<String> = _deviceIpAddress

    // Последние полученные задания
    private val _receivedTasks = MutableStateFlow<List<PickTask>>(emptyList())
    val receivedTasks: StateFlow<List<PickTask>> = _receivedTasks

    // События
    private val _events = MutableStateFlow<TransferEvent?>(null)
    val events: StateFlow<TransferEvent?> = _events

    /**
     * Запуск сервера для приема заданий
     */
    fun startServer() {
        if (_serverState.value == ServerState.RUNNING) return

        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                _serverState.value = ServerState.STARTING
                updateDeviceIpAddress()

                serverSocket = ServerSocket(PORT)
                _serverState.value = ServerState.RUNNING
                Log.i(TAG, "Server started on port $PORT, IP: ${_deviceIpAddress.value}")

                while (isActive) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        clientSocket?.let { socket ->
                            handleClient(socket)
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            Log.e(TAG, "Error accepting client", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
                _events.value = TransferEvent.Error("Ошибка сервера: ${e.message}")
            } finally {
                _serverState.value = ServerState.STOPPED
            }
        }
    }

    /**
     * Остановка сервера
     */
    fun stopServer() {
        serverJob?.cancel()
        serverSocket?.close()
        serverSocket = null
        _serverState.value = ServerState.STOPPED
        Log.i(TAG, "Server stopped")
    }

    /**
     * Обработка подключения клиента
     */
    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            Log.i(TAG, "Client connected: ${socket.inetAddress.hostAddress}")
            _events.value = TransferEvent.ClientConnected(socket.inetAddress.hostAddress ?: "Unknown")

            // Читаем данные от клиента
            val requestBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) break
                requestBuilder.append(line)
            }

            val requestJson = requestBuilder.toString()
            Log.d(TAG, "Received data: $requestJson")

            // Обрабатываем запрос
            val response = processRequest(requestJson)

            // Отправляем ответ
            writer.println(response)
            writer.flush()

            socket.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client", e)
            _events.value = TransferEvent.Error("Ошибка обработки клиента: ${e.message}")
        }
    }

    /**
     * Обработка запроса от клиента
     */
    private fun processRequest(requestJson: String): String {
        return try {
            val request = gson.fromJson(requestJson, TransferRequest::class.java)

            when (request.command) {
                "SEND_TASKS" -> {
                    val tasksData = gson.toJsonTree(request.data).asJsonObject
                    val tasksList = gson.fromJson(tasksData.get("tasks"), Array<PickTask>::class.java).toList()

                    _receivedTasks.value = tasksList
                    _events.value = TransferEvent.TasksReceived(tasksList.size)

                    Log.i(TAG, "Received ${tasksList.size} tasks")

                    gson.toJson(TransferResponse(
                        status = "OK",
                        message = "Получено заданий: ${tasksList.size}"
                    ))
                }
                "PING" -> {
                    gson.toJson(TransferResponse(
                        status = "OK",
                        message = "PONG"
                    ))
                }
                else -> {
                    gson.toJson(TransferResponse(
                        status = "ERROR",
                        message = "Неизвестная команда: ${request.command}"
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing request", e)
            gson.toJson(TransferResponse(
                status = "ERROR",
                message = "Ошибка обработки запроса: ${e.message}"
            ))
        }
    }

    /**
     * Получение IP адреса устройства
     */
    private fun updateDeviceIpAddress() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress

            // Конвертируем int в строку IP
            val formattedIp = String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )

            _deviceIpAddress.value = formattedIp
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP address", e)
            _deviceIpAddress.value = "Неизвестно"
        }
    }

    /**
     * Очистка полученных заданий
     */
    fun clearReceivedTasks() {
        _receivedTasks.value = emptyList()
    }
}

// Состояния сервера
enum class ServerState {
    STOPPED,
    STARTING,
    RUNNING
}

// События передачи
sealed class TransferEvent {
    data class ClientConnected(val address: String) : TransferEvent()
    data class TasksReceived(val count: Int) : TransferEvent()
    data class Error(val message: String) : TransferEvent()
}

// Модели данных для обмена
data class TransferRequest(
    val command: String,
    val data: Any?
)

data class TransferResponse(
    val status: String,
    val message: String
)