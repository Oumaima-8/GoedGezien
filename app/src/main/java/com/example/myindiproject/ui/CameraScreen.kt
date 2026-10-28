package com.example.myindiproject.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myindiproject.camera.BarcodeAnalyzer
import com.example.myindiproject.viewmodel.ProductUiState
import com.example.myindiproject.viewmodel.ProductViewModel
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    productViewModel: ProductViewModel = viewModel(),
    userAllergies: List<String> = emptyList()
) {
    val context = LocalContext.current
    val state by productViewModel.state.collectAsState()
    var lastScannedBarcode by remember { mutableStateOf<String?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    when (val currentState = state) {
        ProductUiState.Idle -> {
            if (!hasCameraPermission) {
                CameraPermissionScreen(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            } else {
                ScanScreenContent(
                    onBarcodeDetected = { barcode ->
                        lastScannedBarcode = barcode
                        productViewModel.loadProduct(barcode)
                    }
                )
            }
        }

        ProductUiState.Loading -> {
            StatusScreen(
                title = "Even geduld…",
                message = "We zoeken de productinformatie.",
                showLoading = true
            )
        }

        is ProductUiState.Success -> {
            ProductScreen(
                product = currentState.product,
                userAllergies = userAllergies,
                onBackToScan = productViewModel::reset
            )
        }

        is ProductUiState.NotFound -> {
            StatusScreen(
                title = "Product niet gevonden",
                message = "We hebben geen bruikbare informatie bij deze barcode. " +
                        "Lees de verpakking.",
                actionLabel = "Opnieuw scannen",
                onAction = productViewModel::reset
            )
        }

        is ProductUiState.Error -> {
            StatusScreen(
                title = "Geen verbinding",
                message = currentState.message,
                actionLabel = if (lastScannedBarcode != null) "Probeer opnieuw" else "Opnieuw scannen",
                onAction = {
                    val barcode = lastScannedBarcode
                    if (barcode == null) {
                        productViewModel.reset()
                    } else {
                        productViewModel.loadProduct(barcode)
                    }
                },
                secondaryActionLabel = "Opnieuw scannen",
                onSecondaryAction = productViewModel::reset
            )
        }
    }
}

@Composable
private fun CameraPermissionScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mag Goed Gezien de camera gebruiken?",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "De camera is nodig om de barcode op uw product te lezen.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onRequestPermission) {
            Text("Camera toestaan")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "U kunt de barcode ook handmatig invoeren.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ScanScreenContent(
    onBarcodeDetected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Scan een product",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Houd de barcode in het kader. De scan start vanzelf.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        CameraPreview(
            onBarcodeDetected = onBarcodeDetected,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Houd uw telefoon even stil.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatusScreen(
    title: String,
    message: String,
    showLoading: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )

        if (showLoading) {
            Spacer(modifier = Modifier.height(20.dp))
            CircularProgressIndicator()
        }

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }

        if (secondaryActionLabel != null && onSecondaryAction != null) {
            TextButton(onClick = onSecondaryAction) {
                Text(secondaryActionLabel)
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        var analyzer: BarcodeAnalyzer? = null
        var disposed = false

        cameraProviderFuture.addListener({
            if (disposed) return@addListener

            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also { useCase ->
                        useCase.surfaceProvider = previewView.surfaceProvider
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

                val barcodeAnalyzer = BarcodeAnalyzer { barcode ->
                    ContextCompat.getMainExecutor(context).execute {
                        if (!disposed) {
                            onBarcodeDetected(barcode)
                        }
                    }
                }

                analyzer = barcodeAnalyzer
                imageAnalysis.setAnalyzer(cameraExecutor, barcodeAnalyzer)

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (exception: Exception) {
                Log.e("CameraPreview", "Failed to start camera", exception)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            analyzer?.close()
            cameraExecutor.shutdown()

            if (cameraProviderFuture.isDone) {
                try {
                    cameraProviderFuture.get().unbindAll()
                } catch (exception: Exception) {
                    Log.e("CameraPreview", "Failed to stop camera", exception)
                }
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}