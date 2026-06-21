# Phase 1 拆日任务清单(Day 3-5)

> 对应主计划 `development-plan.md` v0.5 的 Phase 1
> 走官方教程 2 的 Native C++ JNI 路径,加自定义招牌识别 + mock 团购
> Day 1-2 已在 `phase0-tasks.md` 详细列出,本文件只覆盖 Day 3-5

---

## 前置依赖(必须完成)

- [ ] Day 2 已成功编译 `libMNN.so`(教程 1 + 教程 3 路径)
- [ ] `libMNN.so` 已 `adb push` 到 `app/src/main/jniLibs/arm64-v8a/`
- [ ] Qwen3-VL-2B-Instruct-MNN 模型已 `adb push` 到 `/data/local/tmp/mnn_models/qwen3-vl-2b/`
- [ ] `llm_demo` 命令行能跑通招牌识别(Phase 0 验收)
- [ ] `llm_bench` 已采集 Prefill/Decode 数据(创意文档素材)

---

## Day 3:Native C++ JNI 桥 + 极简 Kotlin 端

### 🌅 上午:JNI 桥代码(qwen_bridge.cpp)

#### 任务 3.1:在 Native C++ 工程下创建 JNI 源文件
- 路径:`app/src/main/cpp/qwen_bridge.cpp`
- 文件结构(参考教程 2 第 9 步):
  ```cpp
  #include <jni.h>
  #include <MNN/Interpreter.hpp>
  #include <MNN/Tensor.hpp>
  // MNN-LLM headers
  #include "llm/llm.hpp"
  
  // 关键:加载顺序(教程 1 强调)
  extern "C" JNIEXPORT jstring JNICALL
  Java_com_mqwen_scanapp_MnnBridge_nativeRunVlm(
      JNIEnv* env, jclass clazz,
      jstring jPrompt, jstring jImagePath
  ) {
      // 1. 构造 LLM config
      // 2. Llm::createLLM(config_path)
      // 3. Bitmap → MNN Tensor(教程 2 核心)
      // 4. MultimodalPrompt 构造
      // 5. response() 拿到 JSON
      // 6. 返回给 Kotlin
  }
  ```

#### 任务 3.2:配置 CMakeLists.txt(参考教程 2 Prompt)
- `app/src/main/cpp/CMakeLists.txt` 关键配置:
  ```cmake
  add_library(qwen_bridge SHARED qwen_bridge.cpp)
  target_link_libraries(qwen_bridge MNN android log jnigraphics)
  target_include_directories(qwen_bridge PRIVATE 
      ${CMAKE_SOURCE_DIR}/../../MNN/include
      ${CMAKE_SOURCE_DIR}/../../MNN/transformers/llm/engine/include
  )
  ```

#### 任务 3.3:实现 Bitmap → MNN Tensor 转换(教程 2 核心难点)
- Android 端 Bitmap 是 `ARGB_8888` 或 `RGB_565`
- Qwen3-VL 期望输入尺寸通常是 448x448 或 224x224(看 visual.mnn 期望)
- 代码关键点:
  ```cpp
  // 1. 获取 Bitmap 像素
  // 2. 缩放到模型期望尺寸
  // 3. RGB 归一化(0-255 → 0-1 或 ImageNet mean/std)
  // 4. HWC → CHW 转换
  // 5. 写入 MNN Tensor
  ```

#### 任务 3.4:跑通 nativeBuild
- Android Studio → Build → Make Project
- 检查 `app/build/intermediates/cmake/.../libqwen_bridge.so` 生成
- **常见错**:找不到 MNN header → 检查 include 路径

---

### 🌞 下午:Kotlin 端 MnnBridge 封装

#### 任务 3.5:封装 MnnBridge 单例
- `app/src/main/java/com/mqwen/scanapp/MnnBridge.kt`:
  ```kotlin
  object MnnBridge {
      init {
          // 关键加载顺序(教程 1 踩坑强调)
          System.loadLibrary("MNN")        // 先 MNN
          System.loadLibrary("qwen_bridge") // 再业务
      }
      
      external fun nativeRunVlm(prompt: String, imagePath: String): String
      external fun nativeRuntimeInfo(): String  // 显示 MNN 版本
  }
  ```

#### 任务 3.6:加最小测试 Activity
- `MainActivity.kt` 显示:
  - MNN 版本
  - ABI(arm64-v8a)
  - 模型文件存在性检查(config.json / llm.mnn / visual.mnn 等)
  - "Ready for Llm::createLLM" 状态
- 这步是教程 2 第 6 步,先把底座打通

#### 任务 3.7:App 装到手机 + 跑通
- `assembleDebug` 出 APK
- `adb install` 到 Neo 11
- 打开 App,确认所有 6 个模型文件都 OK
- **截图存档**:这个"底座就绪"截图进创意文档

---

### 🌙 晚上:扫招牌测试 + JSON 解析

#### 任务 3.8:实现扫招牌最小流程
- 加一个按钮 "扫码识别"
- 点击 → 选系统图库(或 CameraX 简化版)
- 调 `MnnBridge.nativeRunVlm(prompt, imagePath)`
- prompt 模板:
  ```
  你是餐厅招牌识别助手。请仔细看图,以 JSON 格式输出:
  {
    "店名": "string",
    "菜系": "string",
    "招牌文字": ["string", "string"],
    "招牌颜色": "string",
    "置信度": 0.0-1.0
  }
  只输出 JSON,不要其他内容。
  ```

#### 任务 3.9:输出截断补丁(教程 1 踩坑)
- 现象:模型输出 `{"summary": "` 后停止,token=2 直接结束
- 原因:Markdown 代码块前缀 ``` 触发假 EOS
- 解决:在 C++ 层加补丁,生成内容 < 48 字符忽略 EOS,强制继续
- 代码位置:`qwen_bridge.cpp` 推理循环里

#### 任务 3.10:解析 JSON 为 Merchant 数据类
```kotlin
@Serializable
data class Merchant(
    val 店名: String,
    val 菜系: String,
    val 招牌文字: List<String>,
    val 招牌颜色: String? = null,
    val 置信度: Double = 0.0
)
```

**Day 3 验收**:
- [ ] App 能拍(或选)一张招牌图
- [ ] 2-3 秒内输出 JSON
- [ ] 解析为 Merchant 数据类无错
- [ ] 输出截断补丁生效(招牌文字完整)

---

## Day 4:Compose UI + mock 团购

### 🌅 上午:Compose 三页骨架

#### 任务 4.1:三页路由
- `HomeActivity.kt`(或 `MainActivity` 改):
  - 第一页:首页(扫一扫按钮 + 最近识别)
  - 第二页:Loading(进度条 + "AI 正在识别..." 文案)
  - 第三页:Result(显示识别结果 + "查看团购比价" 按钮)
- 用 `NavHost` + `composable` 路由

#### 任务 4.2:首页设计
- Material 3 大按钮"扫招牌比团购"
- 历史记录列表(空状态)
- 底部 "Powered by Qwen3-VL + MNN"

#### 任务 4.3:Loading 页 + 计时器
- 启动 stopwatch
- 显示 "正在端侧识别..." + 实时秒数
- 失败/超时 友好降级

---

### 🌞 下午:结果页 + mock 团购

#### 任务 4.4:Result 页(识别结果)
- 显示:
  - 原图缩略图
  - 店名(大字号)
  - 菜系 / 招牌文字(辅助信息)
  - 置信度(进度条)
  - 识别耗时(下角小字)

#### 任务 4.5:mock 团购数据 + 匹配
- `app/src/main/assets/mock_deals.json`:
  ```json
  [
    {
      "店名关键词": ["瑞幸", "luckin"],
      "抖音": [
        {"name": "瑞幸 9.9 咖啡券", "price": 9.9, "sales": "10万+"},
        {"name": "瑞幸 生椰拿铁套餐", "price": 19.9, "sales": "5万+"}
      ],
      "点评": [
        {"name": "瑞幸 美式咖啡", "price": 12.5, "sales": "3万+"}
      ]
    },
    {"name关键词": "海底捞", ...}
  ]
  ```
- 准备 20 个真实餐厅

#### 任务 4.6:按店名关键词匹配 mock
- 简单 substring 匹配
- 找不到时显示"暂无团购"占位

---

### 🌙 晚上:比价页(极简)

#### 任务 4.7:比价 UI
- "查看团购比价" 按钮 → 进入比价页
- 左右分栏:
  - 左:抖音团购列表(价格大字)
  - 右:大众点评团购列表
  - 顶部:"省 XX 元" 高亮
- 同款匹配:先按店名匹配,再按套餐名关键词

**Day 4 验收**:
- [ ] 拍照 → 加载 → 结果 完整流程跑通
- [ ] 5 张不同招牌都能识别
- [ ] mock 比价页能正常展示
- [ ] 整体耗时 < 8 秒(从拍照到展示比价)

---

## Day 5:性能调优 + 踩坑修复 + 验收

### 🌅 上午:config.json 调优

#### 任务 5.1:模型 config.json 调优
- 路径:`mnn_models/qwen3-vl-2b/config.json`
- 关键参数:
  ```json
  {
    "backend_type": "cpu",          // LLM 走 CPU(SME2 加速)
    "thread_num": 6,                // Oryon V2 6 个大核
    "precision": "low",             // FP16
    "use_mmap": true,               // 1.3GB 模型 mmap
    "sampler_type": "greedy",       // 招牌识别确定性
    "max_new_tokens": 256           // 限长 JSON
  }
  ```

#### 任务 5.2:重新测试性能
- 拍照到结果的耗时
- GPU 利用率(Adreno Profiler)
- 内存峰值(`dumpsys meminfo`)

---

### 🌞 下午:踩坑修复(从教程 1 经验)

#### 任务 5.3:常见踩坑修复
- [ ] 模型加载卡在"verifying..." → 后台协程处理(教程 1 经验)
- [ ] libMNN.so 加载顺序错误 → 显式 `System.loadLibrary("MNN")` 优先
- [ ] Bitmap 转 Tensor 内存爆炸 → 用 `ByteBuffer.allocateDirect`
- [ ] 多个相机设备 → 启动时检测并提示

---

### 🌙 晚上:Phase 1 验收 + 数据采集(🏆 关键)

#### 任务 5.4:SME2 数据采集(创意文档核心素材)
- 跑 `llm_bench`:
  ```bash
  adb push build_64/llm_bench /data/local/tmp/
  adb push build_64/lib/libMNN.so /data/local/tmp/
  adb shell chmod +x /data/local/tmp/llm_bench
  adb shell LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/llm_bench -m /data/local/tmp/mnn_models/qwen3-vl-2b/config.json
  ```
- **采集 2 组数据**:
  - 编译时 `-DMNN_SME2=OFF` → Prefill/Decode token/s
  - 编译时 `-DMNN_SME2=ON`(默认)→ Prefill/Decode token/s
- 另开窗口:
  ```bash
  adb logcat | grep "device supports"
  # 输出: The device supports: i8sdot:1, fp16:1, i8mm:1, sve2:1, sme2:1
  ```
- **所有截图存到** `docs/phase1-evidence/`

#### 任务 5.5:Phase 1 验收
- [ ] 拍照到结果 < 4 秒
- [ ] 10 张招牌识别率 ≥ 80%
- [ ] 飞行模式下仍能跑(关键!)
- [ ] 抓取所有数据截图(给 Phase 3 文档用)

---

## Day 3-5 总验收检查表

- [ ] MNN runtime 加载成功(MNN + qwen_bridge 两个 .so)
- [ ] Qwen3-VL-2B 模型加载成功
- [ ] 拍照/选图 → JNI → JSON 全链路通
- [ ] 商家名准确识别 ≥ 80%(10 张招牌)
- [ ] Compose UI 三页路由通
- [ ] mock 团购比价页可展示
- [ ] 飞行模式离线可用
- [ ] SME2 验证数据采集齐
- [ ] 全部数据截图存档 `docs/phase1-evidence/`

**全部勾完 = Phase 1 完成,可以进入 Phase 2(录视频 + 写文档)**
