package com.example.myprinterapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.compose.*
import com.example.myprinterapp.ui.*
import com.example.myprinterapp.ui.log.PrintLogScreen
import com.example.myprinterapp.ui.pick.*
import com.example.myprinterapp.ui.demo.ScannerTestDemo
import com.example.myprinterapp.ui.settings.SettingsScreen
import com.example.myprinterapp.ui.theme.MyPrinterAppTheme
import com.example.myprinterapp.viewmodel.AcceptViewModel
import com.example.myprinterapp.viewmodel.NewlandScannerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Один Accept-VM на всё Activity, чтобы CameraScreen мог изменять его state */
    private val acceptVm: AcceptViewModel by viewModels()

    /** Один Pick-VM на всё Activity, чтобы состояние сохранялось между экранами */
    private val pickVm: PickViewModel by viewModels()

    /** BLE сканер ViewModel для всего приложения */
    private val newlandVm: NewlandScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyPrinterAppTheme {
                val navController = rememberNavController()

                /* ---------- Навигация ---------- */
                NavHost(navController, startDestination = "start") {

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
                            onScannerTestClick = {
                                navController.navigate("scanner_config") // тестирование сканера
                            }
                        )
                    }

                    /* --- Журнал операций --- */
                    composable("log") {
                        PrintLogScreen(onBack = { navController.popBackStack() })
                    }

                    /* --- Настройки --- */
                    composable("settings") {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }

                    /* --- Настройка сканера --- */
                    composable("scanner_config") {
                        ScannerTestDemo(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    /* --- Приёмка с интеграцией BLE --- */
                    composable("accept") {
                        /* подписываемся на VM */
                        val scanned by acceptVm.scannedValue.collectAsState()
                        val qty by acceptVm.quantity.collectAsState()
                        val cell by acceptVm.cellCode.collectAsState()
                        val uiState by acceptVm.uiState.collectAsState()
                        val printerState by acceptVm.printerConnectionState.collectAsState()
                        val scannerState by acceptVm.scannerConnectionState.collectAsState()

                        AcceptScreen(
                            scannedValue = scanned,
                            quantity = qty,
                            cellCode = cell,
                            uiState = uiState,
                            printerConnectionState = printerState,
                            scannerConnectionState = scannerState,
                            onScanWithScanner = { code ->
                                acceptVm.onBarcodeDetected(code)
                            },
                            onScanWithCamera = {
                                /* Переходим к камере, но оставляем Accept в back-stack */
                                navController.navigate("camera")
                            },
                            onQuantityChange = acceptVm::onQuantityChange,
                            onCellCodeChange = acceptVm::onCellCodeChange,
                            onPrintLabel = acceptVm::onPrintLabel,
                            onResetInputFields = acceptVm::resetInputFields,
                            onClearMessage = acceptVm::clearMessage,
                            onBack = { navController.popBackStack() },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            },
                            viewModel = acceptVm,
                            newlandViewModel = newlandVm // Передаем BLE ViewModel
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

                    /* --- Список заданий («Комплектация») с BLE поддержкой --- */
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
                            },
                            // Добавляем поддержку BLE сканера
                            newlandViewModel = newlandVm
                        )
                    }

                    /* --- Детали конкретного задания с BLE поддержкой --- */
                    composable("pick_details") {
                        val task by pickVm.currentTask.collectAsState()
                        val dialogFor by pickVm.showQtyDialogFor.collectAsState()
                        val lastCode by pickVm.lastScannedCode.collectAsState()

                        println("Debug: pick_details - task = $task, details = ${task?.details}")

                        // Используем улучшенную версию с BLE поддержкой
                        PickDetailsEnhanced(
                            task = task,
                            showQtyDialogFor = dialogFor,
                            onShowQtyDialog = pickVm::requestShowQtyDialog,
                            onDismissQtyDialog = pickVm::dismissQtyDialog,
                            onSubmitQty = pickVm::submitPickedQuantity,
                            onScanAnyCode = { code ->
                                // Поддерживаем как HID, так и BLE сканеры
                                pickVm.handleScannedBarcodeForItem(code)
                            },
                            scannedQr = lastCode,
                            onBack = { navController.popBackStack() },
                            onPrintLabel = { detail ->
                                // TODO: Реализовать печать этикетки для детали
                                println("Debug: Print label for detail ${detail.partNumber}")
                            },
                            onCompleteTask = {
                                pickVm.markTaskAsCompleted(task?.id ?: "")
                                navController.popBackStack()
                            }
                        )
                    }

                    /* --- Тестирование BLE сканера --- */
                    composable("ble_test") {
                        BleTestScreen(
                            viewModel = newlandVm,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Очищаем BLE ресурсы при закрытии приложения
        newlandVm.clearScanCallback()
    }
}

// Дополнительный экран для тестирования BLE (опционально)
@Composable
fun BleTestScreen(
    viewModel: NewlandScannerViewModel,
    onBack: () -> Unit
) {
    // Простой экран для тестирования BLE функциональности
    // Может быть полезен для отладки
    ScannerTestDemo(onBack = onBack)
}