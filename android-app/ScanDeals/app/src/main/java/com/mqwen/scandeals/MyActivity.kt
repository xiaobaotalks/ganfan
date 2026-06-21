package com.mqwen.scandeals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mqwen.scandeals.ui.theme.ScanDealsTheme

/**
 * 我的页(对齐设计稿 1.png)
 * 头像 + 用户名 + 联网/离线模式开关 + 4 个选项(全部记录/隐私/关于/帮助)
 *
 * "联网模式"开关:开启时,允许网络请求(默认关 = 端侧 VLM + 离线 mock + 隐私优先)
 */
class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ScanDealsTheme { MeScreen() } }
    }
}

@Composable
fun MeScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // 联网模式(默认关,完全本地化)
    var onlineMode by remember { mutableStateOf(false) }
    // 4 个 Dialog
    var showHistory by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFE0CC), Color(0xFFFFF8F2))
                )
            )
    ) {
        // 状态栏留位
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Color(0xFFFFE0CC))
        )
        // 顶栏:米色 + 居中标题(无返回箭头,MainShell 模式无 finish)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFE0CC))
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "我的",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }

        Spacer(Modifier.height(8.dp))

        // 头像 + 用户名 + 联网/离线模式开关
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFCBA9)),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 40.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "干饭达人",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (onlineMode) "已联网 · 云端增强" else "完全离线 · 端侧 AI",
                    fontSize = 11.sp,
                    color = if (onlineMode) Color(0xFF4CAF50) else Color(0xFF999999)
                )
            }
            Switch(
                checked = onlineMode,
                onCheckedChange = { onlineMode = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFFF9966),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFCCCCCC)
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        // 联网模式说明卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (onlineMode) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (onlineMode) "🌐" else "🔒", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (onlineMode)
                        "已开启联网模式 · 6 平台价格实时刷新"
                    else
                        "端侧模式 · 价格本地缓存 · 招牌照片绝不上传",
                    fontSize = 12.sp,
                    color = if (onlineMode) Color(0xFF2E7D32) else Color(0xFFE65100)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // 4 个选项
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(vertical = 8.dp)
        ) {
            MeRow("⏱", "全部识别记录") { showHistory = true }
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            MeRow("🔒", "隐私设置") { showPrivacy = true }
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            MeRow("💬", "关于我们") { showAbout = true }
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            MeRow("♥", "帮助中心") { showHelp = true }
        }

        Spacer(Modifier.weight(1f))

        // 底部
        Text(
            "干饭省省 v1.0 · 端侧 AI 创新挑战赛参赛作品",
            fontSize = 11.sp,
            color = Color(0xFF999999),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            textAlign = TextAlign.Center
        )
    }

    // 4 个 Dialog
    if (showHistory) HistoryDialog(onDismiss = { showHistory = false })
    if (showPrivacy) PrivacyDialog(onlineMode, { onlineMode = it }, onDismiss = { showPrivacy = false })
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    if (showHelp) HelpDialog(onDismiss = { showHelp = false })
}

@Composable
fun MeRow(icon: String, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFE0CC)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp)
        }
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        Text("›", fontSize = 22.sp, color = Color(0xFFCCCCCC))
    }
}

// ====== 4 个 Dialog ======

@Composable
fun HistoryDialog(onDismiss: () -> Unit) {
    val history = remember { HistoryRepo.recent() }
    CommonDialog(title = "⏱ 全部识别记录", onDismiss = onDismiss) {
        if (history.isEmpty()) {
            Text("还没有识别记录,去拍个招牌试试吧", fontSize = 14.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(vertical = 20.dp))
        } else {
            Column {
                history.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFE0CC)),
                            contentAlignment = Alignment.Center
                        ) { Text("🏬", fontSize = 20.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US)
                                    .format(java.util.Date(entry.time)),
                                fontSize = 11.sp,
                                color = Color(0xFF999999)
                            )
                        }
                        Text("¥${entry.lowestPrice.toInt()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F))
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyDialog(onlineMode: Boolean, onOnlineChange: (Boolean) -> Unit, onDismiss: () -> Unit) {
    CommonDialog(title = "🔒 隐私设置", onDismiss = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            // 联网模式开关
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (onlineMode) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "联网模式",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (onlineMode)
                                "开启:云端价格实时刷新(数据可能上报)"
                            else
                                "关闭:完全本地化,招牌照片绝不上传",
                            fontSize = 11.sp,
                            color = Color(0xFF666666)
                        )
                    }
                    Switch(
                        checked = onlineMode,
                        onCheckedChange = onOnlineChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFF9966),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCCCCCC)
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // 隐私承诺
            PrivacyItem("📷 招牌照片本地处理", "MNN 端侧 VLM 推理,照片不离开手机")
            PrivacyItem("🗑 历史图片自动清理", "最近 3 张,超出容量自动删最旧")
            PrivacyItem("📍 位置仅用于距离", "GPS 算距离用,不发送到任何服务器")
            PrivacyItem("🚫 无跟踪 SDK", "无 Crashlytics / Firebase / 友盟等跟踪")
            PrivacyItem("🔐 无敏感权限", "不申请 READ_PHONE_STATE / 通讯录等")
        }
    }
}

@Composable
fun PrivacyItem(title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Text("✓", fontSize = 16.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, fontSize = 11.sp, color = Color(0xFF666666))
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    CommonDialog(title = "💬 关于我们", onDismiss = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()) {
            Text("🍱", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text("干饭省省", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF7A40))
            Text("v1.0.0", fontSize = 12.sp, color = Color(0xFF999999))
            Spacer(Modifier.height(16.dp))
            Text(
                "拍餐厅招牌 · 端侧 AI 识别 · 6 平台比价",
                fontSize = 13.sp,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🚀 技术亮点", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE65100))
                    Spacer(Modifier.height(6.dp))
                    Text("• Qwen3-VL-2B INT4 端侧推理", fontSize = 11.sp, color = Color(0xFF666666))
                    Text("• MNN 推理框架(7MB 引擎)", fontSize = 11.sp, color = Color(0xFF666666))
                    Text("• 13.86 tok/s 端侧生成速度", fontSize = 11.sp, color = Color(0xFF666666))
                    Text("• 完全离线(飞行模式可用)", fontSize = 11.sp, color = Color(0xFF666666))
                    Text("• 跨设备兼容(SME2 + i8mm)", fontSize = 11.sp, color = Color(0xFF666666))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "🏆 端侧 AI 创新挑战赛参赛作品\nMade by 1 人独挑 · 9 天极限开发",
                fontSize = 11.sp,
                color = Color(0xFF999999),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    CommonDialog(title = "♥ 帮助中心", onDismiss = onDismiss) {
        Column {
            FaqItem("Q: 招牌识别准确吗?", "A: Qwen3-VL-2B 端侧识别主流餐饮品牌(瑞幸/星巴克/麦当劳/海底捞/肯德基/一点点 等)准确率 >95%,街边小铺可能识别失败,会显示「未识别到店名」提示。")
            FaqItem("Q: 数据上传吗?", "A: 默认完全离线,招牌照片不离开手机。联网模式关闭时,价格数据从本地 mock 加载(7 店 × 4 平台)。")
            FaqItem("Q: 为什么我点大快门没反应?", "A: 需要先在系统设置中授予相机权限。点快门时会自动弹权限请求。")
            FaqItem("Q: 最低价卡片怎么点?", "A: 6 套餐 grid 任意卡片都可点,跳到对应 App 搜索结果。底部 🚀 按钮直达最低价。")
            FaqItem("Q: GPS 定位有什么用?", "A: 算用户到店距离(195m / 1.2km),优先匹配附件门店。开启飞行模式则跳过定位,只按店名匹配。")
            FaqItem("Q: 历史记录存哪里?", "A: 私有目录 filesDir/photos/,只保留最近 3 张图片,超出自动删最旧。")
        }
    }
}

@Composable
fun FaqItem(q: String, a: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(q, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF7A40))
        Spacer(Modifier.height(2.dp))
        Text(a, fontSize = 12.sp, color = Color(0xFF666666), lineHeight = 18.sp)
    }
}

@Composable
fun CommonDialog(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F2))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Text("×", fontSize = 24.sp, color = Color(0xFF999999),
                        modifier = Modifier.clickable { onDismiss() })
                }
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}
