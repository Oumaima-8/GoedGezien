
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myindiproject.camera.BarcodeAnalyzer
import com.example.myindiproject.data.ApiClient
import com.example.myindiproject.data.Product
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var barcode by remember { mutableStateOf<String?>(null) }
    var product by remember { mutableStateOf<Product?>(null) }
    var loading by remember { mutableStateOf(false) }

    var message by remember {
        mutableStateOf("Point the camera at a food barcode.")
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
        }

    fun lookupProduct(code: String) {
        barcode = code
        product = null
        loading = true
        message = "Looking up product..."

        scope.launch {
            try {
                val response = ApiClient.api.getProduct(code)

                if (response.status == 1 && response.product != null) {
                    product = response.product
                    message = "Product found."
                } else {
                    message = "Product not found in Open Food Facts."
                }
            } catch (e: Exception) {
                Log.e("CameraScreen", "Product lookup failed", e)
                message = "Could not load product. Check your internet connection."
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Food Scanner",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasPermission) {
            Text(
                text = "Allow camera access to scan food products.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            ) {
                Text("Allow camera")
            }
        } else if (barcode == null) {
            Text(
                text = "Hold the barcode in front of the camera.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            CameraPreview(
                onBarcodeDetected = { code ->
                    if (barcode == null && !loading) {
                        lookupProduct(code)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (loading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium
        )

        barcode?.let { code ->
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Barcode: $code",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        product?.let { item ->
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = item.productName
                    ?.takeIf { it.isNotBlank() }
                    ?: "Product name unavailable",
                style = MaterialTheme.typography.headlineSmall
            )

            item.brands
                ?.takeIf { it.isNotBlank() }
                ?.let { brand ->
                    Text(
                        text = "Brand: $brand",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ingredients",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.ingredients
                    ?.takeIf { it.isNotBlank() }
                    ?: "Ingredient information is unavailable.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (barcode != null) {
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    barcode = null
                    product = null
                    loading = false
                    message = "Point the camera at a food barcode."
                }
            ) {
                Text("Scan another product")
            }

            if (!loading && product == null) {
                TextButton(
                    onClick = {
                        barcode?.let { lookupProduct(it) }
                    }
                ) {
                    Text("Try again")
                }
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
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        var analyzer: BarcodeAnalyzer? = null
        var disposed = false

        cameraProviderFuture.addListener({
            if (disposed) return@addListener

            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

                val barcodeAnalyzer = BarcodeAnalyzer { code ->
                    ContextCompat.getMainExecutor(context).execute {
                        onBarcodeDetected(code)
                    }
                }

                analyzer = barcodeAnalyzer

                imageAnalysis.setAnalyzer(
                    cameraExecutor,
                    barcodeAnalyzer
                )

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(
                    "CameraPreview",
                    "Failed to start camera",
                    e
                )
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            analyzer?.close()
            cameraExecutor.shutdown()

            if (cameraProviderFuture.isDone) {
                try {
                    cameraProviderFuture.get().unbindAll()
                } catch (e: Exception) {
                    Log.e(
                        "CameraPreview",
                        "Failed to stop camera",
                        e
                    )
                }
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
