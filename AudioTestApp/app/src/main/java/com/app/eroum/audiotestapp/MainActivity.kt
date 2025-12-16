package com.app.eroum.audiotestapp

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.startActivity
import com.app.eroum.audiotestapp.ui.audio.AudioTestScreen
import com.app.eroum.audiotestapp.ui.audio.AudioViewModel
import com.app.eroum.audiotestapp.ui.theme.AudioTestAppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    /** [FIELD] */
    private val vm: AudioViewModel by viewModel()

    private val usbReceiver = object : BroadcastReceiver() {
        // {onReceive}
        @RequiresApi(Build.VERSION_CODES.R)
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            vm.onUsbBroadcast(action)
        }

    }

    @RequiresApi(Build.VERSION_CODES.R)
    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) vm.checkUsbNow()
        }

    /** [LIFE CYCLE METHODS] */
    /** {onCreate} */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val perm = if (Build.VERSION.SDK_INT >= 33)
            android.Manifest.permission.READ_MEDIA_AUDIO
        else
            android.Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            permLauncher.launch(perm)
        }

        setContent { AudioTestScreen(viewModel = vm) }
    }


    /** {onStart} */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStart() {
        super.onStart()

        // USB 장치
        val usbFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        registerReceiver(usbReceiver, usbFilter)

        // Media mount
        val mediaFilter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
            addDataScheme("file")
        }
        registerReceiver(usbReceiver, mediaFilter)
        vm.startUsbMonitor()
        vm.checkUsbNow()
    }


    /** {onStop} */
    override fun onStop() {
        super.onStop()
        vm.stopUsbMonitor()
        unregisterReceiver(usbReceiver)
    }
}

