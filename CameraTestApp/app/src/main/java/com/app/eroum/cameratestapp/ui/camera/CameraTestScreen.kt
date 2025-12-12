package com.app.eroum.cameratestapp.ui.camera

import android.Manifest
import android.R.attr.text
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.view.TextureView
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.app.eroum.cameratestapp.service.CameraController
import java.util.stream.Collectors.toList


@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.RequiresPermission(android.Manifest.permission.CAMERA)
@Composable
fun CameraTestScreen(
    cameraManager: CameraManager,
) {
    val context = LocalContext.current
    val cameraIds = remember {
        // 논리 카메라 1개 drop
        // 필요 시, dropLast(1) 제거
        cameraManager.cameraIdList.dropLast(1).toList()
    }

    Scaffold(
        topBar = {
            // Ethernet Test App과 달리 TopAppBar(ExperimentalMaterial3Api 사용)
            TopAppBar(
                title = {
                    Text(
                        text = "Camera Test App",
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->

        // 카메라가 없으면
        if (cameraIds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("This device does NOT HAVE CAMERA.")
            }
        }

        // 카메라가 있으면
        else {
            // 카메라 개수만큼 프리뷰 보여주기
            // LazyColumn == RecyclerView 비슷
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(top = 12.dp), // TopAppBar와 Card 사이 상하 패딩
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cameraIds) { cameraId ->
                    CameraPreviewCard(
                        cameraId = cameraId,
                        cameraManager = cameraManager
                    )
                }
            }
        }
    }

}

@Composable
@androidx.annotation.RequiresPermission(android.Manifest.permission.CAMERA)
fun CameraPreviewCard(
    cameraId: String,
    cameraManager: CameraManager,
) {
    val context = LocalContext.current
    var isPreviewing by remember { mutableStateOf(true) }

    // 카메라 : 컨트롤러 = 1 : 1
    val cameraController = remember {
        CameraController(
            context = context,
            cameraManager = cameraManager,
            cameraId = cameraId
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .border(1.dp, Color.Gray)
            .padding(8.dp)
    ) {
        Text(
            text = "Camera ID: $cameraId",
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // 카메라 프리뷰 (TextureView)
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(getPreviewAspectRatio(cameraManager, cameraId)),
            factory = { ctx ->
                TextureView(ctx).also { textureView ->
                    cameraController.attachTextureView(textureView)
                    isPreviewing = true
                }
            },
            update = {
                // [TODO] 갱신 필요 시
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        // STOP 버튼
        OutlinedButton(
            onClick = {
                if (isPreviewing) {
                    cameraController.stopPreview()
                } else {
                    cameraController.startPreview()
                }
                isPreviewing = !isPreviewing
            }

        ) {
            Text(if (isPreviewing) "Stop Preview" else "Start Preview")
        }
    }

    // 컴포저블이 없어질 때, 리소스 정리
    DisposableEffect(Unit) {
        onDispose {
            cameraController.stopPreview()
        }
    }
}

@Preview
@Composable
@androidx.annotation.RequiresPermission(android.Manifest.permission.CAMERA)
fun CameraTestScreenPreview() {
    lateinit var cameraManager: CameraManager

    CameraTestScreen(cameraManager = cameraManager)
}

/*
 * 카메라 권한 확인
 */
fun checkCameraPermission(
    context: Context,
    permissionLauncher: ActivityResultLauncher<String>,
): Boolean {
    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    // 카메라 권한 없으면 추가
    if (!hasCameraPermission) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    return hasCameraPermission
}

/*
    카메라 화면 비율 가져오기
 */
fun getPreviewAspectRatio(
    cameraManager: CameraManager,
    cameraId: String,
): Float {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    val map = characteristics.get(
        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
    )

    // 보통 가장 큰 프리뷰 사이즈 사용
    val size = map!!
        .getOutputSizes(SurfaceTexture::class.java)
        .maxBy { it.width * it.height }

    return size.width.toFloat() / size.height.toFloat()
}
