package com.alpefe.fujiptp.data

import com.alpefe.fujiptp.FujiNative
import com.alpefe.fujiptp.UsbIo
import org.json.JSONArray
import org.json.JSONObject

/**
 * High-level facade over the Rust bridge. All methods are synchronous and
 * must be called from a background dispatcher.
 *
 * Protocol stays 100% in Rust; this class only translates JSON DTOs.
 */
class CameraClient(private val bridge: UsbIo) {

    private val sessionId = 1

    fun connect(): Result<String> = runCatching {
        nativeOk(FujiNative.nativeConnect(bridge, sessionId)) { "connect" }
    }

    fun openSession(): Result<String> = runCatching {
        nativeOk(FujiNative.nativeOpenSession(sessionId)) { "open session" }
    }

    fun closeSession(): Result<String> = runCatching {
        // Idempotent: the Rust side also tolerates closing a session that
        // already failed; a stale session is dropped on next connect anyway.
        nativeOk(FujiNative.nativeCloseSession()) { "close session" }
    }

    fun close(): Result<String> = runCatching {
        // Always succeeds: drops the controller and releases the transport.
        val json = JSONObject(FujiNative.nativeClose())
        if (!json.optBoolean("ok", false)) {
            throw IllegalStateException(json.optString("error", "close failed"))
        }
        json.toString()
    }

    fun readRecipes(): Result<List<RecipeModel>> = runCatching {
        val json = JSONObject(FujiNative.nativeReadRecipes())
        if (!json.optBoolean("ok", false)) {
            throw IllegalStateException(json.optString("error", "read failed"))
        }
        // {"ok":true,"profile":{"name":...,"recipes":[...]}} — serialized by
        // fuji-ptp-core (serde feature), parsed here as plain JSON.
        val profile = json.optJSONObject("profile")
            ?: throw IllegalStateException("read failed: missing profile")
        val recipes = profile.optJSONArray("recipes") ?: JSONArray()
        List(recipes.length()) { i -> RecipeModel.fromNativeJson(recipes.getJSONObject(i)) }
    }

    fun writeRecipe(slot: Int, recipe: RecipeModel): Result<String> = runCatching {
        nativeOk(FujiNative.nativeWriteRecipe(slot, recipe.toNativeJson())) { "write C$slot" }
    }

    /** Writes recipe settings only; the camera keeps its slot name. */
    fun writeRecipeSettings(slot: Int, recipe: RecipeModel): Result<String> = runCatching {
        nativeOk(FujiNative.nativeWriteRecipeSettings(slot, recipe.toNativeJson())) { "write settings C$slot" }
    }

    fun writeRecipeNames(names: List<String>): Result<String> = runCatching {
        val payload = JSONArray().apply { names.forEach { put(it) } }.toString()
        nativeOk(FujiNative.nativeWriteRecipeNames(payload)) { "write names" }
    }

    private fun nativeOk(raw: String, op: () -> String): String {
        val json = JSONObject(raw)
        if (!json.optBoolean("ok", false)) {
            throw IllegalStateException("${op()} failed: ${json.optString("error", "unknown error")}")
        }
        return json.toString()
    }
}
