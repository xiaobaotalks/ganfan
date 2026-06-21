package com.mqwen.scandeals

import android.util.Log

/**
 * JNI bridge to MNN runtime + Qwen3-VL-2B model.
 * Loads libMNN.so first (per MNN tutorial), then our libscandeals.so.
 */
object MnnBridge {
    private const val TAG = "MnnBridge"

    init {
        try {
            System.loadLibrary("MNN")
            Log.i(TAG, "libMNN.so loaded")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load libMNN.so: ${e.message}")
            throw e
        }
        try {
            System.loadLibrary("scandeals")
            Log.i(TAG, "libscandeals.so loaded")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load libscandeals.so: ${e.message}")
            throw e
        }
    }

    external fun nativeInit(configPath: String, cacheDir: String): Boolean
    external fun nativeRunVlm(imagePath: String, prompt: String): String
    external fun nativeRuntimeInfo(): String
    external fun nativeRelease()

    fun init(configPath: String, cacheDir: String): Boolean = nativeInit(configPath, cacheDir)
    fun runVlm(imagePath: String, prompt: String): String = nativeRunVlm(imagePath, prompt)
    fun runtimeInfo(): String = nativeRuntimeInfo()
    fun release() = nativeRelease()
}