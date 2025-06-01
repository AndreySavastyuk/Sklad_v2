package com.example.myprinterapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavOptions
import com.example.myprinterapp.ui.*
import com.example.myprinterapp.ui.log.PrintLogScreen
import com.example.myprinterapp.ui.pick.*
import com.example.myprinterapp.ui.settings.SettingsScreen
import com.example.myprinterapp.ui.theme.MyPrinterAppTheme
import com.example.myprinterapp.viewmodel.AcceptViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /* ViewModels с поддержкой Hilt */
    private val acceptVm: AcceptViewModel by viewModels()
    private val pickVm: PickViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

                    /* --- Приёмка --- */
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
                            uiState = when (val currentUiState = uiState) {
                                is com.example.myprinterapp.viewmodel.AcceptUiState.Idle -> com.example.myprinterapp.ui.AcceptUiState.Idle
                                is com.example.myprinterapp.viewmodel.AcceptUiState.Printing -> com.example.myprinterapp.ui.AcceptUiState.Printing
                                is com.example.myprinterapp.viewmodel.AcceptUiState.Success -> com.example.myprinterapp.ui.AcceptUiState.Success(currentUiState.message)
                                is com.example.myprinterapp.viewmodel.AcceptUiState.Error -> com.example.myprinterapp.ui.AcceptUiState.Error(currentUiState.message)
                            },
                            printerConnectionState = printerState,
                            scannerConnectionState = when (scannerState) {
                                com.example.myprinterapp.viewmodel.ScannerState.CONNECTED -> com.example.myprinterapp.ui.ScannerState.CONNECTED
                                com.example.myprinterapp.viewmodel.ScannerState.DISCONNECTED -> com.example.myprinterapp.ui.ScannerState.DISCONNECTED
                            },
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
                            onResetInputFields = acceptVm::onResetInputFields,
                            onClearMessage = acceptVm::onClearMessage,
                            onBack = { navController.popBackStack() },
                            onNavigateToSettings = {
                                navController.navigate("settings")
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
                                // TODO: Реализовать печать этикетки для детали
                                println("Debug: Print label for detail ${detail.partNumber}")
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
}