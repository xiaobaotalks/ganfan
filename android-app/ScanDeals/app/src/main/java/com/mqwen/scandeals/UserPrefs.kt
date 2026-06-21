package com.mqwen.scandeals

import android.content.Context
import android.content.SharedPreferences

/**
 * 全局用户偏好(SharedPreferences 持久化)
 * - onlineMode: 联网模式开关(默认 false = 完全离线)
 * - amapKey: 高德 API Key(可选,首次使用联网模式时引导用户填写)
 */
object UserPrefs {
    private const val PREFS_NAME = "scan_deals_prefs"
    private const val KEY_ONLINE_MODE = "online_mode"
    private const val KEY_AMAP_KEY = "amap_key"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var onlineMode: Boolean
        get() = ApiDeals.onlineMode
        set(value) { ApiDeals.onlineMode = value }

    fun saveOnlineMode(ctx: Context, enabled: Boolean) {
        ApiDeals.onlineMode = enabled
        prefs(ctx).edit().putBoolean(KEY_ONLINE_MODE, enabled).apply()
    }

    fun loadOnlineMode(ctx: Context): Boolean {
        val v = prefs(ctx).getBoolean(KEY_ONLINE_MODE, false)
        ApiDeals.onlineMode = v
        return v
    }

    /** 持久化高德 Key(用户填过则保存) */
    fun saveAmapKey(ctx: Context, key: String) {
        prefs(ctx).edit().putString(KEY_AMAP_KEY, key).apply()
    }

    fun loadAmapKey(ctx: Context): String? =
        prefs(ctx).getString(KEY_AMAP_KEY, null)
}