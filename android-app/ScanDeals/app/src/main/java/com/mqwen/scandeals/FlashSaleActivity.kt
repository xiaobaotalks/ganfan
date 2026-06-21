package com.mqwen.scandeals

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 秒杀团购页(秒省)
 * 风格参考: 顶部暖橙渐变 + 圆形 product 图 + 大字 ¥ + 立即抢购按钮
 */
class FlashSaleActivity : ComponentActivity() {

    data class FlashItem(
        val emoji: String,
        val name: String,
        val subtitle: String,
        val price: Double,
        val tags: List<String>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FlashSaleScreen() }
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}

@Composable
fun FlashSaleScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F2))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部暖橙渐变栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFB199), Color(0xFFFF9966), Color(0xFFFFF8F2))
                        )
                    )
                    .padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        "秒杀团购",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
            // 标题大字
            Text(
                "⚡ 限时秒杀 · 每天 12:00 / 18:00 准时开抢",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            // 列表
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(flashItems) { item -> FlashCard(item) { url -> openUrlStatic(ctx, url) } }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

private fun openUrlStatic(ctx: android.content.Context, url: String) {
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
private fun FlashCard(item: FlashSaleActivity.FlashItem, onBuy: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圆形 product 占位
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF1E8)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.emoji, fontSize = 40.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333),
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(item.subtitle, fontSize = 11.sp, color = Color(0xFF999999))
                Spacer(Modifier.height(6.dp))
                Row {
                    item.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFE8D9))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(tag, fontSize = 10.sp, color = Color(0xFFFF6B3D))
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "¥${item.price.toInt()}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B3D)
                    )
                    Text(
                        ".${((item.price - item.price.toInt()) * 10).toInt()}",
                        fontSize = 12.sp,
                        color = Color(0xFFFF6B3D)
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF9966), Color(0xFFFF6B3D))
                                )
                            )
                            .clickable {
                                onBuy("https://m.douyin.com/search/${Uri.encode(item.name)}")
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text("立即抢购", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}


// 顶层数据(供 Composable 直接引用)
private val flashItems = listOf(
    FlashSaleActivity.FlashItem("🍰", "景荷山玛(黄醇黄苷)二色 将呆菠", "秒杀结束", 95.8, listOf("即将结束", "仅剩 X 份")),
    FlashSaleActivity.FlashItem("🍞", "宝葛平包多芬(秒杀跪) 银将呆菠", "秒杀结束", 95.8, listOf("即将结束", "仅剩 X 份")),
    FlashSaleActivity.FlashItem("🥮", "景荷山玛(黄醇黄苷)二色 将呆菠", "平茶黄仑 秒结品", 45.8, listOf("即将结束", "仅剩 X 份")),
    FlashSaleActivity.FlashItem("🍑", "壹葛平包多芬(乙色荔包) 银将呆菠", "甲条鲜 C位计任通购", 95.8, listOf("即将结束", "仅剩 X 份"))
)
