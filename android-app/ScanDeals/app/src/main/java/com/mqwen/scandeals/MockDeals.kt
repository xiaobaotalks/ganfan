package com.mqwen.scandeals

import java.net.URLEncoder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 6 套餐多平台比价数据库 + 定位匹配(7 品牌 × 3-4 分店)
 * - 每个品牌:共享 5 平台套餐 + 多分店(地址/评分/经纬度)
 * - 定位匹配:招牌识别 → 关键词命中品牌 → Haversine 选最近分店
 */
object MockDeals {

    data class Deal(
        val platform: String,
        val platformIcon: String,
        val platformKey: String,
        val name: String,
        val price: Double,
        val sales: String,
        val note: String = "",
        val group: String = "团购"
    ) {
        fun deeplink(storeName: String): String = when (platformKey) {
            "douyin" -> "snssdk1128://search/result?keyword=${URLEncoder.encode(storeName, "UTF-8")}"
            "meituan" -> "imeituan://www.meituan.com/search?q=${URLEncoder.encode(storeName, "UTF-8")}"
            "meituan_wm" -> "imeituan://waimai/meishi/waimai/search?keyword=${URLEncoder.encode(storeName, "UTF-8")}"
            "taobao" -> "taobao://s.taobao.com/search?q=${URLEncoder.encode(storeName, "UTF-8")}"
            "jdmiaosong" -> "openapp.jdmobile://virtual?params={%22category%22:%22jump%22,%22des%22:%22search%22,%22keyWord%22:%22${URLEncoder.encode(storeName, "UTF-8")}%22}&channelId=03"
            else -> "snssdk1128://"
        }

        fun webFallback(storeName: String, lat: Double?, lng: Double?): String {
            val q = URLEncoder.encode(storeName, "UTF-8")
            val location = if (lat != null && lng != null) {
                val locParam = "$lat,$lng"
                "&location=$locParam&lat=$lat&lng=$lng"
            } else ""
            return when (platformKey) {
                "douyin" -> "https://www.douyin.com/search/$q?type=shop"
                "meituan" -> "https://i.meituan.com/awp/h5/article/searchresult.html?keyword=$q$location"
                "meituan_wm" -> "https://i.meituan.com/awp/h5/article/waimaiseachresult.html?keyword=$q$location"
                "taobao" -> "https://s.taobao.com/search?q=$q"
                "jdmiaosong" -> "https://daojia.jd.com/search?keyword=$q&type=0&t=1"
                else -> "https://www.baidu.com/s?wd=$q"
            }
        }
    }

    data class StoreInfo(
        val name: String,
        val address: String,
        val rating: String,
        val lat: Double,
        val lng: Double,
        val deals: List<Deal>
    ) {
        fun distanceTo(lat: Double, lng: Double): Double {
            val r = 6371000.0
            val dLat = Math.toRadians(lat - this.lat)
            val dLng = Math.toRadians(lng - this.lng)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(this.lat)) * cos(Math.toRadians(lat)) *
                    sin(dLng / 2) * sin(dLng / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }

    data class MatchResult(
        val info: StoreInfo,
        val distanceMeters: Double
    ) {
        val distanceText: String
            get() = when {
                distanceMeters < 1000 -> "${distanceMeters.toInt()}m"
                else -> "%.1fkm".format(distanceMeters / 1000.0)
            }
    }

    /** 一个品牌的多个分店(共享 5 平台套餐,不同地址) */
    data class Brand(
        val brand: String,
        val deals: List<Deal>,
        val stores: List<StoreInfo>
    ) {
        fun nearestStore(userLat: Double?, userLng: Double?): StoreInfo {
            if (stores.isEmpty()) error("品牌 $brand 无分店")
            if (userLat == null || userLng == null) return stores.first()
            return stores.minBy { it.distanceTo(userLat, userLng) }
        }
        fun nearestMatch(userLat: Double?, userLng: Double?): MatchResult {
            val s = nearestStore(userLat, userLng)
            val d = if (userLat == null || userLng == null) 0.0
                    else s.distanceTo(userLat, userLng)
            return MatchResult(s, d)
        }
    }

    private fun deal(
        p: String, icon: String, key: String, name: String,
        price: Double, sales: String, note: String = "", group: String = "团购"
    ) = Deal(p, icon, key, name, price, sales, note, group)

    // 7 品牌 × 3-4 分店,招牌识别后按 GPS 选最近
    private val brands: List<Brand> = listOf(
        Brand(
            brand = "瑞幸",
            deals = listOf(
                deal("抖音团购", "🎵", "douyin", "9.9 元任选券", 9.9, "10万+", "限时秒杀"),
                deal("美团", "🟢", "meituan", "生椰拿铁套餐", 15.9, "5万+", "美团专享"),
                deal("美团外卖", "🛵", "meituan_wm", "外卖专享", 12.8, "1万+", "免配送", "外卖"),
                deal("淘宝闪购", "🟧", "taobao", "咖啡+轻食券", 13.8, "2万+", "外卖专享", "外卖"),
                deal("京东秒送", "🟧", "jdmiaosong", "大师咖啡券", 18.0, "8000+", "极速配送", "外卖")
            ),
            stores = listOf(
                StoreInfo("瑞幸咖啡(中关村店)", "海淀区中关村大街 1 号", "4.6", 39.9840, 116.3120, emptyList()),
                StoreInfo("瑞幸(学院路店)", "海淀区学院路 35 号", "4.5", 39.9912, 116.3195, emptyList()),
                StoreInfo("瑞幸(五道口店)", "海淀区成府路 28 号", "4.7", 39.9925, 116.3370, emptyList()),
                StoreInfo("瑞幸(上地店)", "海淀区上地信息路 9 号", "4.4", 40.0310, 116.3085, emptyList())
            )
        ),
        Brand(
            brand = "星巴克",
            deals = listOf(
                deal("抖音团购", "🎵", "douyin", "35 元代金券", 30.0, "8万+", "周三特惠"),
                deal("美团", "🟢", "meituan", "星冰乐 6 选 1", 28.0, "4万+", "美团会员价"),
                deal("美团外卖", "🛵", "meituan_wm", "专星送", 32.0, "8000+", "会员免运", "外卖"),
                deal("淘宝闪购", "🟧", "taobao", "外送专享杯", 35.0, "1.5万+", "免配送费", "外卖"),
                deal("京东秒送", "🟧", "jdmiaosong", "37 元双人套餐", 37.0, "1.2万+", "", "外卖")
            ),
            stores = listOf(
                StoreInfo("星巴克(中关村店)", "海淀区中关村大街 18 号", "4.8", 39.9855, 116.3108, emptyList()),
                StoreInfo("星巴克(五道口店)", "海淀区成府路 30 号", "4.7", 39.9930, 116.3360, emptyList()),
                StoreInfo("星巴克(学院路店)", "海淀区学院路 40 号", "4.6", 39.9920, 116.3205, emptyList())
            )
        ),
        Brand(
            brand = "麦当劳",
            deals = listOf(
                deal("抖音团购", "🎵", "douyin", "巨无霸套餐", 25.9, "12万+", "抖音独家"),
                deal("美团", "🟢", "meituan", "麦辣鸡腿堡餐", 22.9, "8万+", "美团专享"),
                deal("美团外卖", "🛵", "meituan_wm", "麦乐送", 19.9, "3万+", "29 分钟达", "外卖"),
                deal("淘宝闪购", "🟧", "taobao", "麦满分早餐", 11.9, "3万+", "早餐时段", "外卖"),
                deal("京东秒送", "🟧", "jdmiaosong", "家庭欢享餐", 49.9, "1.5万+", "3-4 人餐", "外卖")
            ),
            stores = listOf(
                StoreInfo("麦当劳(中关村南店)", "海淀区中关村南大街 5 号", "4.5", 39.9788, 116.3150, emptyList()),
                StoreInfo("麦当劳(五道口店)", "海淀区成府路 28 号 1F", "4.4", 39.9928, 116.3362, emptyList()),
                StoreInfo("麦当劳(苏州街店)", "海淀区苏州街 12 号", "4.5", 39.9805, 116.3095, emptyList()),
                StoreInfo("麦当劳(上地店)", "海淀区上地信息路 12 号", "4.3", 40.0315, 116.3090, emptyList())
            )
        ),
        Brand(
            brand = "海底捞",
            deals = listOf(
                deal("抖音团购", "🎵", "douyin", "4 人套餐", 299.0, "3万+", "周末可用"),
                deal("美团", "🟢", "meituan", "双人浪漫餐", 188.0, "2万+", "美团用户"),
                deal("美团外卖", "🛵", "meituan_wm", "捞派送", 168.0, "5000+", "锅具同送", "外卖"),
                deal("淘宝闪购", "🟧", "taobao", "2-3 人餐", 168.0, "1.2万+", "外送", "外卖"),
                deal("京东秒送", "🟧", "jdmiaosong", "企业 6 人餐", 599.0, "3000+", "发票专享", "外卖")
            ),
            stores = listOf(
                StoreInfo("海底捞火锅(中关村店)", "海淀区科学院南路 2 号", "4.9", 39.9820, 116.3250, emptyList()),
                StoreInfo("海底捞(学院路店)", "海淀区学院路 38 号", "4.8", 39.9920, 116.3210, emptyList()),
                StoreInfo("海底捞(五道口店)", "海淀区成府路 35 号 B1", "4.7", 39.9935, 116.3375, emptyList())
            )
        ),
        Brand(
            brand = "肯德基",
            deals = listOf(
                deal("抖音团购", "🎵", "douyin", "全家桶", 88.0, "15万+", "限时特惠"),
                deal("美团", "🟢", "meituan", "黄金鸡块 5 块", 12.9, "10万+", "美团会员"),
                deal("美团外卖", "🛵", "meituan_wm", "宅急送", 25.9, "2万+", "满 49 免运", "外卖"),
                deal("淘宝闪购", "🟧", "taobao", "吮指原味鸡", 13.5, "4万+", "外卖专享", "外卖"),
                deal("京东秒送", "🟧", "jdmiaosong", "宅急送双人餐", 49.9, "2万+", "", "外卖")
            ),
            stores = listOf(
                StoreInfo("肯德基(中关村店)", "海淀区中关村大街 27 号", "4.4", 39.9860, 116.3095, emptyList()),
                StoreInfo("肯德基(苏州街店)", "海淀区苏州街 18 号", "4.3", 39.9810, 116.3100, emptyList()),
                StoreInfo("肯德基(五道口店)", "海淀区成府路 32 号", "4.4", 39.9932, 116.3368, emptyList())
            )
        ),
        Brand(
            brand = "爆汁小龙虾",
            deals = listOf(
                deal("抖音团购", "🎵", "douyin", "59 元 3 斤套餐", 59.0, "8万+", "限时秒杀"),
                deal("美团", "🟢", "meituan", "麻辣小龙虾 2 斤", 88.0, "5万+", "美团专享"),
                deal("美团外卖", "🛵", "meituan_wm", "夜宵专送", 45.0, "4000+", "凌晨到天亮", "外卖"),
                deal("淘宝闪购", "🟧", "taobao", "蒜蓉小龙虾", 78.0, "1.5万+", "外送免运费", "外卖"),
                deal("京东秒送", "🟧", "jdmiaosong", "海鲜大拼盘", 158.0, "2000+", "4-6 人餐", "外卖")
            ),
            stores = listOf(
                StoreInfo("爆汁小龙虾(夜宵店)", "海淀区中关村大街 38 号", "4.5", 39.9870, 116.3070, emptyList()),
                StoreInfo("爆汁小龙虾(五道口店)", "海淀区成府路 38 号", "4.4", 39.9938, 116.3378, emptyList()),
                StoreInfo("爆汁小龙虾(学院路店)", "海淀区学院路 42 号", "4.3", 39.9922, 116.3212, emptyList())
            )
        ),
        Brand(
            brand = "一点点",
            deals = listOf(
                deal("抖音团购", "🎵", "douyin", "大杯任选", 12.0, "2万+"),
                deal("美团", "🟢", "meituan", "波霸奶茶套餐", 14.9, "1.5万+", "美团专享"),
                deal("美团外卖", "🛵", "meituan_wm", "满杯送", 13.5, "1.2万+", "顺丰冷链", "外卖"),
                deal("淘宝闪购", "🟧", "taobao", "满杯水果茶", 16.0, "1.2万+", "外卖", "外卖"),
                deal("京东秒送", "🟧", "jdmiaosong", "冰沙系列", 15.0, "3000+", "夏日限定", "外卖")
            ),
            stores = listOf(
                StoreInfo("一点点(中关村店)", "海淀区中关村北大街 8 号", "4.7", 39.9900, 116.3125, emptyList()),
                StoreInfo("一点点(五道口店)", "海淀区成府路 27 号", "4.6", 39.9928, 116.3360, emptyList()),
                StoreInfo("一点点(苏州街店)", "海淀区苏州街 22 号", "4.6", 39.9808, 116.3105, emptyList())
            )
        )
    )

    /** 关键词 → 品牌(按关键词长度降序匹配,优先匹配更长更精确的) */
    fun findBrand(storeName: String): Brand? {
        if (storeName.isBlank()) return null
        val lower = storeName.lowercase()
        val candidates = brands.flatMap { brand ->
            val keywords = listOf(brand.brand) + extraKeywords(brand.brand)
            keywords.mapNotNull { kw ->
                val kwl = kw.lowercase()
                if (kwl.length >= 2 && kwl in lower) Pair(brand, kwl.length)
                else null
            }
        }
        return candidates.maxByOrNull { it.second }?.first
    }

    private fun extraKeywords(brand: String): List<String> = when (brand) {
        "瑞幸" -> listOf("luckin", "咖啡", "瑞幸咖啡")
        "星巴克" -> listOf("starbucks", "星巴克")
        "麦当劳" -> listOf("mcdonald", "金拱门", "麦当劳")
        "海底捞" -> listOf("haidilao", "海底捞")
        "肯德基" -> listOf("kfc", "肯德基", "肯")
        "爆汁小龙虾" -> listOf("小龙虾", "龙虾", "小虾", "麻辣小龙虾", "香辣虾", "皮皮虾", "爆汁")
        "一点点" -> listOf("coco", "一点点", "奶茶")
        else -> emptyList()
    }

    /** 关键词匹配(无定位):返回第 1 个分店(无 GPS 时 fallback) */
    fun findInfo(storeName: String): StoreInfo? {
        val brand = findBrand(storeName) ?: return null
        val store = brand.stores.first()
        // 注入 deals 给 StoreInfo(原本 deals 是 emptyList 占位)
        return store.copy(deals = brand.deals)
    }

    /** 关键词 + 定位匹配:按 Haversine 距离选最近分店 */
    fun findMatch(storeName: String, userLat: Double?, userLng: Double?): MatchResult? {
        val brand = findBrand(storeName) ?: return null
        val s = brand.nearestStore(userLat, userLng)
        val dist = if (userLat == null || userLng == null) 0.0
                   else s.distanceTo(userLat, userLng)
        return MatchResult(s.copy(deals = brand.deals), dist)
    }

    fun findLowestDeal(storeName: String): Deal? =
        findBrand(storeName)?.deals?.minByOrNull { it.price }

    /**
     * 查找用户位置附近的所有 MockDeals 分店(所有品牌)
     * @param userLat 用户纬度
     * @param userLng 用户经度
     * @param radiusMeters 半径(默认 2km)
     * @return 附近分店列表,按距离升序
     */
    fun findNearby(userLat: Double?, userLng: Double?, radiusMeters: Double = 2000.0): List<MatchResult> {
        if (userLat == null || userLng == null) return emptyList()
        return brands.flatMap { brand ->
            brand.stores.mapNotNull { store ->
                val dist = store.distanceTo(userLat, userLng)
                if (dist <= radiusMeters) {
                    MatchResult(store.copy(deals = brand.deals), dist)
                } else null
            }
        }.sortedBy { it.distanceMeters }
    }
}