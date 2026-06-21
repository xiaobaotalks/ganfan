package com.mqwen.scandeals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * 社群圈子页(省圈)
 * 风格参考: 顶部暖橙渐变 + 群头像(白底圆图) + 群名/简介/成员数
 */
class CircleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CircleScreen() }
    }
}

// 顶层 data class
data class GroupInfo(
    val emoji: String,
    val name: String,
    val desc: String,
    val members: Int
)

// 顶层数据(供 Composable 直接引用)
private val groupList = listOf(
    GroupInfo("🍱", "干饭省钱群 · 五道口", "分享五道口周边折扣 + 限时秒杀", 123),
    GroupInfo("☕", "咖啡奶茶优惠群", "瑞幸/一点点/星巴克 9 折拼单", 456),
    GroupInfo("🍔", "麦当劳肯德基薅羊毛", "麦乐送 / 宅急送 每日优惠汇总", 289),
    GroupInfo("🥘", "海底捞聚友会", "4 人套餐拼单 + 生日福利", 167),
    GroupInfo("🍜", "深夜食堂外卖团", "美团/淘宝闪购/京东秒送 满减凑单", 78)
)

@Composable
fun CircleScreen() {
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
                        "社群圈子",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
            Text(
                "👥 和附近人一起干饭 · 拼单拼团更便宜",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(groupList) { g -> GroupCard(g) }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun GroupCard(g: GroupInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).clickable { },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圆形头像(白底 + emoji)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF1E8)),
                contentAlignment = Alignment.Center
            ) {
                Text(g.emoji, fontSize = 30.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    g.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333)
                )
                Spacer(Modifier.height(2.dp))
                Text(g.desc, fontSize = 12.sp, color = Color(0xFF999999))
                Spacer(Modifier.height(4.dp))
                Text("成员:${g.members} 人", fontSize = 11.sp, color = Color(0xFFFF9966))
            }
            Text("›", fontSize = 22.sp, color = Color(0xFFCCCCCC))
        }
    }
}
