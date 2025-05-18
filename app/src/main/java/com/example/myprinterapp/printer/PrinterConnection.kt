package com.example.myprinterapp.printer

import android.content.Context
import android.util.Log
import net.posprinter.POSConnect
import net.posprinter.IDeviceConnection
import net.posprinter.IConnectListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PrinterConnection(private val context: Context) {
    companion object {
        private const val TAG = "PrinterConnection"
    }

    private var connection: IDeviceConnection? = null

    /**
     * Инициализация SDK и создание соединения Bluetooth.
     */
    fun init() {
        POSConnect.init(context.applicationContext)
    }

    /**
     * Подключение по MAC-адресу.
     * @return true, если подключение успешно.
     */
    fun connect(mac: String): Boolean {
        Log.d(TAG, "connect: mac=$mac")
        connection?.close()
        connection = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)
        val latch = CountDownLatch(1)
        var success = false
        connection!!.connect(mac, object : IConnectListener {
            override fun onStatus(code: Int, info: String?, msg: String?) {
                success = code == POSConnect.CONNECT_SUCCESS
                latch.countDown()
            }
        })
        latch.await(5, TimeUnit.SECONDS)
        return success
    }

    fun getConnection(): IDeviceConnection? = connection
}