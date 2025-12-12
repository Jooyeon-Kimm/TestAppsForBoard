package com.app.eroum.cameratestapp.service

import android.Manifest
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Log.e
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresPermission

class CameraController(
    private val context: Context,
    private val cameraManager: CameraManager,
    private val cameraId: String,
) {
    companion object {
        private const val TAG = "CameraController"
    }

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var textureView: TextureView? = null

    /**
     * Preview TextureView 붙이기
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    fun attachTextureView(tv: TextureView) {
        textureView = tv
        tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            @RequiresPermission(Manifest.permission.CAMERA)
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
                startPreviewInternal()
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                stopPreview()
                return true
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        // 이미 준비된 경우도 커버
        if (tv.isAvailable) {
            startPreviewInternal()
        }
    }

    /**
    Preview 시작
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    fun startPreview() {
        startPreviewInternal()
    }

    /**
    Camera 열기
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    private fun openCamera() {
        val stateCallback = object : CameraDevice.StateCallback() {

            // {onDisconnected}
            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
                Log.d("[CameraDevice.StateCallback]", "Camera ${camera.id} Disconnected.")
            }

            // {onError}
            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
                Log.d("[CameraDevice.StateCallback]", "Camera ${camera.id} Error: $error")
            }

            // {onOpened}
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createPreviewSession()
                Log.d("[CameraDevice.StateCallback]", "Camera ${camera.id} Opened.")
            }

        }

        try {
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: Exception) {
            Log.w(TAG, "[openCamera Err] $e")
        }

    }

    /**
    Preview Session 생성
     */
    private fun createPreviewSession() {
        val tv = textureView ?: return
        val surfaceTexture = tv.surfaceTexture ?: return

        surfaceTexture.setDefaultBufferSize(tv.width, tv.height)
        val surface = Surface(surfaceTexture)

        try {
            val camera = cameraDevice ?: return
            previewRequestBuilder =
                camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(surface)
                    set(
                        CaptureRequest.CONTROL_MODE,
                        CaptureRequest.CONTROL_MODE_AUTO
                    )
                }

            camera.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    // {onConfigureFailed}
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.w(TAG, "[createPreviewSession] onConfigureFailed")
                    }

                    // {onConfigured}
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        try {
                            val request = previewRequestBuilder?.build()
                            if (request != null) {
                                session.setRepeatingRequest(
                                    request,
                                    null,
                                    backgroundHandler
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "[createPreviewSession] onConfigured Err: $e")
                        }
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.w(TAG, "[createPreviewSession] Err: $e")
        }
    }

    /**
     * Preview 중지
     */
    fun stopPreview() {
        try {
            captureSession?.close()
            captureSession = null
        } catch (e: Exception) {
            Log.w(TAG, "[stopPreview] Close Capture Session Err: $e")
        }

        try {
            cameraDevice?.close()
            cameraDevice = null
        } catch (e: Exception) {
            Log.w(TAG, "[stopPreview] Close Camera Device Err: $e")
        }

        stopBackgroundThread()
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private fun startPreviewInternal() {
        if (textureView == null) return
        startBackgroundThread()
        openCamera()
    }

    /**
     * Preview 스레드 시작
     */
    private fun startBackgroundThread() {
        if (backgroundThread != null) return
        backgroundThread = HandlerThread("CameraBackground_$cameraId").apply {
            start()
            backgroundHandler = Handler(looper)
        }
    }

    /**
     * Preview 스레드 중지
     */
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
        } catch (e: InterruptedException) {
            // InterruptedException: 쓰레드야, 그만해/깨워 라는 신호를 담은 예외
            //            Exception: 모든 예외의 부모 타입
            Log.w(TAG, "[stopBackgroundThread] $e")
        }
        backgroundThread = null
        backgroundHandler = null
    }

}