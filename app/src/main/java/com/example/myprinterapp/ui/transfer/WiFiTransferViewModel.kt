package com.example.myprinterapp.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprinterapp.network.WiFiTransferService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WiFiTransferViewModel @Inject constructor(
    private val wiFiTransferService: WiFiTransferService
) : ViewModel() {

    val serverState = wiFiTransferService.serverState
    val deviceIpAddress = wiFiTransferService.deviceIpAddress
    val receivedTasks = wiFiTransferService.receivedTasks
    val events = wiFiTransferService.events

    fun startServer() {
        wiFiTransferService.startServer()
    }

    fun stopServer() {
        wiFiTransferService.stopServer()
    }

    fun clearReceivedTasks() {
        wiFiTransferService.clearReceivedTasks()
    }

    fun clearEvent() {
        // TODO: Добавить метод в сервис для очистки событий
    }

    override fun onCleared() {
        super.onCleared()
        stopServer()
    }
}