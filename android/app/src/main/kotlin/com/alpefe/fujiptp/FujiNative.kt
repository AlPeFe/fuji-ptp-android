package com.alpefe.fujiptp

/**
 * JNI boundary. Every method returns a JSON string:
 *   - success: {"ok":true} (readRecipes adds "recipes":[...])
 *   - failure: {"ok":false,"error":"..."}
 *
 * The Rust side owns the whole PTP/Fujifilm protocol; Kotlin only supplies
 * raw USB bytes through [UsbIo].
 */
object FujiNative {
    init { System.loadLibrary("fuji_ptp_android") }

    external fun nativeVersion(): String
    external fun nativeConnect(bridge: UsbIo, sessionId: Int): String
    external fun nativeOpenSession(sessionId: Int): String
    external fun nativeCloseSession(): String
    external fun nativeReadRecipes(): String
    external fun nativeWriteRecipe(slot: Int, recipeJson: String): String
    external fun nativeWriteRecipeNames(namesJson: String): String
    external fun nativeClose(): String
}
