package com.alpefe.fujiptp

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.content.BroadcastReceiver

import android.os.Build

/**
 * Raw bulk I/O contract used by the Rust bridge. `UsbIoBridge` is the real
 * USB implementation; the interface exists so the bridge can be swapped in
 * tests and so the JNI layer only ever depends on `send`/`receive`.
 */
interface UsbIo {
    /** Bulk OUT. Returns bytes written, or a negative value on failure. */
    fun send(data: ByteArray): Int

    /** Bulk IN. Returns up to [size] bytes; may throw on failure. */
    fun receive(size: Int): ByteArray

    fun close()
}

/**
 * Android USB Host implementation. The whole PTP/Fujifilm protocol lives in
 * Rust; this class only does USB discovery, permission, and bulk I/O.
 */
class FujiUsbManager(private val context: Context) {
    private val usb = context.getSystemService(Context.USB_SERVICE) as UsbManager

    fun findPtpCamera(): UsbDevice? = usb.deviceList.values.firstOrNull { device ->
        device.vendorId == FUJI_VENDOR_ID && findPtpInterface(device) != null
    }

    fun hasPermission(device: UsbDevice): Boolean = usb.hasPermission(device)

    /**
     * Requests USB permission. The caller must already have registered
     * [receiver] for [ACTION_USB_PERMISSION]; the result arrives there.
     */
    fun requestPermission(device: UsbDevice, receiver: BroadcastReceiver) {
        val flags = if (Build.VERSION.SDK_INT >= 31) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val intent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION).setPackage(context.packageName), flags,
        )
        usb.requestPermission(device, intent)
    }

    /** Opens the PTP interface and hands back a working [UsbIoBridge]. */
    fun openBridge(device: UsbDevice): UsbIoBridge {
        val ptp = findPtpInterface(device) ?: error("Fujifilm PTP interface not found")
        val connection = usb.openDevice(device) ?: error("USB permission denied")
        check(connection.claimInterface(ptp.usbInterface, true)) { "Unable to claim PTP interface" }
        return UsbIoBridge(connection, ptp.usbInterface, ptp.bulkIn, ptp.bulkOut)
    }

    /**
     * Locates the PTP interface: prefer class 0x06 (PTP/imaging), fall back to
     * any interface exposing a bulk IN/OUT endpoint pair (some Fuji bodies
     * present a vendor-specific interface).
     */
    private fun findPtpInterface(device: UsbDevice): PtpInterface? {
        var fallback: PtpInterface? = null
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            val pair = findBulkPair(iface) ?: continue
            if (iface.interfaceClass == PTP_CLASS) return pair
            if (fallback == null) fallback = pair
        }
        return fallback
    }

    private fun findBulkPair(iface: UsbInterface): PtpInterface? {
        var input: UsbEndpoint? = null
        var output: UsbEndpoint? = null
        for (e in 0 until iface.endpointCount) {
            val endpoint = iface.getEndpoint(e)
            if (endpoint.type != UsbConstants.USB_ENDPOINT_XFER_BULK) continue
            if (endpoint.direction == UsbConstants.USB_DIR_IN) input = endpoint else output = endpoint
        }
        return if (input != null && output != null) PtpInterface(iface, input!!, output!!) else null
    }

    private data class PtpInterface(
        val usbInterface: UsbInterface,
        val bulkIn: UsbEndpoint,
        val bulkOut: UsbEndpoint,
    )

    companion object {
        const val ACTION_USB_PERMISSION = "com.alpefe.fujiptp.USB_PERMISSION"
        const val FUJI_VENDOR_ID = 0x04CB
        private const val PTP_CLASS = 0x06
    }
}

/** USB bulk I/O over a claimed interface. Owns the [UsbDeviceConnection]. */
class UsbIoBridge(
    private val connection: UsbDeviceConnection,
    private val claimedInterface: UsbInterface,
    private val bulkIn: UsbEndpoint,
    private val bulkOut: UsbEndpoint,
) : UsbIo {
    override fun send(data: ByteArray): Int {
        val count = connection.bulkTransfer(bulkOut, data, data.size, TIMEOUT)
        android.util.Log.d(TAG, "USB TX ${data.size}B -> $count (first ${data.take(12).joinToString { "%02x".format(it) }})")
        return count
    }

    override fun receive(size: Int): ByteArray {
        // `size` is always a full bulk packet (512) from the Rust transport,
        // so a single bulkTransfer never drops trailing bytes.
        val buffer = ByteArray(size)
        val count = connection.bulkTransfer(bulkIn, buffer, size, TIMEOUT)
        android.util.Log.d(TAG, "USB RX <- $count of $size (first ${buffer.take(12).joinToString { "%02x".format(it) }})")
        check(count >= 0) { "USB receive failed: $count" }
        return buffer.copyOf(count)
    }

    override fun close() {
        runCatching { connection.releaseInterface(claimedInterface) }
        runCatching { connection.close() }
    }

    companion object {
        // Generous timeout: the X100VI can take several seconds to answer
        // the first property reads after OPEN_SESSION.
        private const val TIMEOUT = 10000
        private const val TAG = "FujiPtpUsb"
    }
}
