package com.app.eroum.cameratestapp

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.app.eroum.cameratestapp.ui.camera.CameraTestScreen
import com.app.eroum.cameratestapp.ui.camera.checkCameraPermission

class MainActivity : ComponentActivity() {
    private lateinit var cameraManager: CameraManager

    /** Field */
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    private var hasCameraPermission by mutableStateOf(false)

    /** Lifecycle Method */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // 카메라 권한 확인
        hasCameraPermission = checkCameraPermission(this, permissionLauncher)

        // 상/하단 시스템 바 투명하게 만들어서
        // 화면을 끝(edge)까지 쓰게 해주는 함수
        enableEdgeToEdge()

        setContent {
            if (hasCameraPermission) {
                CameraTestScreen(
                    cameraManager = cameraManager,
                )
            }
        }
    }
}
