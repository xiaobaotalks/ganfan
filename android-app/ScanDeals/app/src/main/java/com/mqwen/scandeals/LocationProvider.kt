package com.mqwen.scandeals

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 简单定位工具(用系统 LocationManager,不依赖 Google Play 服务)
 * 优先取 last known location,无则 5s 超时返回 null
 */
object LocationProvider {
    data class LatLng(val lat: Double, val lng: Double)

    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(ctx: Context, timeoutMs: Long = 5000): LatLng? {
        if (!hasPermission(ctx)) return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        // 1) 优先 last known
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER)
        var best: Location? = null
        for (p in providers) {
            try {
                if (!lm.isProviderEnabled(p)) continue
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.time > best.time) best = loc
            } catch (_: Throwable) { }
        }
        if (best != null) return LatLng(best.latitude, best.longitude)
        // 2) 无 last known,尝试请求一次(5s 超时)
        return withTimeoutOrNull(timeoutMs) { requestSingleUpdate(ctx, lm) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleUpdate(ctx: Context, lm: LocationManager): LatLng? =
        suspendCancellableCoroutine { cont ->
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    try { lm.removeUpdates(this) } catch (_: Throwable) {}
                    if (cont.isActive) cont.resume(LatLng(location.latitude, location.longitude))
                }
                @Deprecated("override required") override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {}
            }
            val provider = when {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
            if (provider == null) { cont.resume(null); return@suspendCancellableCoroutine }
            try {
                lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                cont.invokeOnCancellation { try { lm.removeUpdates(listener) } catch (_: Throwable) {} }
            } catch (_: Throwable) {
                cont.resume(null)
            }
        }
}
