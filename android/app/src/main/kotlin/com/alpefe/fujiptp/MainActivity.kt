package com.alpefe.fujiptp

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.alpefe.fujiptp.ui.FujiApp
import com.alpefe.fujiptp.ui.FujiViewModel
import com.alpefe.fujiptp.ui.theme.FujiRecipesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    companion object { private const val TAG = "FujiPtp" }

    private val viewModel: FujiViewModel by viewModels()
    private val usbManager by lazy { FujiUsbManager(this) }

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != FujiUsbManager.ACTION_USB_PERMISSION) return
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            if (granted && device != null) {
                lifecycleScope.launch {
                    val bridge = withContext(Dispatchers.IO) { usbManager.openBridge(device) }
                    viewModel.onBridgeReady(bridge)
                }
            } else {
                viewModel.notifyUser("Permiso USB denegado")
            }
        }
    }

    private val detachReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.notifyUser("Cámara desconectada")
            viewModel.disconnect()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dark window background: prevents the white flash while the
        // Compose content is drawn / during screen transitions.
        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#171310"))

        // Fail fast if the Rust bridge library is missing on this ABI.
        runCatching { FujiNative.nativeVersion() }
            .onSuccess { android.util.Log.i(TAG, "native bridge: $it") }
            .onFailure { e -> android.util.Log.e(TAG, "native bridge unavailable: $e") }

        registerReceiver(
            permissionReceiver,
            IntentFilter(FujiUsbManager.ACTION_USB_PERMISSION),
            if (Build.VERSION.SDK_INT >= 33) Context.RECEIVER_NOT_EXPORTED else 0,
        )
        registerReceiver(
            detachReceiver,
            IntentFilter(UsbAttachReceiver.ACTION_USB_DETACHED),
            if (Build.VERSION.SDK_INT >= 33) Context.RECEIVER_NOT_EXPORTED else 0,
        )

        setContent {
            FujiRecipesTheme {
                // One-shot USB permission requests coming from the UI.
                LaunchedEffect(Unit) {
                    viewModel.permissionRequest.collect { device ->
                        if (device != null) {
                            usbManager.requestPermission(device, permissionReceiver)
                            viewModel.permissionHandled()
                        }
                    }
                }
                FujiApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDevicePresence()
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(permissionReceiver) }
        runCatching { unregisterReceiver(detachReceiver) }
    }
}
