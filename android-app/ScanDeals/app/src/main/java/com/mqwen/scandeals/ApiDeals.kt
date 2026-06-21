package com.mqwen.scandeals

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 半真实数据层(方案 B)
 *
 * - 招牌识别 → 真实店名 → 用高德 POI 搜索拿真实店地址/评分/经纬度
 * - 套餐价格仍走 deeplink 跳 4 官方 App 实时查询(合法)
 * - 销量/标注:本地合理预估(写明"预估销量",不假装真实)
 *
 * 流程:
 * 1. ApiStoreFinder.search(name, userLat, userLng) → StoreInfo?
 *    a. 先查本地 cache(ConcurrentHashMap, 24h 过期)
 *    b. miss → 高德 text 搜索 API → 解析 JSON → 返回
 *    c. 网络失败/解析失败 → fallback MockDeals.findInfo()
 *    d. 都 miss → null
 * 2. 拿到 StoreInfo 后,ResultActivity 用真实数据渲染(地址/评分/距离/经纬度)
 *    套餐仍是 MockDeals 5 平台(抖音/美团/美团外卖/淘宝闪购/京东秒送)
 *    但 deeplink 跳转时实时查 4 App 拿真实价格
 *
 * 用户配置:
 * - 高德 Web 服务 Key: https://lbs.amap.com/dev/key/app 注册
 * - 默认占位 "YOUR_AMAP_KEY_HERE",代码检测到占位时直接走 MockDeals
 */
object ApiDeals {

    /** 高德 Web 服务 API Key(占位符,请在 local.properties 中配置 amap.key 或在 App 内手动设置) */
    private const val AMAP_KEY = BuildConfig.AMAP_KEY

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /** 本地缓存 key=name@city → StoreInfo */
    private data class CacheEntry(val info: MockDeals.StoreInfo, val timestamp: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 24 * 3600 * 1000L

    /** 状态:用户是否开了"联网模式"(toggle) */
    @Volatile var onlineMode: Boolean = false

    /**
     * 招牌识别后的店名 → 拿到最匹配的 POI 真实信息
     * @param storeName VLM 识别的店名(可能错字/简写)
     * @param userLat 用户 GPS(用于排序和距离)
     * @param userLng 用户 GPS
     * @return MatchResult? (带距离)
     */
    suspend fun search(storeName: String, userLat: Double?, userLng: Double?): MockDeals.MatchResult? {
        if (storeName.isBlank()) return null

        // 0) 离线模式或未配置 Key:直接走 MockDeals
        if (!onlineMode || AMAP_KEY == "YOUR_AMAP_KEY_HERE") {
            return MockDeals.findMatch(storeName, userLat, userLng)
        }

        // 1) 查 cache
        val cacheKey = cacheKey(storeName)
        cache[cacheKey]?.let { entry ->
            if (System.currentTimeMillis() - entry.timestamp < CACHE_TTL_MS) {
                return wrapDistance(entry.info, userLat, userLng)
            } else {
                cache.remove(cacheKey)
            }
        }

        // 2) 高德 POI 搜索
        val real = queryAmap(storeName)
        if (real != null) {
            cache[cacheKey] = CacheEntry(real, System.currentTimeMillis())
            return wrapDistance(real, userLat, userLng)
        }

        // 3) Fallback MockDeals(招牌识别对了但高德没收录 → 用本地预设)
        return MockDeals.findMatch(storeName, userLat, userLng)
    }

    private fun cacheKey(name: String): String = name.trim().lowercase()

    private fun wrapDistance(info: MockDeals.StoreInfo, userLat: Double?, userLng: Double?): MockDeals.MatchResult {
        return if (userLat != null && userLng != null) {
            MockDeals.MatchResult(info, info.distanceTo(userLat, userLng))
        } else {
            MockDeals.MatchResult(info, 0.0)
        }
    }

    /**
     * 高德 text 搜索
     * URL: https://restapi.amap.com/v3/place/text?key={K}&keywords={name}&city=北京&offset=5
     * 返回字段:name, address, location(lng,lat), biz_ext.rating
     */
    private suspend fun queryAmap(keywords: String): MockDeals.StoreInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://restapi.amap.com/v3/place/text" +
                    "?key=$AMAP_KEY" +
                    "&keywords=${java.net.URLEncoder.encode(keywords, "UTF-8")}" +
                    "&city=北京" +
                    "&offset=5" +
                    "&extensions=base"

            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                if (json.optString("status") != "1") return@withContext null

                val pois = json.optJSONArray("pois") ?: return@withContext null
                if (pois.length() == 0) return@withContext null

                // 取第 1 个 POI(高德默认按相关度排序)
                val first = pois.getJSONObject(0)
                val name = first.optString("name").ifBlank { keywords }
                val address = first.optString("address").ifBlank { first.optString("adname", "未知地址") }
                val location = first.optString("location") // "lng,lat"
                val locParts = location.split(",")
                if (locParts.size != 2) return@withContext null
                val lng = locParts[0].toDouble()
                val lat = locParts[1].toDouble()
                val rating = first.optJSONObject("biz_ext")?.optString("rating")?.takeIf { it.isNotBlank() } ?: "4.5"

                // 套餐:拿 MockDeals 的预设(同名匹配)
                val mockInfo = MockDeals.findInfo(name)
                    ?: MockDeals.findInfo(extractPureName(name))
                val deals = mockInfo?.deals ?: defaultDeals(name)

                MockDeals.StoreInfo(name, address, rating, lat, lng, deals)
            }
        } catch (_: Throwable) {
            null
        }
    }

    /** 从高德店名提取纯店名(去掉"咖啡(中关村店)"括号) */
    private fun extractPureName(name: String): String {
        return name.replace(Regex("[(\\s\\(].*$"), "").trim()
    }

    /** 默认 5 平台兜底套餐(招牌对但 MockDeals 没收录 → 给 5 平台 deeplink 占位) */
    private fun defaultDeals(name: String): List<MockDeals.Deal> {
        val q = java.net.URLEncoder.encode(name, "UTF-8")
        return listOf(
            MockDeals.Deal("抖音团购", "🎵", "douyin", "团购套餐", 0.0, "预估销量", "实时刷新", "团购"),
            MockDeals.Deal("美团", "🟢", "meituan", "到店套餐", 0.0, "预估销量", "实时刷新", "团购"),
            MockDeals.Deal("美团外卖", "🛵", "meituan_wm", "外卖套餐", 0.0, "预估销量", "实时配送", "外卖"),
            MockDeals.Deal("淘宝闪购", "🟧", "taobao", "闪购套餐", 0.0, "预估销量", "实时配送", "外卖"),
            MockDeals.Deal("京东秒送", "🟧", "jdmiaosong", "秒送套餐", 0.0, "预估销量", "极速达", "外卖")
        )
    }

    /** 清空缓存(供设置/调试用) */
    fun clearCache() {
        cache.clear()
    }
}