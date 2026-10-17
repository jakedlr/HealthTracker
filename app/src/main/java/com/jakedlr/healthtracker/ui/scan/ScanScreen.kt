package com.jakedlr.healthtracker.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jakedlr.healthtracker.ui.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onKnownItem: (itemId: Long) -> Unit,
    onUnknownBarcode: (barcode: String) -> Unit,
    onEnterManually: () -> Unit,
    viewModel: ScanViewModel = viewModel(factory = ScanViewModel.Factory),
) {
    val result by viewModel.result.collectAsStateWithLifecycle()
    LaunchedEffect(result) {
        when (val r = result) {
            is ScanResult.KnownItem -> onKnownItem(r.itemId)
            is ScanResult.UnknownBarcode -> onUnknownBarcode(r.barcode)
            null -> Unit
        }
    }

    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        askedOnce = true
    }
    LaunchedEffect(Unit) {
        if (!hasPermission && !askedOnce) launcher.launch(Manifest.permission.CAMERA)
    }
    // Picks up a grant made in system Settings.
    LifecycleResumeEffect(Unit) {
        hasPermission = context.hasCameraPermission()
        onPauseOrDispose { }
    }

    var torchOn by rememberSaveable { mutableStateOf(false) }
    val hasFlash = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan barcode") },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (hasPermission && hasFlash) {
                        TextButton(onClick = { torchOn = !torchOn }) { Text(if (torchOn) "Light off" else "Light on") }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                hasPermission -> CameraPreview(torchOn, viewModel::onBarcodeScanned)
                askedOnce -> PermissionDenied(
                    canAskAgain = context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true,
                    onAskAgain = { launcher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    },
                    onEnterManually = onEnterManually,
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(torchOn: Boolean, onBarcode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analyzer = remember { BarcodeAnalyzer(onBarcode) }
    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context), analyzer)
        }
    }
    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.unbind()
            controller.clearImageAnalysisAnalyzer()
            analyzer.close()
        }
    }
    LaunchedEffect(torchOn) { controller.enableTorch(torchOn) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        // Aiming guide; the whole frame is analyzed.
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.8f)
                .height(140.dp)
                .border(3.dp, Color.White, RoundedCornerShape(12.dp)),
        )
        Text(
            "Point the camera at a product barcode",
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
        )
    }
}

@Composable
private fun PermissionDenied(
    canAskAgain: Boolean,
    onAskAgain: () -> Unit,
    onOpenSettings: () -> Unit,
    onEnterManually: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Scanning needs the camera. Images are processed on this phone and never leave it.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (canAskAgain) {
            Button(onClick = onAskAgain, modifier = Modifier.fillMaxWidth()) { Text("Allow camera") }
        } else {
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("Open settings") }
        }
        OutlinedButton(onClick = onEnterManually, modifier = Modifier.fillMaxWidth()) { Text("Enter manually") }
    }
}

private fun Context.hasCameraPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
