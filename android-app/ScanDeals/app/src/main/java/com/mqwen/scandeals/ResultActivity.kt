package com.mqwen.scandeals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.mqwen.scandeals.ui.theme.ScanDealsTheme
import kotlinx.coroutines.launch

/**
 * 结果页
 * 顶部:店名 + 匹配度
 * 商家卡片(图 + 地址 + 评分 + 距离)
 * 6 套餐对比 grid(中间高亮最低价,可点)
 * 底部 sticky bar:重拍 + 去最低价
 *
 * 原始 JSON 调试区已隐藏(只开发者看,不要给用户看)
 */
class ResultActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 让内容延伸到状态栏后面(系统栏透明),但顶栏用 windowInsetsPadding 避开状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val storeName = intent.getStringExtra("店名") ?: "未知商家"
        val rawJson = intent.getStringExtra("raw_json") ?: ""
        val hasLat = intent.hasExtra("lat")
        val hasLng = intent.hasExtra("lng")
        val initLat = if (hasLat) intent.getDoubleExtra("lat", 0.0) else null
        val initLng = if (hasLng) intent.getDoubleExtra("lng", 0.0) else null

        // 把本次识别写入历史(首页"最近识别"会显示)
        val lowest = MockDeals.findInfo(storeName)?.deals?.minByOrNull { it.price }
        if (storeName != "未知商家") {
            HistoryRepo.add(
                HistoryItem(
                    name = storeName,
                    time = System.currentTimeMillis(),
                    lowestPrice = lowest?.price ?: 0.0
                )
            )
        }

        setContent {
            ScanDealsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFF8F2)) {
                    ResultScreen(
                        storeName = storeName,
                        rawJson = rawJson,
                        initLat = initLat,
                        initLng = initLng
                    )
                }
            }
        }
    }
}

@Composable
fun ResultScreen(storeName: String, rawJson: String, initLat: Double? = null, initLng: Double? = null) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val activity = (ctx as? android.app.Activity)
    val scope = rememberCoroutineScope()

    // 优先用 Intent 传过来的(识别时拿的),没有再异步拿
    var userLoc by remember { mutableStateOf<LocationProvider.LatLng?>(
        if (initLat != null && initLng != null) LocationProvider.LatLng(initLat, initLng) else null
    ) }
    LaunchedEffect(storeName) {
        if (userLoc == null && LocationProvider.hasPermission(ctx)) {
            userLoc = LocationProvider.getLastLocation(ctx, timeoutMs = 4000)
        }
    }

    val match = remember(userLoc) {
        MockDeals.findMatch(storeName, userLoc?.lat, userLoc?.lng)
    }
    val storeInfo = match?.info
    val matchPct = if (storeInfo != null) 98 else 0
    val lowest = storeInfo?.deals?.minByOrNull { it.price }

    // 半真实:联网模式开启时,尝试用高德 POI 拿真实店信息(MockDeals 没收录也能搜到)
    var realStore by remember { mutableStateOf<MockDeals.StoreInfo?>(null) }
    var loadingReal by remember { mutableStateOf(false) }
    LaunchedEffect(storeName, userLoc) {
        // 只有用户开了"联网模式"才走网络(默认离线 — 端侧 AI 卖点)
        UserPrefs.loadOnlineMode(ctx)
        if (ApiDeals.onlineMode) {
            loadingReal = true
            val r = ApiDeals.search(storeName, userLoc?.lat, userLoc?.lng)
            if (r != null && r.info.lat != 0.0) {
                realStore = r.info
            }
            loadingReal = false
        }
    }

    // 附近门店列表(所有品牌 2km 内分店,按距离)
    val nearby = remember(userLoc) {
        MockDeals.findNearby(userLoc?.lat, userLoc?.lng, radiusMeters = 2000.0)
    }
    // 优先用真实 POI,fallback MockDeals
    val displayInfo = realStore ?: storeInfo
    val displayMatch = realStore?.let {
        MockDeals.MatchResult(it,
            if (userLoc != null) it.distanceTo(userLoc!!.lat, userLoc!!.lng) else 0.0)
    } ?: match
    val displayLowest = displayInfo?.deals?.minByOrNull { it.price }

    fun openDeal(deal: MockDeals.Deal) {
        val target = activity ?: ctx
        val lat = userLoc?.lat
        val lng = userLoc?.lng
        // 3 个外卖平台 deeplink 各家 schema 不稳 → 直接用 web URL(浏览器打开,实时搜索结果)
        val preferWeb = deal.platformKey in listOf("meituan_wm", "taobao", "jdmiaosong")
        if (!preferWeb) {
            // 1) 先试 deeplink(抖音团购 / 美团团购,这俩 schema 稳定)
            try {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(deal.deeplink(storeName))
                )
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                target.startActivity(intent)
                return
            } catch (_: Throwable) { }
        }
        // 2) Web fallback(浏览器中打开 web 搜索结果)
        try {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(deal.webFallback(storeName, lat, lng))
            )
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            target.startActivity(intent)
        } catch (_: Throwable) { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 可滚动内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp) // 给 sticky bar 留位
        ) {
            // 顶部状态栏留位(米色背景)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(androidx.compose.foundation.layout.WindowInsets.statusBars)
                    .background(Color(0xFFFFF8F2))
            )
            TopBarResult()
            // 店名 + 匹配度
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(storeName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "$matchPct% 匹配",
                    fontSize = 16.sp,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            // 商家卡片
            if (displayInfo != null) {
                StoreCard(displayInfo, displayMatch?.distanceText ?: "")
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(storeName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("暂未收录到该商家团购信息,试试搜更多平台", fontSize = 12.sp, color = Color(0xFF999999))
                    }
                }
            }
            // 联网模式标识
            if (ApiDeals.onlineMode && realStore != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌐", fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "数据来自高德 POI · 套餐价格实时刷新",
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // 附近门店列表(2km 内所有 MockDeals 分店)— 顶部位置,用户一眼看到
            if (nearby.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📍", fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "附近 ${nearby.size} 家同款门店(2km 内)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF333333)
                    )
                }
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(nearby) { r ->
                        NearbyStoreCard(r)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📍", fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "附近未找到同款门店,显示该商家(2km 内)",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            // 同套餐价格对比
            Text(
                "🍱 同套餐价格对比",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(10.dp))
            if (displayInfo != null) {
                DealGrid(
                    deals = displayInfo.deals,
                    lowest = displayLowest,
                    onDealClick = { openDeal(it) }
                )
            }
            Spacer(Modifier.height(16.dp))
            // 价格小贴士
            if (displayLowest != null && displayInfo != null) {
                val maxPrice = displayInfo.deals.maxOf { it.price }
                val save = (maxPrice - displayLowest.price).toInt()
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1E8))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "最低价比最高价省 ¥$save",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF333333)
                            )
                            Text(
                                "点对应平台卡片可直达 App 搜索结果",
                                fontSize = 11.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
        }

        // 底部 sticky bar(优化:重拍 + 去最低价 两按钮)
        if (displayLowest != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                BottomActionBar(
                    lowestPlatform = displayLowest.platform,
                    lowestPrice = displayLowest.price,
                    onRescan = { activity?.finish() },
                    onGoLowest = { openDeal(displayLowest) }
                )
            }
        }
    }
}

@Composable
fun TopBarResult() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF8F2))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹", fontSize = 28.sp, color = Color(0xFF333333), modifier = Modifier.clickable {
            (ctx as? android.app.Activity)?.finish()
        })
        Spacer(Modifier.weight(1f))
        Text("干饭省省", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text("💬", fontSize = 22.sp)
    }
}

@Composable
fun StoreCard(info: MockDeals.StoreInfo, distanceText: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            // 店招占位
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFE0CC)),
                contentAlignment = Alignment.Center
            ) {
                Text("🏬", fontSize = 36.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(info.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "📍 ${info.address}",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    TagBadge("⭐ ${info.rating}", bg = Color(0xFFFFE8D9))
                    Spacer(Modifier.width(8.dp))
                    if (distanceText.isNotEmpty()) {
                        TagBadge("📏 $distanceText", bg = Color(0xFFFFE8D9))
                    }
                }
            }
            Text("›", fontSize = 24.sp, color = Color(0xFFCCCCCC))
        }
    }
}

@Composable
fun NearbyStoreCard(r: MockDeals.MatchResult) {
    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                r.info.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333),
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                r.info.address,
                fontSize = 9.sp,
                color = Color(0xFF999999),
                maxLines = 1
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TagBadge("📏 ${r.distanceText}", bg = Color(0xFFFFE8D9))
                Spacer(Modifier.width(4.dp))
                TagBadge("⭐ ${r.info.rating}", bg = Color(0xFFFFE8D9))
            }
        }
    }
}

@Composable
fun TagBadge(text: String, bg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, color = Color(0xFFFF7A40))
    }
}

@Composable
fun DealGrid(
    deals: List<MockDeals.Deal>,
    lowest: MockDeals.Deal?,
    onDealClick: (MockDeals.Deal) -> Unit
) {
    // 按分组(团购 / 外卖)分两行
    val grouped = deals.groupBy { it.group }
    val groupOrder = listOf("团购", "外卖")
    val groupIcons = mapOf("团购" to "🎟", "外卖" to "🛵")
    val groupSubs = mapOf("团购" to "到店核销", "外卖" to "极速配送")

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        groupOrder.forEach { g ->
            val rowDeals = grouped[g].orEmpty()
            if (rowDeals.isEmpty()) return@forEach
            // 分组小标题
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(groupIcons[g] ?: "📦", fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    g,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    groupSubs[g] ?: "",
                    fontSize = 10.sp,
                    color = Color(0xFF999999)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowDeals.forEach { deal ->
                    val isLowest = deal == lowest
                    Box(modifier = Modifier.weight(1f)) {
                        DealCard(deal, isHighlighted = isLowest, onClick = { onDealClick(deal) })
                    }
                }
            }
        }
    }
}

@Composable
fun DealCard(deal: MockDeals.Deal, isHighlighted: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isHighlighted) Color(0xFFFFE8D9) else Color.White
            ),
            border = if (isHighlighted)
                androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF9966))
            else null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(deal.platformIcon, fontSize = 20.sp)
                Spacer(Modifier.height(4.dp))
                Text(deal.platform, fontSize = 11.sp, color = Color(0xFF666666))
                Spacer(Modifier.height(8.dp))
                Text(
                    "¥${deal.price.toInt()}",
                    fontSize = 22.sp,
                    color = if (isHighlighted) Color(0xFFD32F2F) else Color(0xFF333333),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    deal.name,
                    fontSize = 10.sp,
                    color = Color(0xFF666666),
                    maxLines = 1
                )
                if (deal.note.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        deal.note,
                        fontSize = 9.sp,
                        color = Color(0xFF999999),
                        maxLines = 1
                    )
                }
            }
        }
        if (isHighlighted) {
            // 顶部"最低价"标签
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFF9966))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("🏷 最低价", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 底部 sticky bar(优化:重拍次按钮 + 去最低价主按钮)
 */
@Composable
fun BottomActionBar(
    lowestPlatform: String,
    lowestPrice: Double,
    onRescan: () -> Unit,
    onGoLowest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xFFFFF8F2).copy(alpha = 0.95f), Color(0xFFFFF8F2))
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 重拍次按钮
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                    .clickable { onRescan() },
                contentAlignment = Alignment.Center
            ) {
                Text("🔄", fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            // 主按钮:🚀 去 lowestPlatform ¥lowestPrice
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFFB347), Color(0xFFFF6B35))
                        )
                    )
                    .clickable { onGoLowest() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚀", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "去 $lowestPlatform · ¥${lowestPrice.toInt()}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
