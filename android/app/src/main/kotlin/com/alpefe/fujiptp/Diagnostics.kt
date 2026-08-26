package com.alpefe.fujiptp

import java.util.Collections

/**
 * In-app diagnostics: captures native bridge logs (forwarded from Rust via
 * JNI) plus USB logs, so the user can debug camera I/O without a PC.
 */
object Diagnostics {
    private val lines = Collections.synchronizedList(ArrayDeque<String>())
    private const val MAX_LINES = 500

    fun log(tag: String, msg: String) {
        val line = "${System.currentTimeMillis() % 100000} [$tag] $msg"
        synchronized(lines) {
            lines.addLast(line)
            while (lines.size > MAX_LINES) lines.removeFirst()
        }
    }

    /** Called from Rust (JNI) for every native log line. */
    @JvmStatic
    fun onNativeLog(msg: String) {
        log("NATIVE", msg)
    }

    fun snapshot(): List<String> = synchronized(lines) { lines.toList() }

    fun clear() = synchronized(lines) { lines.clear() }
}
