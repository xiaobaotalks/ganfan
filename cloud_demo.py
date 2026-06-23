"""
干饭省省 · 云端 Demo 测试
模拟招牌识别 + 5 平台比价核心逻辑
"""
import math
import json
from dataclasses import dataclass
from typing import List, Optional


@dataclass
class Deal:
    platform: str
    platform_icon: str
    platform_key: str
    name: str
    price: float
    sales: str
    note: str = ""
    group: str = "团购"


@dataclass
class StoreInfo:
    name: str
    address: str
    rating: str
    lat: float
    lng: float
    deals: List[Deal]

    def distance_to(self, lat: float, lng: float) -> float:
        r = 6371000.0
        d_lat = math.radians(lat - self.lat)
        d_lng = math.radians(lng - self.lng)
        a = math.sin(d_lat / 2) ** 2 + \
            math.cos(math.radians(self.lat)) * math.cos(math.radians(lat)) * \
            math.sin(d_lng / 2) ** 2
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        return r * c


@dataclass
class Brand:
    brand: str
    deals: List[Deal]
    stores: List[StoreInfo]
    keywords: List[str]

    def nearest_store(self, user_lat: Optional[float], user_lng: Optional[float]) -> StoreInfo:
        if not self.stores:
            raise ValueError(f"品牌 {self.brand} 无分店")
        if user_lat is None or user_lng is None:
            return self.stores[0]
        return min(self.stores, key=lambda s: s.distance_to(user_lat, user_lng))


def make_deal(p, icon, key, name, price, sales, note="", group="团购"):
    return Deal(p, icon, key, name, price, sales, note, group)


BRANDS = [
    Brand(
        brand="瑞幸",
        keywords=["瑞幸", "luckin", "咖啡", "瑞幸咖啡"],
        deals=[
            make_deal("抖音团购", "🎵", "douyin", "9.9 元任选券", 9.9, "10万+", "限时秒杀"),
            make_deal("美团", "🟢", "meituan", "生椰拿铁套餐", 15.9, "5万+", "美团专享"),
            make_deal("美团外卖", "🛵", "meituan_wm", "外卖专享", 12.8, "1万+", "免配送", "外卖"),
            make_deal("淘宝闪购", "🟧", "taobao", "咖啡+轻食券", 13.8, "2万+", "外卖专享", "外卖"),
            make_deal("京东秒送", "🟧", "jdmiaosong", "大师咖啡券", 18.0, "8000+", "极速配送", "外卖"),
        ],
        stores=[
            StoreInfo("瑞幸咖啡(中关村店)", "海淀区中关村大街 1 号", "4.6", 39.9840, 116.3120, []),
            StoreInfo("瑞幸(学院路店)", "海淀区学院路 35 号", "4.5", 39.9912, 116.3195, []),
            StoreInfo("瑞幸(五道口店)", "海淀区成府路 28 号", "4.7", 39.9925, 116.3370, []),
            StoreInfo("瑞幸(上地店)", "海淀区上地信息路 9 号", "4.4", 40.0310, 116.3085, []),
        ]
    ),
    Brand(
        brand="星巴克",
        keywords=["星巴克", "starbucks"],
        deals=[
            make_deal("抖音团购", "🎵", "douyin", "35 元代金券", 30.0, "8万+", "周三特惠"),
            make_deal("美团", "🟢", "meituan", "星冰乐 6 选 1", 28.0, "4万+", "美团会员价"),
            make_deal("美团外卖", "🛵", "meituan_wm", "专星送", 32.0, "8000+", "会员免运", "外卖"),
            make_deal("淘宝闪购", "🟧", "taobao", "外送专享杯", 35.0, "1.5万+", "免配送费", "外卖"),
            make_deal("京东秒送", "🟧", "jdmiaosong", "37 元双人套餐", 37.0, "1.2万+", "", "外卖"),
        ],
        stores=[
            StoreInfo("星巴克(中关村店)", "海淀区中关村大街 18 号", "4.8", 39.9855, 116.3108, []),
            StoreInfo("星巴克(五道口店)", "海淀区成府路 30 号", "4.7", 39.9930, 116.3360, []),
            StoreInfo("星巴克(学院路店)", "海淀区学院路 40 号", "4.6", 39.9920, 116.3205, []),
        ]
    ),
    Brand(
        brand="麦当劳",
        keywords=["麦当劳", "mcdonald", "金拱门"],
        deals=[
            make_deal("抖音团购", "🎵", "douyin", "巨无霸套餐", 25.9, "12万+", "抖音独家"),
            make_deal("美团", "🟢", "meituan", "麦辣鸡腿堡餐", 22.9, "8万+", "美团专享"),
            make_deal("美团外卖", "🛵", "meituan_wm", "麦乐送", 19.9, "3万+", "29 分钟达", "外卖"),
            make_deal("淘宝闪购", "🟧", "taobao", "麦满分早餐", 11.9, "3万+", "早餐时段", "外卖"),
            make_deal("京东秒送", "🟧", "jdmiaosong", "家庭欢享餐", 49.9, "1.5万+", "3-4 人餐", "外卖"),
        ],
        stores=[
            StoreInfo("麦当劳(中关村南店)", "海淀区中关村南大街 5 号", "4.5", 39.9788, 116.3150, []),
            StoreInfo("麦当劳(五道口店)", "海淀区成府路 28 号 1F", "4.4", 39.9928, 116.3362, []),
            StoreInfo("麦当劳(苏州街店)", "海淀区苏州街 12 号", "4.5", 39.9805, 116.3095, []),
        ]
    ),
    Brand(
        brand="海底捞",
        keywords=["海底捞", "haidilao"],
        deals=[
            make_deal("抖音团购", "🎵", "douyin", "4 人套餐", 299.0, "3万+", "周末可用"),
            make_deal("美团", "🟢", "meituan", "双人浪漫餐", 188.0, "2万+", "美团用户"),
            make_deal("美团外卖", "🛵", "meituan_wm", "捞派送", 168.0, "5000+", "锅具同送", "外卖"),
            make_deal("淘宝闪购", "🟧", "taobao", "2-3 人餐", 168.0, "1.2万+", "外送", "外卖"),
            make_deal("京东秒送", "🟧", "jdmiaosong", "企业 6 人餐", 599.0, "3000+", "发票专享", "外卖"),
        ],
        stores=[
            StoreInfo("海底捞火锅(中关村店)", "海淀区科学院南路 2 号", "4.9", 39.9820, 116.3250, []),
            StoreInfo("海底捞(学院路店)", "海淀区学院路 38 号", "4.8", 39.9920, 116.3210, []),
            StoreInfo("海底捞(五道口店)", "海淀区成府路 35 号 B1", "4.7", 39.9935, 116.3375, []),
        ]
    ),
    Brand(
        brand="肯德基",
        keywords=["肯德基", "kfc", "肯"],
        deals=[
            make_deal("抖音团购", "🎵", "douyin", "全家桶", 88.0, "15万+", "限时特惠"),
            make_deal("美团", "🟢", "meituan", "黄金鸡块 5 块", 12.9, "10万+", "美团会员"),
            make_deal("美团外卖", "🛵", "meituan_wm", "宅急送", 25.9, "2万+", "满 49 免运", "外卖"),
            make_deal("淘宝闪购", "🟧", "taobao", "吮指原味鸡", 13.5, "4万+", "外卖专享", "外卖"),
            make_deal("京东秒送", "🟧", "jdmiaosong", "宅急送双人餐", 49.9, "2万+", "", "外卖"),
        ],
        stores=[
            StoreInfo("肯德基(中关村店)", "海淀区中关村大街 27 号", "4.4", 39.9860, 116.3095, []),
            StoreInfo("肯德基(苏州街店)", "海淀区苏州街 18 号", "4.3", 39.9810, 116.3100, []),
            StoreInfo("肯德基(五道口店)", "海淀区成府路 32 号", "4.4", 39.9932, 116.3368, []),
        ]
    ),
]


def find_brand(store_name: str) -> Optional[Brand]:
    if not store_name:
        return None
    lower = store_name.lower()
    for brand in BRANDS:
        for kw in brand.keywords:
            kw_lower = kw.lower()
            if kw_lower and lower and (kw_lower in lower or lower in kw_lower):
                return brand
    return None


def format_distance(meters: float) -> str:
    if meters < 1000:
        return f"{int(meters)}m"
    return f"{meters / 1000:.1f}km"


def run_demo(store_name: str, user_lat: float = None, user_lng: float = None):
    """运行一次完整的比价 demo"""
    print("=" * 60)
    print("🍱 干饭省省 · 端侧 AI 干饭比价 App")
    print("=" * 60)
    print()

    print(f"📸 招牌识别结果: {store_name}")
    if user_lat and user_lng:
        print(f"📍 用户位置: {user_lat:.4f}, {user_lng:.4f}")
    print()

    brand = find_brand(store_name)
    if not brand:
        print("❌ 未识别到匹配的商家品牌")
        return

    store = brand.nearest_store(user_lat, user_lng)
    distance = store.distance_to(user_lat, user_lng) if user_lat and user_lng else 0.0

    print(f"✅ 匹配品牌: {brand.brand}")
    print(f"🏪 最近分店: {store.name}")
    print(f"📍 地址: {store.address}")
    print(f"⭐ 评分: {store.rating}")
    if distance > 0:
        print(f"📏 距离: {format_distance(distance)}")
    print()

    deals_by_group = {}
    for deal in brand.deals:
        deals_by_group.setdefault(deal.group, []).append(deal)

    print("💰 同套餐价格对比")
    print("-" * 60)

    for group, deals in deals_by_group.items():
        group_icon = "🎟" if group == "团购" else "🛵"
        group_sub = "到店核销" if group == "团购" else "极速配送"
        print(f"\n  {group_icon} {group} · {group_sub}")
        print("  " + "-" * 50)
        for deal in deals:
            print(f"  {deal.platform_icon} {deal.platform:10s}  ¥{deal.price:6.1f}  "
                  f"{deal.name:15s}  {deal.note}")

    all_deals = brand.deals
    lowest = min(all_deals, key=lambda d: d.price)
    highest = max(all_deals, key=lambda d: d.price)
    save = highest.price - lowest.price

    print()
    print("=" * 60)
    print(f"🏆 最低价: {lowest.platform} · ¥{lowest.price} ({lowest.name})")
    print(f"💸 比最高价省: ¥{save:.1f} (省 {save/highest.price*100:.0f}%)")
    print("=" * 60)
    print()

    result = {
        "brand": brand.brand,
        "store": store.name,
        "address": store.address,
        "rating": store.rating,
        "distance_meters": distance,
        "lowest_price": lowest.price,
        "lowest_platform": lowest.platform,
        "highest_price": highest.price,
        "save_amount": save,
        "save_percent": save / highest.price * 100,
        "deals": [
            {
                "platform": d.platform,
                "price": d.price,
                "name": d.name,
                "group": d.group
            } for d in all_deals
        ]
    }
    return result


def main():
    print("\n" + "=" * 60)
    print("🚀 干饭省省 · 云端 Demo 测试启动")
    print("=" * 60)
    print()

    test_cases = [
        ("瑞幸咖啡", 39.9925, 116.3370),
        ("麦当劳", 39.9840, 116.3120),
        ("海底捞", 39.9920, 116.3210),
        ("星巴克", None, None),
        ("KFC", 39.9860, 116.3095),
    ]

    all_results = []
    for store_name, lat, lng in test_cases:
        result = run_demo(store_name, lat, lng)
        all_results.append(result)

    print("\n" + "=" * 60)
    print("📊 批量测试汇总")
    print("=" * 60)
    print(f"{'商家':<12} {'最低价':<10} {'最省平台':<12} {'省':<10}")
    print("-" * 60)
    for r in all_results:
        if r:
            print(f"{r['brand']:<12} ¥{r['lowest_price']:<8.1f} "
                  f"{r['lowest_platform']:<12} ¥{r['save_amount']:<8.1f}")

    print()
    print("✅ Demo 测试完成！")
    print()
    print("📱 完整 App 功能:")
    print("   • 端侧 Qwen3-VL-2B 招牌识别（MNN 推理框架）")
    print("   • 完全离线 · 飞行模式可用 · 隐私不出手机")
    print("   • 5 平台比价 + GPS 最近分店匹配")
    print("   • 一键 deeplink 跳官方 App 买券")
    print("   • 秒省限时秒杀 + 省圈拼单拼团")
    print()


if __name__ == "__main__":
    main()
