package com.example.myprinterapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavOptions
import com.example.myprinterapp.printer.ConnectionState as PrinterConnectionState
import com.example.myprinterapp.data.models.ConnectionState as ModelConnectionState
import com.example.myprinterapp.ui.*
import com.example.myprinterapp.ui.log.PrintLogScreen
import com.example.myprinterapp.ui.pick.*
import com.example.myprinterapp.ui.settings.SettingsScreen
import com.example.myprinterapp.ui.settings.BleScannerPairingScreen
import com.example.myprinterapp.ui.settings.BleScannerSettingsScreen
import com.example.myprinterapp.ui.theme.MyPrinterAppTheme
import com.example.myprinterapp.viewmodel.AcceptViewModel
import com.example.myprinterapp.ui.validateAcceptanceQrMask
import com.example.myprinterapp.viewmodel.AcceptUiState
import com.example.myprinterapp.viewmodel.ScannerState
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import com.example.myprinterapp.ui.ScannerState as UiScannerState

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /* ViewModels с поддержкой Hilt */
    private val acceptVm: AcceptViewModel by viewModels()
    private val pickVm: PickViewModel by viewModels()

    // Для обработки результатов запроса разрешений
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Timber.i("All Bluetooth permissions granted")
            onBluetoothPermissionsGranted()
        } else {
            val deniedPermissions = permissions.filterValues { !it }.keys.toList()
            Timber.w("Bluetooth permissions denied: $deniedPermissions")
            onBluetoothPermissionsDenied(deniedPermissions)
        }
    }

    // Колбэки для результатов запроса разрешений
    private var onPermissionsGrantedCallback: (() -> Unit)? = null
    private var onPermissionsDeniedCallback: ((List<String>) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Проверяем разрешения при запуске
        if (!hasBluetoothPermissions()) {
            Timber.d("Bluetooth permissions missing, will request when needed")
        }

        setContent {
            MyPrinterAppTheme {
                val navController = rememberNavController()

                /* ---------- Навигация ---------- */
                NavHost(navController, startDestination = "splash") {

                    /* --- Splash Screen --- */
                    composable("splash") {
                        SplashScreen(
                            onNavigateToMain = {
                                navController.navigate("start") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        )
                    }

                    /* --- стартовое меню --- */
                    composable("start") {
                        StartScreen(
                            onReceiveClick = {
                                navController.navigate("accept")       // к приёмке
                            },
                            onPickClick = {
                                navController.navigate("pick_tasks")   // список заданий
                            },
                            onJournalClick = {
                                navController.navigate("log")          // журнал операций
                            },
                            onSettingsClick = {
                                navController.navigate("settings")     // настройки
                            },
                            onExpressConnectionClick = {
                                navController.navigate("express_connection") // экспресс-подключение
                            }
                        )
                    }

                    /* --- Экспресс-подключение --- */
                    composable("express_connection") {
                        ExpressConnectionScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }

                    /* --- Журнал операций --- */
                    composable("log") {
                        PrintLogScreen(onBack = { navController.popBackStack() })
                    }

                    /* --- Настройки --- */
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToBlePairing = {
                                navController.navigate("ble_scanner_settings")
                            }
                        )
                    }

                    /* --- Настройки BLE сканера --- */
                    composable("ble_scanner_settings") {
                        BleScannerSettingsScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToBlePairing = {
                                // На случай если понадобится дополнительный экран сопряжения
                                navController.navigate("ble_scanner_pairing")
                            }
                        )
                    }

                    /* --- Сопряжение BLE сканера --- */
                    composable("ble_scanner_pairing") {
                        BleScannerPairingScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    /* --- Приёмка --- */
                    composable("accept") {
                        /* подписываемся на VM */
                        val scanned by acceptVm.scannedValue.collectAsState()
                        val qty by acceptVm.quantity.collectAsState()
                        val cell by acceptVm.cellCode.collectAsState()
                        val uiState by acceptVm.uiState.collectAsState()
                        val printerState by acceptVm.printerConnectionState.collectAsState()
                        val scannerState by acceptVm.scannerConnectionState.collectAsState()
                        val showQuantityDialog by acceptVm.showQuantityDialog.collectAsState()
                        val showCellCodeDialog by acceptVm.showCellCodeDialog.collectAsState()

                        AcceptScreen(
                            scannedValue = scanned,
                            quantity = qty,
                            cellCode = cell,
                            uiState = when (val currentUiState = uiState) {
                                is AcceptUiState.Idle -> com.example.myprinterapp.ui.AcceptUiState.Idle
                                is AcceptUiState.Printing -> com.example.myprinterapp.ui.AcceptUiState.Printing
                                is AcceptUiState.Success -> com.example.myprinterapp.ui.AcceptUiState.Success(
                                    currentUiState.message
                                )

                                is AcceptUiState.Error -> com.example.myprinterapp.ui.AcceptUiState.Error(
                                    currentUiState.message
                                )
                            },

                            printerConnectionState = when (printerState) {
                                PrinterConnectionState.CONNECTED -> ModelConnectionState.CONNECTED
                                PrinterConnectionState.CONNECTING -> ModelConnectionState.CONNECTING
                                PrinterConnectionState.DISCONNECTED -> ModelConnectionState.DISCONNECTED
                            },
                            showQuantityDialog = showQuantityDialog,
                            showCellCodeDialog = showCellCodeDialog,
                            onScanWithScanner = { code ->
                                // Проверяем маску перед принятием кода
                                if (validateAcceptanceQrMask(code)) {
                                    acceptVm.onBarcodeDetected(code)
                                }
                            },
                            onScanWithCamera = {
                                navController.navigate("camera")
                            },
                            onQuantityChange = acceptVm::onQuantityChange,
                            onCellCodeChange = acceptVm::onCellCodeChange,
                            onPrintLabel = {
                                requestBluetoothPermissionsIfNeeded(
                                    onGranted = { acceptVm.onPrintLabel() },
                                    onDenied = { permissions ->
                                        Timber.w("Cannot print: missing permissions $permissions")
                                    }
                                )
                            },
                            onResetInputFields = acceptVm::onResetInputFields,
                            onClearMessage = acceptVm::onClearMessage,
                            onBack = { navController.popBackStack() },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            },
                            onNavigateToJournal = {
                                navController.navigate("log")
                            },
                            onNavigateToBleScannerSettings = {
                                navController.navigate("ble_scanner_settings")
                            },
                            onQuantityConfirmed = acceptVm::onQuantityConfirmed,
                            onCellCodeConfirmed = acceptVm::onCellCodeConfirmed,
                            onQuantityDialogDismissed = acceptVm::onQuantityDialogDismissed,
                            onCellCodeDialogDismissed = acceptVm::onCellCodeDialogDismissed,
                            scannerConnectionState = when (scannerState) {
                                ScannerState.CONNECTED -> UiScannerState.CONNECTED
                                ScannerState.DISCONNECTED -> UiScannerState.DISCONNECTED
                            }
                        )
                    }

                    /* --- Камера --- */
                    composable("camera") {
                        CameraScreen(
                            onCodeScanned = { code ->
                                acceptVm.onBarcodeDetected(code)    // сохраняем в VM
                                /* Явно возвращаемся к accept
                                   (если вдруг пользователь пришёл НЕ из accept,
                                   он всё-равно попадёт туда)                   */
                                navController.navigate(
                                    "accept",
                                    NavOptions.Builder()
                                        .setPopUpTo("accept", /*inclusive*/false)
                                        .setLaunchSingleTop(true)   // не плодим дублей
                                        .build()
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    /* --- Список заданий («Комплектация») --- */
                    composable("pick_tasks") {
                        val tasks by pickVm.tasks.collectAsState()

                        PickTasksScreen(
                            tasks = tasks,
                            onOpenTask = { taskId ->
                                println("Debug: Opening task $taskId")
                                pickVm.openTask(taskId)
                                navController.navigate("pick_details")
                            },
                            onBack = { navController.popBackStack() },
                            onImportTasks = {
                                // TODO: Реализовать импорт заданий
                                println("Debug: Import tasks clicked")
                            },
                            onFilterTasks = {
                                // TODO: Реализовать расширенные фильтры
                                println("Debug: Filter tasks clicked")
                            },
                            onMarkAsCompleted = { taskId ->
                                pickVm.markTaskAsCompleted(taskId)
                                println("Debug: Marked task $taskId as completed")
                            },
                            onPauseTask = { taskId ->
                                pickVm.pauseTask(taskId)
                                println("Debug: Paused task $taskId")
                            },
                            onCancelTask = { taskId ->
                                pickVm.cancelTask(taskId)
                                println("Debug: Cancelled task $taskId")
                            }
                        )
                    }

                    /* --- Детали конкретного задания --- */
                    composable("pick_details") {
                        val task by pickVm.currentTask.collectAsState()
                        val dialogFor by pickVm.showQtyDialogFor.collectAsState()
                        val lastCode by pickVm.lastScannedCode.collectAsState()

                        println("Debug: pick_details - task = $task, details = ${task?.details}")

                        PickDetailsEnhanced(
                            task = task,
                            showQtyDialogFor = dialogFor,
                            onShowQtyDialog = pickVm::requestShowQtyDialog,
                            onDismissQtyDialog = pickVm::dismissQtyDialog,
                            onSubmitQty = pickVm::submitPickedQuantity,
                            onScanAnyCode = { code ->
                                pickVm.handleScannedBarcodeForItem(code)
                            },
                            scannedQr = lastCode,
                            onBack = { navController.popBackStack() },
                            onPrintLabel = { detail ->
                                // Проверяем разрешения перед печатью
                                requestBluetoothPermissionsIfNeeded(
                                    onGranted = {
                                        // TODO: Реализовать печать этикетки для детали
                                        println("Debug: Print label for detail ${detail.partNumber}")
                                    },
                                    onDenied = { permissions ->
                                        Timber.w("Cannot print: missing permissions $permissions")
                                    }
                                )
                            },
                            onCompleteTask = {
                                pickVm.markTaskAsCompleted(task?.id ?: "")
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Проверяет, есть ли все необходимые разрешения для Bluetooth
     */
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ требует BLUETOOTH_SCAN и BLUETOOTH_CONNECT
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) && 
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-11 требует ACCESS_FINE_LOCATION для поиска устройств
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // Android < 6.0 - разрешения даются при установке
            true
        }
    }

    /**
     * Проверяет отдельное разрешение
     */
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Запрашивает разрешения Bluetooth если они не предоставлены
     */
    private fun requestBluetoothPermissionsIfNeeded(
        onGranted: () -> Unit,
        onDenied: (List<String>) -> Unit
    ) {
        if (hasBluetoothPermissions()) {
            onGranted()
            return
        }

        // Сохраняем колбэки для последующего использования
        onPermissionsGrantedCallback = onGranted
        onPermissionsDeniedCallback = onDenied

        val permissions = getRequiredPermissions()
        
        if (permissions.isNotEmpty()) {
            Timber.d("Requesting Bluetooth permissions: $permissions")
            bluetoothPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            onGranted()
        }
    }

    /**
     * Получает список необходимых разрешений для текущей версии Android
     */
    private fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            emptyList()
        }
    }

    /**
     * Вызывается когда разрешения предоставлены
     */
    private fun onBluetoothPermissionsGranted() {
        onPermissionsGrantedCallback?.invoke()
        onPermissionsGrantedCallback = null
        onPermissionsDeniedCallback = null
    }

    /**
     * Вызывается когда разрешения отклонены
     */
    private fun onBluetoothPermissionsDenied(deniedPermissions: List<String>) {
        onPermissionsDeniedCallback?.invoke(deniedPermissions)
        onPermissionsGrantedCallback = null
        onPermissionsDeniedCallback = null
    }

    /**
     * Публичный метод для запроса разрешений из настроек
     */
    fun checkAndRequestBluetoothPermissions(
        onGranted: () -> Unit,
        onDenied: (List<String>) -> Unit
    ) {
        requestBluetoothPermissionsIfNeeded(onGranted, onDenied)
    }
}

