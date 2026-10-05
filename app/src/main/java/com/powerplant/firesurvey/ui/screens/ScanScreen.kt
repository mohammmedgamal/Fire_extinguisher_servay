package com.powerplant.firesurvey.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onBack: () -> Unit, onCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    var torchOn by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    // Guard so a QR code seen on several consecutive frames only navigates once.
    var handled by remember { mutableStateOf(false) }
    val onCode: (String) -> Unit = { code ->
        if (!handled) {
            handled = true
            onCodeScanned(code)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan extinguisher QR") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (hasPermission) {
                        IconButton(onClick = { torchOn = !torchOn }) {
                            Icon(if (torchOn) Icons.Filled.FlashlightOff else Icons.Filled.FlashlightOn, "Toggle flashlight")
                        }
                    }
                    IconButton(onClick = { showManual = true }) { Icon(Icons.Filled.Keyboard, "Enter code manually") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (hasPermission) {
                CameraPreview(torchOn = torchOn, onCode = onCode)
                // Viewfinder frame
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(260.dp)
                        .border(BorderStroke(3.dp, Color.White), RoundedCornerShape(16.dp)),
                )
                Text(
                    "Point the camera at the QR label on the extinguisher",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp),
                )
            } else {
                Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Camera permission is needed to scan QR codes.", textAlign = TextAlign.Center)
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Grant camera permission") }
                    OutlinedButton(onClick = { showManual = true }) { Text("Enter code manually") }
                }
            }
        }
    }

    if (showManual) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManual = false },
            title = { Text("Enter extinguisher code") },
            text = {
                OutlinedTextField(value = code, onValueChange = { code = it }, singleLine = true, label = { Text("Code printed under the QR") })
            },
            confirmButton = {
                TextButton(enabled = code.isNotBlank(), onClick = { showManual = false; onCode(code.trim()) }) { Text("Open") }
            },
            dismissButton = { TextButton(onClick = { showManual = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CameraPreview(torchOn: Boolean, onCode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context) }
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }

    DisposableEffect(lifecycleOwner) {
        val executor = ContextCompat.getMainExecutor(context)
        controller.setImageAnalysisAnalyzer(
            executor,
            MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, executor) { result ->
                val value = result.getValue(scanner)
                    ?.firstNotNullOfOrNull { it.rawValue?.trim()?.takeIf(String::isNotEmpty) }
                if (value != null) onCode(value)
            },
        )
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            scanner.close()
        }
    }

    LaunchedEffect(torchOn) { controller.enableTorch(torchOn) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        },
    )
}
