package com.example.myprinterapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.compose.*
import com.example.myprinterapp.ui.*
import com.example.myprinterapp.ui.log.PrintLogScreen
import com.example.myprinterapp.ui.pick.*
import com.example.myprinterapp.ui.settings.SettingsScreen
import com.example.myprinterapp.ui.theme.MyPrinterAppTheme
import com.example.myprinterapp.viewmodel.AcceptViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Один Accept-VM на всё Activity, чтобы CameraScreen мог изменять его state */
    private val acceptVm: AcceptViewModel by viewModels()

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

                        AcceptScreen(
                            scannedValue = scanned,
                            quantity = qty,
                            cellCode = cell,
                            uiState = uiState,
                            printerConnectionState = printerState,
                            onScanWithScanner = {/* TODO */},
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
                            onNavigateToSettings = { navController.navigate("settings") },
                            viewModel = acceptVm  // Передаем viewModel
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
                        val pickVm: PickViewModel = viewModel()
                        val tasks = pickVm.tasks.collectAsState().value

                        PickTasksScreen(
                            tasks = tasks,
                            onOpenTask = { id ->
                                pickVm.openTask(id)
                                navController.navigate("pick_details")
                            },
                            onBack = { navController.popBackStack() },
                            onImportTasks = { /* TODO: Импорт заданий */ },
                            onFilterTasks = { /* TODO: Фильтрация */ }
                        )
                    }

                    /* --- Детали конкретного задания --- */
                    composable("pick_details") {
                        val pickVm: PickViewModel = viewModel()
                        val task by pickVm.currentTask.collectAsState()
                        val dialogFor by pickVm.showQtyDialogFor.collectAsState()
                        val lastCode by pickVm.lastScannedCode.collectAsState()

                        PickDetailsScreen(
                            task = task,
                            showQtyDialogFor = dialogFor,
                            onShowQtyDialog = pickVm::requestShowQtyDialog,
                            onDismissQtyDialog = pickVm::dismissQtyDialog,
                            onSubmitQty = pickVm::submitPickedQuantity,
                            onScanAnyCode = pickVm::handleScannedBarcodeForItem,
                            onSubmitPickedQty = pickVm::submitPickedQuantity, // alias
                            scannedQr = lastCode,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}