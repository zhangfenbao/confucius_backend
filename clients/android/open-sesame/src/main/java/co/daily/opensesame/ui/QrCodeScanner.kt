package co.daily.opensesame.ui

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrCodeScanner(
    onQrCodeScanned: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val cameraPermission = Manifest.permission.CAMERA

    val cameraPermissionState: PermissionState =
        rememberPermissionState(cameraPermission)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->

            val isGranted = permissionsMap[cameraPermission] ?: false

            if (!isGranted) {
                onError("Camera permission denied")
            }
        }
    )

    if (!cameraPermissionState.status.isGranted) {
        LaunchedEffect(Unit) {
            permissionLauncher.launch(arrayOf(cameraPermission))
        }
    } else {

        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        val cameraProvider = remember { cameraProviderFuture.get() }
        val preview = remember { Preview.Builder().build() }

        DisposableEffect(Unit) {
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(context), { image ->

                        val buffer = image.planes[0].buffer

                        val data = ByteArray(buffer.capacity()).apply {
                            buffer.rewind()
                            buffer.get(this)
                        }

                        val reader = MultiFormatReader()
                        reader.setHints(
                            mapOf(
                                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
                            )
                        )

                        try {
                            val result = reader.decode(
                                BinaryBitmap(
                                    HybridBinarizer(
                                        PlanarYUVLuminanceSource(
                                            data,
                                            image.planes[0].rowStride,
                                            image.height,
                                            0,
                                            0,
                                            image.width,
                                            image.height,
                                            false
                                        )
                                    )
                                )
                            )
                            onQrCodeScanned(result.text)

                        } catch (e: NotFoundException) {
                            // Nothing to do
                        } catch (e: Exception) {
                            Log.e("QrCodeScanner", "Exception during QR scan", e)
                        } finally {
                            image.close()
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)

            onDispose {
                cameraProvider.unbindAll()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .animateContentSize(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { viewContext ->
                    PreviewView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    // Attach the surface provider from the preview to the PreviewView
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .fillMaxHeight(0.4f)
                    .aspectRatio(1f)
                    .border(6.dp, Color.Red, RoundedCornerShape(12.dp))
            )
        }
    }
}