package com.mqwen.scandeals

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.mqwen.scandeals.ui.theme.ScanDealsTheme
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 招牌拍照页
 * - 大圆快门 = 系统相机拍照(TakePicture + FileProvider,需 CAMERA 权限)
 * - 📷 按钮 = 从图库选图(GetContent)
 * - ⚡ = 闪光灯(目前占位,留给后续 CameraX)
 * - 启动时后台异步初始化 MNN
 * - 顶部"对准餐厅招牌"提示 + 中央取景框
 * - 识别过的图片存到 filesDir/photos/,只保留最近 3 张,超出自动删
 */
class SignScanActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SignScanActivity"
        // 模型放在 App 专属外部目录(Android 11+ 唯一不需权限的外部路径)
        const val MODEL_REL_PATH = "qwen3-vl-2b/Qwen3-VL-2B-Instruct-MNN/config.json"
        const val SIGN_PROMPT = """你是一个餐厅招牌识别助手。请仔细看这张图,以严格的 JSON 格式输出:
{"店名":"","菜系":"","招牌文字":["",""],"招牌颜色":"","置信度":0.0}
只输出 JSON,不要任何其他文字。"""
        private const val MAX_KEEP_PHOTOS = 3
    }

    private var llmReady by mutableStateOf(false)
    private var llmError by mutableStateOf<String?>(null)
    private var processing by mutableStateOf(false)
    private var statusText by mutableStateOf("初始化中…")

    // 当前拍照输出文件(系统相机写入的 uri 目标)
    private var pendingCameraFile: File? = null
    private var pendingCameraUri: Uri? = null

    /** 从图库选图 */
    private val pickFromGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleImagePicked(it) }
    }

    /** 系统相机拍照 */
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok && pendingCameraFile != null && pendingCameraFile!!.exists()) {
            runInference(pendingCameraFile!!.absolutePath)
        } else {
            statusText = "拍照未保存"
        }
        pendingCameraFile = null
        pendingCameraUri = null
    }

    /** Camera 权限请求 */
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchSystemCamera()
            } else {
                statusText = "需要相机权限"
            }
        }

    /** 位置权限请求 */
    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                statusText = "已开定位,正在拍照"
            } else {
                statusText = "未开定位(不影响识别)"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLlmAsync()
        // 请求位置权限(可选,只为 GPS 距离匹配)
        if (!LocationProvider.hasPermission(this)) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        setContent {
            ScanDealsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    CaptureScreen(
                        llmReady = llmReady,
                        statusText = statusText,
                        processing = processing,
                        onShutter = { onShutterClicked() },
                        onAlbum = { pickFromGalleryLauncher.launch("image/*") },
                        onBack = { finish() }
                    )
                }
            }
        }

        // debug 入口:从 adb 传图直接跑 inference
        intent?.getStringExtra("debug_image")?.let { debugImagePath ->
            if (java.io.File(debugImagePath).exists()) {
                runInference(debugImagePath)
            }
        }
    }

    /** 大圆快门 = 拍照 */
    private fun onShutterClicked() {
        if (!llmReady) {
            statusText = "MNN 还没准备好"
            return
        }
        if (processing) return
        // 请求 Camera 权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        } else {
            launchSystemCamera()
        }
    }

    /** 启动系统相机,FileProvider 输出到 filesDir/photos/camera_*.jpg */
    private fun launchSystemCamera() {
        try {
            val dir = File(filesDir, "photos").apply { mkdirs() }
            // 先清理旧文件(只留 3 张)
            cleanupOldPhotos(dir)
            val name = "camera_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(Date()) + ".jpg"
            val file = File(dir, name)
            val uri = FileProvider.getUriForFile(
                this, "$packageName.fileprovider", file
            )
            pendingCameraFile = file
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Throwable) {
            statusText = "启动相机失败: ${e.message}"
        }
    }

    /** 处理图库选图:复制到 filesDir/photos,清理旧图,跑 inference */
    private fun handleImagePicked(uri: Uri) {
        try {
            val dir = File(filesDir, "photos").apply { mkdirs() }
            cleanupOldPhotos(dir)
            val name = "pick_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(Date()) + ".jpg"
            val file = File(dir, name)
            contentResolver.openInputStream(uri)?.use { ins ->
                FileOutputStream(file).use { out -> ins.copyTo(out) }
            }
            runInference(file.absolutePath)
        } catch (e: Throwable) {
            statusText = "读图失败: ${e.message}"
        }
    }

    /** 只保留最近 MAX_KEEP_PHOTOS 张,删其余 */
    private fun cleanupOldPhotos(dir: File) {
        try {
            val all = dir.listFiles { f -> f.isFile && f.name.endsWith(".jpg") } ?: return
            if (all.size <= MAX_KEEP_PHOTOS) return
            // 按最后修改时间降序
            all.sortedByDescending { it.lastModified() }
                .drop(MAX_KEEP_PHOTOS)
                .forEach { it.delete() }
        } catch (_: Throwable) { }
    }

    private fun initLlmAsync() {
        Thread {
            try {
                val extDir = getExternalFilesDir(null)
                if (extDir == null) {
                    llmError = "getExternalFilesDir 返回 null"
                    statusText = "无法访问 App 目录"
                    return@Thread
                }
                val modelPath = File(extDir, MODEL_REL_PATH).absolutePath
                if (!File(modelPath).exists()) {
                    llmError = "模型未就位"
                    statusText = "请用 adb push 模型到 App 目录"
                    return@Thread
                }
                Log.i(TAG, "init llm at $modelPath")
                llmReady = MnnBridge.init(modelPath, cacheDir.absolutePath)
                if (llmReady) {
                    statusText = "MNN 已就绪 · 点快门"
                } else {
                    llmError = "MNN init 失败"
                    statusText = "MNN 加载失败"
                }
            } catch (e: Throwable) {
                llmError = e.message ?: e.javaClass.simpleName
                statusText = "MNN 异常: $llmError"
            }
        }.start()
    }

    private fun runInference(imagePath: String) {
        if (!llmReady) {
            statusText = "MNN 还没初始化好:$llmError"
            return
        }
        processing = true
        statusText = "AI 正在识别…"
        Thread {
            val result = try {
                MnnBridge.runVlm(imagePath, SIGN_PROMPT)
            } catch (e: Throwable) {
                Log.e(TAG, "inference failed", e)
                "{\"error\":\"${e.message}\"}"
            }
            processing = false
            runOnUiThread {
                val merchant = parseMerchant(result)
                val storeName = merchant["店名"]?.toString().orEmpty()
                val cuisine = merchant["菜系"]?.toString().orEmpty()
                if (storeName.isBlank()) {
                    statusText = "未识别到店名"
                    return@runOnUiThread
                }
                // 优先用 intent extra(adb debug 可注入),否则用 GPS
                fun readDebugExtra(key: String, altKey: String): Double? {
                    return when {
                        intent.hasExtra(key) -> intent.getDoubleExtra(key, 0.0)
                        intent.hasExtra(altKey) -> intent.getDoubleExtra(altKey, 0.0)
                        else -> null
                    }
                }
                val debugLat = readDebugExtra("lat", "debug_lat")
                val debugLng = readDebugExtra("lng", "debug_lng")
                val loc = if (debugLat != null && debugLng != null) {
                    LocationProvider.LatLng(debugLat, debugLng)
                } else try {
                    kotlinx.coroutines.runBlocking {
                        LocationProvider.getLastLocation(this@SignScanActivity, timeoutMs = 2500)
                    }
                } catch (_: Throwable) { null }
                val intent = android.content.Intent(
                    this, ResultActivity::class.java
                ).apply {
                    putExtra("raw_json", result)
                    putExtra("image_path", imagePath)
                    putExtra("店名", storeName)
                    putExtra("菜系", cuisine)
                    if (loc != null) {
                        putExtra("lat", loc.lat)
                        putExtra("lng", loc.lng)
                    }
                }
                startActivity(intent)
                finish()
            }
        }.start()
    }

    private fun parseMerchant(raw: String): Map<String, Any?> {
        val cleaned = raw.replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()
        return try {
            val o = JSONObject(cleaned)
            mapOf(
                "店名" to o.optString("店名"),
                "菜系" to o.optString("菜系"),
                "招牌文字" to o.optJSONArray("招牌文字")?.toString(),
                "招牌颜色" to o.optString("招牌颜色"),
                "置信度" to o.optDouble("置信度", 0.0)
            )
        } catch (e: Throwable) {
            mapOf("error" to (e.message ?: "parse failed"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MnnBridge.release()
    }
}

@Composable
fun CaptureScreen(
    llmReady: Boolean,
    statusText: String,
    processing: Boolean,
    onShutter: () -> Unit,
    onAlbum: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 背景大渐变(模拟取景氛围)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF333333),
                            Color(0xFF666666),
                            Color(0xFF999999)
                        )
                    )
                )
        )

        // 顶部 bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", fontSize = 28.sp, color = Color.White, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.weight(1f))
            Text("干饭省省", fontSize = 18.sp, color = Color.White)
            Spacer(Modifier.weight(1f))
            Text("⋯", fontSize = 24.sp, color = Color.White)
        }

        // 中央取景框
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(260.dp)) {
                Corner(Alignment.TopStart)
                Corner(Alignment.TopEnd)
                Corner(Alignment.BottomStart)
                Corner(Alignment.BottomEnd)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(220.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("对准餐厅招牌", fontSize = 16.sp, color = Color(0xFF333333))
                        Spacer(Modifier.height(6.dp))
                        Text(statusText, fontSize = 11.sp, color = Color(0xFF666666))
                    }
                }
            }
        }

        // 底部控制条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 相册按钮
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF333333))
                    .clickable { onAlbum() },
                contentAlignment = Alignment.Center
            ) {
                Text("📷", fontSize = 24.sp)
            }

            // 大快门按钮(相机)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFFC299), Color(0xFFFF9966))
                        )
                    )
                    .border(4.dp, Color.White, CircleShape)
                    .clickable(enabled = llmReady && !processing) { onShutter() },
                contentAlignment = Alignment.Center
            ) {
                if (processing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9966))
                    )
                }
            }

            // 闪光灯按钮
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF333333))
                    .clickable { /* TODO: 闪光灯 */ },
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun BoxScope.Corner(align: Alignment) {
    val size = 24.dp
    val stroke = 3.dp
    Box(
        modifier = Modifier
            .align(align)
            .size(size)
            .border(
                width = stroke,
                color = Color.White,
                shape = RoundedCornerShape(2.dp)
            )
    )
}

// statusBarsPadding 兼容
@Composable
fun Modifier.statusBarsPadding(): Modifier =
    this.padding(top = androidx.compose.ui.unit.Dp(40f))
