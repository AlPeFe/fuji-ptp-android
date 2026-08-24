package com.alpefe.fujiptp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager

/**
 * Launches the app when a Fujifilm camera is plugged in. Detach events are
 * forwarded to MainActivity through an explicit intent.
 */
class UsbAttachReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val launch = Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(launch)
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val notify = Intent(ACTION_USB_DETACHED).setPackage(context.packageName)
                context.sendBroadcast(notify)
            }
        }
    }

    companion object {
        const val ACTION_USB_DETACHED = "com.alpefe.fujiptp.USB_DETACHED"
    }
}
