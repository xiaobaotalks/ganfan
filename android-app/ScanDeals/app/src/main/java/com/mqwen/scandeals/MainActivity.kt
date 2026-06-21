package com.mqwen.scandeals

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.mqwen.scandeals.ui.theme.ScanDealsTheme

/**
 * 首页(对齐设计稿 4.png)
 * 暖橙主题 + 大圆 CTA + 最近识别 + 底部 Tab
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // debug 入口:从 adb 传 image_path 直接进 SignScanActivity 跑 inference
        //   adb shell am start -n com.mqwen.scandeals/.MainActivity --es debug_image /sdcard/Pictures/test.jpg
        intent?.getStringExtra("debug_image")?.let { path ->
            if (java.io.File(path).exists()) {
                val intent = android.content.Intent(this, SignScanActivity::class.java).apply {
                    putExtra("debug_image", path)
                    if (intent.hasExtra("debug_lat")) putExtra("lat", intent.getDoubleExtra("debug_lat", 0.0))
                    if (intent.hasExtra("debug_lng")) putExtra("lng", intent.getDoubleExtra("debug_lng", 0.0))
                }
                startActivity(intent)
                finish()
                return
            }
        }

        setContent {
            ScanDealsTheme {
                MainShell()
            }
        }
    }
}

@Composable
fun HomePage(onJumpToFlash: () -> Unit = {}) {
    val context = LocalContext.current
    // 用 mutableStateOf + 每次进入页面重新读(模拟 SharedPreferences 持久化的最新数据)
    var history by remember { mutableStateOf(HistoryRepo.recent()) }

    // onResume 时刷新历史(从 ResultActivity 返回时会刷新)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                history = HistoryRepo.recent()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8F2))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 状态栏留位(米色背景)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(Color(0xFFFFF8F2))
            )
            // 顶部 bar
            TopBarHome()

            Spacer(Modifier.height(8.dp))

            // 大圆 CTA(缩小到 220)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                CaptureCircle(
                    onClick = {
                        context.startActivity(Intent(context, SignScanActivity::class.java))
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            // 最近识别(最多 5 家)
            if (history.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.home_recent), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "(${history.size}/5)",
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                    Text(
                        "🗑 清空",
                        fontSize = 13.sp,
                        color = Color(0xFFFF9966),
                        modifier = Modifier.clickable {
                            HistoryRepo.clear()
                            history = HistoryRepo.recent()
                        }
                    )
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(history) { item ->
                        RecentCard(item, onClick = {
                            context.startActivity(Intent(context, ResultActivity::class.java).apply {
                                putExtra("店名", item.name)
                                putExtra("lat", item.lowestPrice)
                            })
                        })
                    }
                }
            }

            // 秒省精选商户(来自秒省数据,首页露出)
            HomeFlashSaleSection(
                onMore = onJumpToFlash
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun TopBarHome() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 橙色"干"方块
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFF9966)),
            contentAlignment = Alignment.Center
        ) {
            Text("干", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.app_name), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        // ⊕ 图标
        Box(
            modifier = Modifier
                .size(32.dp)
                .clickable { /* TODO: 添加 */ },
            contentAlignment = Alignment.Center
        ) {
            Text("⊕", fontSize = 24.sp, color = Color(0xFF333333))
        }
    }
}

@Composable
fun CaptureCircle(onClick: () -> Unit) {
    // 外圈光晕 + 内白圆
    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFFE0CC), Color(0xFFFFCBA9), Color(0xFFFFB892)),
                    radius = 260f
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF6EC)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 相机 icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFF9966)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷", fontSize = 32.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.home_cta), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.home_cta_subtitle), fontSize = 11.sp, color = Color(0xFF999999))
            }
        }
    }
}

@Composable
fun RecentCard(item: HistoryItem, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFE0CC))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📸", fontSize = 28.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            item.name,
            fontSize = 11.sp,
            color = Color(0xFF1F1F1F),
            maxLines = 1
        )
        if (item.lowestPrice > 0) {
            Text(
                "¥${item.lowestPrice.toInt()}",
                fontSize = 10.sp,
                color = Color(0xFFFF6B35),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 单一 Activity 内部 state 切换 4 page(首页/秒省/省圈/我的)
 * 切 Tab 不会再 startActivity,丝滑且不重建页面
 */
@Composable
fun MainShell() {
    var currentTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F2))
    ) {
        when (currentTab) {
            0 -> HomePage(onJumpToFlash = { currentTab = 1 })
            1 -> FlashPage()
            2 -> CirclePage()
            3 -> MePage()
        }
        SharedBottomBar(
            selected = currentTab,
            onSelect = { idx -> currentTab = idx },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun SharedBottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    BottomTabBar(selected = selected, onSelect = onSelect, modifier = modifier)
}

@Composable
fun BottomTabBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 6.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem(label = "首页", icon = "🏠", selected = selected == 0, onClick = { onSelect(0) })
            TabItem(label = "秒省", icon = "⚡", selected = selected == 1, onClick = { onSelect(1) })
            TabItem(label = "省圈", icon = "👥", selected = selected == 2, onClick = { onSelect(2) })
            TabItem(label = "我的", icon = "👤", selected = selected == 3, onClick = { onSelect(3) })
        }
    }
}

@Composable
fun RowScope.TabItem(label: String, icon: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) Color(0xFFFFE0CC) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 22.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = if (selected) Color(0xFFFF6B3D) else Color(0xFF999999),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// === 数据 ===
data class HistoryItem(
    val name: String,
    val time: Long = System.currentTimeMillis(),
    val lowestPrice: Double = 0.0
)

@Composable
fun FeatureButton(
    icon: String,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(gradient))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 22.sp)
                Spacer(Modifier.width(6.dp))
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

object HistoryRepo {
    private const val MAX = 5
    private val cache = mutableListOf<HistoryItem>().apply {
        add(HistoryItem("爆汁小虾", System.currentTimeMillis() - 1000L * 60 * 5, 39.0))
        add(HistoryItem("瑞幸咖啡", System.currentTimeMillis() - 1000L * 60 * 60 * 2, 8.8))
        add(HistoryItem("麦当劳", System.currentTimeMillis() - 1000L * 60 * 60 * 24, 11.9))
    }
    fun recent(): List<HistoryItem> = cache.toList()
    fun add(item: HistoryItem) {
        cache.add(0, item)
        // 只保留最近 5 家
        while (cache.size > MAX) cache.removeAt(cache.size - 1)
    }
    fun clear() = cache.clear()
}


// ========== 秒省精选商户(首页) ==========
data class HomeFlashItem(
    val emoji: String,
    val name: String,
    val originalPrice: Double,
    val flashPrice: Double
)

private val homeFlashItems = listOf(
    HomeFlashItem("🍔", "麦当劳巨无霸套餐", 35.0, 11.0),
    HomeFlashItem("☕", "瑞幸咖啡生椰拿铁", 32.0, 8.8),
    HomeFlashItem("🍕", "必胜客披萨9寸", 79.0, 29.9),
    HomeFlashItem("🧋", "喜茶多肉葡萄", 28.0, 9.9),
    HomeFlashItem("🍜", "海底捞自选套餐", 99.0, 49.0)
)

@Composable
fun HomeFlashSaleSection(onMore: () -> Unit) {
    val context = LocalContext.current
    // 卡片点击和"立即抢购"都用 onMore(切换到秒省 Tab)
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.home_flash_sale), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onMore() }
            ) {
                Text(
                    stringResource(R.string.home_view_more),
                    fontSize = 12.sp,
                    color = Color(0xFFFF9966)
                )
                Text(
                    " ›",
                    fontSize = 14.sp,
                    color = Color(0xFFFF9966)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(homeFlashItems) { item ->
                HomeFlashCard(item, onClick = onMore)
            }
        }
    }
}

@Composable
fun HomeFlashCard(item: HomeFlashItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 圆形 emoji 大头
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF1E8)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.emoji, fontSize = 30.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333),
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            // 划线原价 + 大字秒杀价
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "¥${item.originalPrice.toInt()}",
                    fontSize = 10.sp,
                    color = Color(0xFF999999),
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "¥",
                    fontSize = 12.sp,
                    color = Color(0xFFFF6B3D),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${item.flashPrice.toInt()}",
                    fontSize = 22.sp,
                    color = Color(0xFFFF6B3D),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    ".${((item.flashPrice - item.flashPrice.toInt()) * 10).toInt()}",
                    fontSize = 11.sp,
                    color = Color(0xFFFF6B3D)
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF9966), Color(0xFFFF6B3D))
                        )
                    )
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "立即抢购",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


// ========== 4 个 page 的桥接(被 MainShell 调用) ==========
// HomePage 已在本文件定义
// FlashPage 在 FlashSaleActivity.kt 中定义
// CirclePage 在 CircleActivity.kt 中定义
// MePage 桥接到 MeScreen()(在 MyActivity.kt 中定义)
@Composable
fun FlashPage() = FlashSaleScreen()
@Composable
fun CirclePage() = CircleScreen()
@Composable
fun MePage() = MeScreen()
