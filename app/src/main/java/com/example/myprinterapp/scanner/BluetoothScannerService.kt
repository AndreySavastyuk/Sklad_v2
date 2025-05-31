package com.example.myprinterapp.scanner

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис для работы с Bluetooth-сканерами штрих-кодов
 * Поддерживает Newland HR32-BT и аналогичные HID-сканеры
 */
@Singleton
class BluetoothScannerService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "BluetoothScannerService"

        // Известные имена устройств Newland
        private val NEWLAND_DEVICE_NAMES = listOf(
            "HR32-BT",
            "HR32",
            "NLS-HR32",
            "Newland Scanner"
        )
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter

    // Флаг для отслеживания состояния регистрации receiver'а
    private var isReceiverRegistered = false

    // Состояние подключения сканера
    private val _scannerState = MutableStateFlow(ScannerState.DISCONNECTED)
    val scannerState: StateFlow<ScannerState> = _scannerState

    // Подключенный сканер
    private val _connectedScanner = MutableStateFlow<BluetoothDevice?>(null)
    val connectedScanner: StateFlow<BluetoothDevice?> = _connectedScanner

    // Список доступных сканеров
    private val _availableScanners = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val availableScanners: StateFlow<List<BluetoothDevice>> = _availableScanners

    // Последний отсканированный код
    private val _lastScannedCode = MutableStateFlow<String?>(null)
    val lastScannedCode: StateFlow<String?> = _lastScannedCode

    // BroadcastReceiver для отслеживания состояния Bluetooth
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        if (isScannerDevice(it)) {
                            handleScannerConnected(it)
                        }
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        if (it.address == _connectedScanner.value?.address) {
                            handleScannerDisconnected()
                        }
                    }
                }
            }
        }
    }

    init {
        setupBluetoothListener()
        // Добавляем задержку для инициализации Bluetooth
        CoroutineScope(Dispatchers.IO).launch {
            delay(500) // Даем время на инициализацию
            checkConnectedScanners()
        }
    }

    /**
     * Настройка слушателей Bluetooth
     */
    private fun setupBluetoothListener() {
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(bluetoothReceiver, filter)
            }
            isReceiverRegistered = true
            Log.d(TAG, "Bluetooth receiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering bluetooth receiver", e)
        }
    }

    /**
     * Проверка уже подключенных сканеров
     */
    private fun checkConnectedScanners() {
        try {
            Log.d(TAG, "Checking for connected scanners...")

            val pairedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
            Log.d(TAG, "Found ${pairedDevices.size} paired devices")

            val scanners = pairedDevices.filter { device ->
                val isScanner = isScannerDevice(device)
                if (isScanner) {
                    try {
                        Log.d(TAG, "Found scanner: ${device.name ?: "Unknown"} (${device.address})")
                    } catch (_: SecurityException) {
                        Log.d(TAG, "Found scanner: Unknown name (${device.address})")
                    }
                }
                isScanner
            }

            _availableScanners.value = scanners.toList()

            // Упрощенная проверка: подключаемся к первому найденному сопряженному сканеру,
            // который также числится как подключенный по GATT.
            scanners.firstOrNull { scanner ->
                val connectedGattDevices = try {
                    bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
                } catch (se: SecurityException) {
                    Log.e(TAG, "No permission to get GATT devices", se)
                    emptyList()
                }
                val isConnected = connectedGattDevices.any { it.address == scanner.address }

                try {
                    Log.d(TAG, "Scanner ${scanner.name ?: "Unknown"} (${scanner.address}) - Bonded: ${scanner.bondState == BluetoothDevice.BOND_BONDED}, Connected via GATT: $isConnected")
                } catch (_: SecurityException) {
                    Log.d(TAG, "Scanner ${scanner.address} - Bonded: ${scanner.bondState == BluetoothDevice.BOND_BONDED}, Connected via GATT: $isConnected")
                }
                isConnected // Считаем подключенным, если он есть в списке GATT-подключенных
            }?.let { connectedScannerDevice ->
                handleScannerConnected(connectedScannerDevice)
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception checking paired devices", e)
        }
    }

    /**
     * Проверка, является ли устройство сканером
     */
    private fun isScannerDevice(device: BluetoothDevice): Boolean {
        return try {
            val deviceName = try {
                device.name
            } catch (e: SecurityException) {
                null
            }

            if (deviceName == null) return false

            // Проверяем по известным именам
            NEWLAND_DEVICE_NAMES.any { knownName ->
                deviceName.contains(knownName, ignoreCase = true)
            } ||
                    // Проверяем по классу устройства (HID)
                    device.bluetoothClass?.deviceClass == 0x0540 || // Peripheral keyboard
                    device.bluetoothClass?.deviceClass == 0x05C0 || // Peripheral keyboard/pointing
                    // Проверяем по общим паттернам имен сканеров
                    deviceName.contains("scanner", ignoreCase = true) ||
                    deviceName.contains("barcode", ignoreCase = true) ||
                    deviceName.contains("HID", ignoreCase = true)
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Проверка, подключено ли устройство
     */
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return try {
            // Основная проверка: через BluetoothManager и профиль GATT
            val connectedDevices = try {
                bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
            } catch (se: SecurityException) {
                Log.e(TAG, "No permission to get GATT devices for isDeviceConnected check", se)
                emptyList()
            }
            val isConnectedViaGatt = connectedDevices.any { it.address == device.address }

            // Дополнительная проверка: состояние сопряжения (если устройство сопряжено, это хороший знак)
            val isBonded = try {
                device.bondState == BluetoothDevice.BOND_BONDED
            } catch (e: SecurityException) {
                Log.w(TAG, "Security exception checking bond state for ${device.address}", e)
                false
            }

            Log.d(TAG, "Device ${device.address} - Connected via GATT: $isConnectedViaGatt, Bonded: $isBonded")
            // Считаем устройство подключенным, если оно подключено через GATT
            // или если оно сопряжено и является сканером (как запасной вариант, если GATT не дал точного ответа)
            isConnectedViaGatt || (isBonded && isScannerDevice(device))
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device connection for ${device.address}", e)
            false
        }
    }

    /**
     * Обработка подключения сканера
     */
    private fun handleScannerConnected(device: BluetoothDevice) {
        try {
            val deviceName = try {
                device.name ?: "Unknown"
            } catch (e: SecurityException) {
                "Unknown"
            }
            Log.i(TAG, "Scanner connected: $deviceName (${device.address})")
        } catch (e: SecurityException) {
            Log.i(TAG, "Scanner connected: ${device.address}")
        }

        _connectedScanner.value = device
        _scannerState.value = ScannerState.CONNECTED
    }

    /**
     * Обработка отключения сканера
     */
    private fun handleScannerDisconnected() {
        Log.i(TAG, "Scanner disconnected")
        _connectedScanner.value = null
        _scannerState.value = ScannerState.DISCONNECTED
    }

    /**
     * Обработка отсканированного кода
     * В HID режиме коды приходят как ввод с клавиатуры
     */
    fun processScannedData(data: String) {
        if (data.isNotBlank()) {
            _lastScannedCode.value = data
            Log.d(TAG, "Scanned code: $data")
        }
    }

    /**
     * Очистка последнего отсканированного кода
     */
    fun clearLastScannedCode() {
        _lastScannedCode.value = null
    }

    /**
     * Получение списка доступных сканеров
     */
    fun getAvailableScanners(): List<BluetoothDevice> {
        return try {
            val pairedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
            pairedDevices.filter { isScannerDevice(it) }.toList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting paired devices", e)
            emptyList()
        }
    }

    /**
     * Инструкции по настройке HR32-BT
     */
    fun getSetupInstructions(): String {
        return """
            Настройка сканера HR32-BT:
            
            1. Включите сканер (удерживайте кнопку питания)
            2. Войдите в режим сопряжения:
               - Отсканируйте штрих-код "Enter Setup"
               - Отсканируйте "Bluetooth HID Mode"
               - Отсканируйте "Pair Mode"
            3. Найдите устройство "HR32-BT" в настройках Bluetooth Android
            4. Выполните сопряжение (PIN обычно 0000 или 1234)
            5. Отсканируйте "Exit Setup"
            
            После настройки сканер будет автоматически подключаться при включении.
        """.trimIndent()
    }

    /**
     * Принудительная проверка подключенных сканеров
     */
    fun forceCheckConnection() {
        checkConnectedScanners()
    }

    /**
     * Безопасная отмена регистрации receiver'а
     */
    private fun unregisterReceiverSafely() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(bluetoothReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "Bluetooth receiver unregistered successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering bluetooth receiver", e)
            }
        }
    }

    /**
     * Очистка ресурсов
     */
    fun cleanup() {
        unregisterReceiverSafely()
    }
}

/**
 * Состояние подключения сканера
 */
enum class ScannerState {
    DISCONNECTED,
    CONNECTED
}
