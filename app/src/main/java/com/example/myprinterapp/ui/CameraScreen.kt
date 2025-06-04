package com.example.myprinterapp.ui

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay

/**
 * Экран для сканирования QR через камеру с помощью CameraX и ML Kit.
 */

@Composable
fun CameraScreen(
    onCodeScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val previewView = remember { PreviewView(context) }
    var hasPermission by remember { mutableStateOf(false) }
    var showQrError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Запросчик разрешений камеры
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    // Проверяем разрешение камеры при запуске
    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Автоматически скрываем сообщение об ошибке через 3 секунды
    LaunchedEffect(showQrError) {
        if (showQrError) {
            delay(3000)
            showQrError = false
        }
    }

    // Настраиваем камеру при получении разрешения
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()
            }, ContextCompat.getMainExecutor(context))
        }
    }

    // Настраиваем CameraX при получении provider
    LaunchedEffect(cameraProvider) {
        cameraProvider?.let { provider ->
            try {
                provider.unbindAll()

                val preview = androidx.camera.core.Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    processImageProxy(imageProxy) { qrCode ->
                        // Проверяем маску QR для приемки (значение=значение=значение=значение)
                        val parts = qrCode.split('=')
                        if (parts.size == 4 && parts.all { it.isNotBlank() }) {
                            onCodeScanned(qrCode)
                        } else {
                            // Показываем сообщение об ошибке
                            errorMessage = "QR не распознан"
                            showQrError = true
                        }
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                provider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

            } catch (e: Exception) {
                Log.e("CameraScreen", "Ошибка настройки камеры", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Кнопка возврата
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Инструкция для пользователя
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Наведите камеру на QR-код",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Сообщение об ошибке QR
            if (showQrError) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Предупреждение",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = errorMessage,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

        } else {
            // Экран запроса разрешений
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Нужно разрешение на использование камеры",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Для сканирования QR-кодов приложению необходим доступ к камере",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                ) {
                    Text("Предоставить разрешение")
                }
            }
        }
    }
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    onQrCodeDetected: (String) -> Unit
) {
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    val scanner = BarcodeScanning.getClient(options)
    val image = imageProxy.image
    
    if (image != null) {
        val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { value ->
                        onQrCodeDetected(value)
                        return@addOnSuccessListener
                    }
                }
            }
            .addOnFailureListener {
                Log.e("CameraScreen", "Ошибка распознавания QR-кода", it)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}