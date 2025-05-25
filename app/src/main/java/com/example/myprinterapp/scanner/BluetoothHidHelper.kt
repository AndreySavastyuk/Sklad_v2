package com.example.myprinterapp.scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import java.lang.reflect.Method

/**
 * Вспомогательный класс для работы с Bluetooth HID устройствами
 */
object BluetoothHidHelper {
    private const val TAG = "BluetoothHidHelper"

    /**
     * Проверка подключения HID устройства через различные методы
     */
    fun isHidDeviceConnected(device: BluetoothDevice, context: Context): Boolean {
        try {
            // Метод 1: Через BluetoothHidDevice профиль (API 28+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                val profileProxy = adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (profile == BluetoothProfile.HID_DEVICE) {
                            val connectedDevices = proxy.connectedDevices
                            Log.d(TAG, "HID connected devices: ${connectedDevices.size}")
                        }
                    }
                    override fun onServiceDisconnected(profile: Int) {}
                }, BluetoothProfile.HID_DEVICE)
            }

            // Метод 2: Через рефлексию для старых версий
            val bluetoothInputDeviceClass = Class.forName("android.bluetooth.BluetoothInputDevice")
            val method: Method = bluetoothInputDeviceClass.getDeclaredMethod("getConnectionState", BluetoothDevice::class.java)

            val adapter = BluetoothAdapter.getDefaultAdapter()
            val profileField = adapter.javaClass.getDeclaredField("INPUT_DEVICE")
            profileField.isAccessible = true
            val profileId = profileField.getInt(null)

            val proxy = adapter.getProfileProxy(context, null, profileId)
            if (proxy != null) {
                val state = method.invoke(proxy, device) as Int
                return state == BluetoothProfile.STATE_CONNECTED
            }

            // Метод 3: Простая проверка через isConnected
            val isConnectedMethod = device.javaClass.getMethod("isConnected")
            return isConnectedMethod.invoke(device) as Boolean

        } catch (e: Exception) {
            Log.e(TAG, "Error checking HID connection", e)

            // Fallback: проверяем через общее состояние подключения
            return try {
                val method = device.javaClass.getDeclaredMethod("isConnected")
                method.isAccessible = true
                method.invoke(device) as Boolean
            } catch (e2: Exception) {
                // Последний способ - считаем подключенным если устройство сопряжено
                device.bondState == BluetoothDevice.BOND_BONDED
            }
        }
    }

    /**
     * Получение списка подключенных HID устройств
     */
    fun getConnectedHidDevices(context: Context): List<BluetoothDevice> {
        val connectedDevices = mutableListOf<BluetoothDevice>()

        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val pairedDevices = adapter.bondedDevices

            pairedDevices.forEach { device ->
                if (isHidDevice(device) && isHidDeviceConnected(device, context)) {
                    connectedDevices.add(device)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connected HID devices", e)
        }

        return connectedDevices
    }

    /**
     * Проверка, является ли устройство HID
     */
    fun isHidDevice(device: BluetoothDevice): Boolean {
        return try {
            val deviceClass = device.bluetoothClass?.deviceClass ?: 0
            // Проверяем класс устройства
            when (deviceClass) {
                0x0540, // Peripheral keyboard
                0x05C0, // Peripheral keyboard/pointing
                0x0580  // Peripheral pointing
                    -> true
                else -> {
                    // Дополнительная проверка по имени
                    val name = device.name?.lowercase() ?: ""
                    name.contains("keyboard") ||
                            name.contains("scanner") ||
                            name.contains("barcode") ||
                            name.contains("hid") ||
                            name.contains("hr32") ||
                            name.contains("newland")
                }
            }
        } catch (e: SecurityException) {
            false
        }
    }
}