package com.example.myprinterapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myprinterapp.printer.PrinterService
import com.example.myprinterapp.ui.AcceptScreen
import com.example.myprinterapp.ui.CameraScreen
import com.example.myprinterapp.ui.StartScreen
import com.example.myprinterapp.ui.pick.PickTasksScreen
import com.example.myprinterapp.ui.pick.PickDetailsScreen
import com.example.myprinterapp.ui.pick.PickViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // -- состояния для экрана Приемка
            var acceptScannedValue by rememberSaveable { mutableStateOf<String?>(null) }
            var quantity           by rememberSaveable { mutableStateOf("") }
            var cellCode           by rememberSaveable { mutableStateOf("") }

            val pickVm: PickViewModel = viewModel()
            val printerService = remember { PrinterService(this) }
            val navController = rememberNavController()

            NavHost(navController, startDestination = "start_screen") {
                // === стартовое меню
                composable("start_screen") {
                    StartScreen(
                        onReceiveClick  = { navController.navigate("accept") },
                        onPickClick     = { navController.navigate("pick_tasks") },
                        onJournalClick  = { /* TODO */ },
                        onSettingsClick = { /* TODO */ }
                    )
                }

                // === Приемка
                composable("accept") {
                    AcceptScreen(
                        scannedValue      = acceptScannedValue,
                        quantity          = quantity,
                        cellCode          = cellCode,
                        onScanWithScanner = { /* TODO: Bluetooth */ },
                        onScanWithCamera  = { navController.navigate("camera_accept") },
                        onQuantityChange  = { quantity = it },
                        onCellCodeChange  = { cellCode = it },
                        onPrintLabel      = {
                            if (acceptScannedValue != null && quantity.isNotBlank() && cellCode.isNotBlank()) {
                                printerService.printFromScanned(
                                    qrData   = acceptScannedValue!!,
                                    quantity = quantity,
                                    cellCode = cellCode
                                )
                                // после печати можно сбросить поля
                                acceptScannedValue = null
                                quantity = ""
                                cellCode = ""
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onResetInputFields = {
                            acceptScannedValue = null
                            quantity = ""
                            cellCode = ""
                        }
                    )
                }

                // Камера для экрана Приемка
                composable("camera_accept") {
                    CameraScreen(
                        onBarcodeDetected = { code ->
                            acceptScannedValue = code
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // === Список заданий на комплектацию
                composable("pick_tasks") {
                    val tasks by pickVm.tasks.collectAsState()
                    PickTasksScreen(
                        tasks      = tasks,
                        onOpenTask = { taskId ->
                            pickVm.openTask(taskId)
                            navController.navigate("pick_details")
                        },
                        onBack     = { navController.popBackStack() }
                    )
                }

                // === Детали конкретного задания ===
                composable("pick_details") {
                    val vm = hiltViewModel<PickViewModel>()
                    PickDetailsScreen(
                        task              = vm.currentTask.collectAsState().value,
                        showQtyDialogFor  = vm.showQtyDialogFor.collectAsState().value,
                        onScanAnyCode     = { code -> vm.handleScannedBarcodeForItem(code) },
                        onShowQtyDialog   = { detail -> vm.requestShowQtyDialog(detail) },
                        onSubmitQty       = { id, qty -> vm.submitPickedQuantity(id, qty) },
                        onDismissQtyDialog= { vm.dismissQtyDialog() },
                        onBack            = { navController.popBackStack() }
                    )
                }

                // Камера для режима Комплектации
                composable("camera_pick") {
                    CameraScreen(
                        onBarcodeDetected = { code ->
                            pickVm.handleScannedBarcodeForItem(code)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // TODO: journal, settings и т.д.
            }
        }
    }
}
