# 拍照比团购 · 端云协同 Android App 开发计划

> **场景**:拍照餐厅招牌 → 端侧 Qwen3-VL 识别商家 → 比对抖音 / 大众点评团购
> **参赛**:**端侧 AI 创新挑战赛**(MNN + Qwen 赛道)
> **关键截止**:**2026-06-22 23:59 初赛**(创意方案文档 + 演示视频 + 小红书项目介绍)
> **版本**:v0.5 — **官方三教程整合版**(2026-06-13 同步)
> **剩余时间**:**9 天**(紧迫)
> **目标**:用最小 PoC 跑通"端侧识别"核心链路,讲故事,交 3 件套

---

## ⚠️ 0. 紧急重排说明(必读)

我之前的 v0.2 计划是按"做完整可用 App"思路给的 10-12 周节奏,**没意识到初赛 6.22 就截止**。剩 **9 天**,完整 App 肯定做不出来。

**新策略**:把整个项目拆成"PoC 阶段(初赛)+ 完整阶段(决赛)"两段:
- **PoC 阶段(9 天,初赛)**:聚焦"端侧 Qwen3-VL-2B 离线识别招牌"这个**最酷、最贴题**的能力,团购数据全部 mock。先交出能讲故事的 3 件套。
- **完整阶段(决赛 ~7 周)**:补齐云端商家识别、真实团购聚合、比价引擎。

**官方三篇教程已整合**(本版关键升级):
- **教程 1**(让手机拥有视觉感知能力):MCP + Qwen3-VL + libMNN.so 编译部署全流程
- **教程 2**(Agent 辅助开发):Native C++ Android 工程 + JNI 桥接 + 模型推送 + Bitmap 转 Tensor
- **教程 3**(端侧 AI 提速 80%):SME2 编译开关 + `llm_bench` 性能对比 + 真实数据 Prefill +81%

这三篇教程给出了**完整的官方实战路径**,我把所有能直接用的招式都整合到本计划里。**走官方路线,风险最低,数据最扎实,文档最容易得高分**。

---

## 1. 初赛交付物(6.22 23:59 必交)

| 交付物 | 形式 | 关键内容 | 预估工时 |
|---|---|---|---|
| **创意方案文档** | PDF / Word,5-15 页 | 痛点分析、技术方案、端云分工、SME2 优化点、原型截图、决赛路线图 | 2 天(从现有文档改) |
| **原型演示视频** | 视频,2-3 分钟 | 街边拍招牌 → 端侧识别 → 展示结果 → 飞行模式再次识别(端侧亮点) | 3 天(含录制+剪辑) |
| **小红书项目介绍** | 小红书图文 | 种草风格,带演示视频链接,带"端侧 AI"tag | 0.5 天 |

**核心叙事**:
> 普通消费者街边看到一家餐厅想知道哪家团购便宜 → 我们的 App **完全离线**(飞 mode 也能用)拍招牌 → 端侧 Qwen2-VL 直接告诉你是哪家店 → 立刻比价 → 隐私不出手机,零延迟,零流量

**差异化卖点**:
1. **完全离线**(其他比价 App 都要联网)
2. **隐私保护**(招牌图不出手机)
3. **速度快**(端侧 2 秒出商家识别,不等云端)
4. **Arm SME2 优化**(技术上扎实的证据)

---

## 2. PoC 阶段(19 天)系统架构

### 2.1 极简分工(初赛版)

| 任务 | 端侧 | 云端 |
|---|---|---|
| 拍照 | ✅ | |
| **端侧 Qwen2-VL-2B 商家识别(主卖点)** | ✅ | |
| 端侧 OCR(PaddleOCR,辅助字段提取) | ✅ | |
| **团购数据展示(初赛版)** | ✅ 本地 mock | |
| UI 展示 | ✅ | |

> **关键决策**:初赛阶段**完全不上云端**,所有能力都跑在手机上。理由:1) 端侧比赛就该讲端侧故事;2) 19 天内不接云端能省 1 周;3) 决赛再补云端协同。

### 2.2 极简数据流

```
1. 用户点"扫一扫" → 拍照(展示招牌)
2. 端侧 Qwen2-VL-2B 直接看图
   ↓ 2-4 秒
3. 输出 JSON:{店名, 菜系, 招牌文字, 置信度}
4. 端上展示识别结果(可以手动修正)
5. (演示用)点击"比价"→ 加载本地 mock 团购数据
   ↓ 1 秒
6. 展示"抖音 9.9 折 vs 点评 9.5 折"对比(数据写死的,但 UI 仿真)
7. 录制时重点录:飞行模式下仍能跑通
```

### 2.3 完整架构图(初赛 + 决赛整体观)

```
┌──────────────────────────────────────────────────────────────┐
│                    Android App (Kotlin)                       │
│                                                              │
│   ┌──────────┐  ┌─────────────────────┐  ┌──────────────┐   │
│   │ CameraX  │─▶│ MNN Runtime         │─▶│ Qwen2-VL-2B  │   │
│   │ 拍照     │  │ (libMNN.so + LLM)   │  │ INT4 + SME2  │   │
│   └──────────┘  │ + Arm SME2 加速     │  └──────┬───────┘   │
│                 └─────────────────────┘         │ 商家 JSON   │
│                                                  ▼             │
│   ┌──────────┐    ┌─────────────────┐   ┌─────────────────┐  │
│   │ Compose  │◀──▶│ 本地 mock 团购  │◀─▶│ PaddleOCR(MNN)  │  │
│   │  UI 层   │    │ (assets/...)    │   │ 辅助字段提取   │  │
│   └──────────┘    └─────────────────┘   └─────────────────┘  │
└──────────────────────────────────────────────────────────────┘

[初赛阶段] 完全本地运行
[决赛阶段] + 云端 DashScope Qwen-VL-Max + 团购聚合服务
```

---

## 3. 关键技术选型(PoC 视角)

### 3.1 端侧多模态 — **Qwen3-VL-2B-Instruct-MNN** ✅
**直接采用官方预转换模型**(教程 1 + 教程 2 都推荐),不要自己转。

- 模型地址:**`https://modelscope.cn/models/MNN/Qwen3-VL-2B-Instruct-MNN`**(官方已 INT4 量化,约 1.3-1.4GB)
- 模型目录包含 6 个文件:
  ```
  config.json
  llm.mnn + llm.mnn.weight
  visual.mnn + visual.mnn.weight
  tokenizer.txt
  ```
- 选型理由:体积(1.3GB)+ 速度(2-4s)+ 能力 三者最优平衡;官方预转换,省 0.5-1 天
- 备选:教程 3 用 **Qwen3-VL-4B-Instruct-MNN**(能力更强但 2 倍体积,4B 模型对内存压力更大,Neo 11 16GB 跑得动但首次加载慢)
- **本项目选 2B** —— 招牌识别不需要 4B 的推理深度,2B 速度快、内存友好

### 3.2 端侧 OCR(辅助)— **PaddleOCR Mobile (MNN)**
- 用作 Qwen-VL 的兜底:招牌特别模糊时,VLM 漏字,OCR 补
- 也用于结构化字段提取(如分店编号、地址)
- PoC 阶段可先不上(用 Qwen3-VL 看图就行),备用

### 3.3 推理引擎 — **MNN 官方源码编译**
**重大更新**:不再依赖 MNN Chat fork,改为**自己编译官方 `libMNN.so`**(教程 1 + 教程 3 完整路径)。

- 关键编译参数(教程 3 实战):
  ```bash
  cmake .. -DMNN_BUILD_LLM=true \
           -DLLM_SUPPORT_AUDIO=true \
           -DMNN_BUILD_OPENCV=true \
           -DMNN_SEP_BUILD=OFF    # 关键!LLM+视觉+音频 全部静态链接到同一个 .so
  ```
- `MNN_SEP_BUILD=OFF` 是教程 1 强调的:**默认构建只有几 MB 基础库,不支持多模态**;改成 OFF 才能得到完整的 libMNN.so
- 编译产物:`MNN/project/android/build_64/lib/libMNN.so`(通常 10-30MB)
- 放到 Android 工程:`app/src/main/jniLibs/arm64-v8a/libMNN.so`

### 3.4 演示设备 — **iQOO Neo 11(骁龙 8 Elite,16G+1T)** ✅

| 关键参数 | 规格 |
|---|---|
| SoC | 高通 **骁龙 8 Elite (SM8750-AC)**,台积电 3nm |
| CPU | **Oryon V2 自研核心**:2 × 4.47GHz Phoenix L + 6 × 3.53GHz Phoenix M |
| GPU | **Adreno 830**(硬件光追,Hexagon NPU 820 75 TOPS) |
| 内存 | 16GB LPDDR5X Ultra(9600Mbps) |
| 存储 | 1TB UFS 4.1 |
| 电池 | 7500mAh + 100W 快充 |
| 系统 | Android 16 + OriginOS 6 |

**SME2 支持情况**:**待 MNN `llm_bench` + `adb logcat` 验证**。Oryon V2 是 Qualcomm 自研核心(基于 ARMv8 兼容),可能支持也可能不支持 SME2 扩展;但 MNN 框架会自动检测,**支持就走 SME2,不支持自动回退到 i8mm**,业务代码无需修改。

**关键验证方法**(教程 3):
```bash
# 跑 llm_bench 时另开窗口
adb logcat | grep "device supports"
# 输出: The device supports: i8sdot:1, fp16:1, i8mm:1, sve2:1, sme2:1
# 其中 sme2: 1 表示支持(否则不显示或 0)
```

**已知优势**:
- **Adreno 830 + OpenCL 后端** 跑 Vision Encoder 视觉部分
- **2 × 4.47GHz 超大核** 单核性能炸裂
- **16GB RAM** 装得下 Qwen3-VL-2B(1.3GB)+ 系统 + 应用
- **75 TOPS NPU** 后续可以走高通 QNN SDK 加速(备选方案)

**为什么用 MNN 框架不用担心 SME2**:
- MNN 官方文档明确:编译期默认开 `-DMNN_SME2=ON`,运行期自动检测
- 同一份代码在 Armv8.2 / Armv8.6 / Armv9 + SME2 设备上都能跑
- 这是 MNN 框架的**工程价值**,也是比赛可以重点讲的"跨设备兼容"故事

### 3.5 端侧硬件加速方案(🏆 比赛加分项) — **MNN 跨设备 SME2 + Adreno 830 GPU**

根据官方《在 MNN 框架中开启 Arm SME2 加速 CPU 推理 v1.1》文档 + 教程 3,**MNN 框架原生支持 Arm SME2**:
- **编译期**:`-DMNN_SME2=ON`(官方默认开启)
- **运行期**:MNN 自动检测 CPU 是否支持 SME2,支持就走 SME2,不支持自动回退到 Armv8.6(i8mm)/Armv8.2(dotprod)
- **业务代码**:**零修改**,同份代码跨设备兼容
- **官方数据**(教程 3 实测,vivo X300 + Qwen3-VL-4B):
  - **Prefill 阶段 +81%**(计算密集型,大 tile 矩阵乘,SME2 矩阵外积指令优势)
  - **Decode 阶段 +13%**(逐 token 生成,瓶颈在内存带宽,SME2 优势有限)
- **生态支持**:vivo、OPPO 等厂商已推出集成 SME2 的 Arm C1 CPU 旗舰机,iOS 同样大幅启用

iQOO Neo 11 是 vivo 子品牌,Oryon V2 核心(基于 ARMv8 兼容指令集)的 SME2 支持情况需要在 MNN 框架内**实际验证**:
- ✅ 若自动检测到 SME2 → 享受 2-3 倍 Prefill 加速
- ❌ 若不支持 → MNN 自动回退到 i8mm,业务无感知

**Layer 1:MNN Arm SME2 加速(主卖点,🏆 比赛官方技术点)**
- 编译:用官方 `project/android/build_64.sh` 脚本(默认带 `-DMNN_SME2=ON -DMNN_ARM82=ON -DMNN_BUILD_LLM=ON`)
- 验证:`./llm_bench -m /path/to/config.json` 看 Prefill 阶段的 token/s
- 创意文档关键素材:用 `llm_bench` 在 iQOO Neo 11 上跑出 SME2 加速 vs 不加速的对比数据

**Layer 2:Adreno 830 GPU 加速(补充)**
- 编译:同时开 `-DMNN_OPENCL=ON`
- 运行时:`backend_type=opencl`(`config.json` 里改)
- 跑 Qwen2-VL-2B 的 Vision Encoder 部分(图像特征提取)走 GPU,VLM 主干走 CPU
- 双路分工:**GPU 跑视觉编码 / CPU 跑 LLM**(避免 GPU 调度开销拖累 LLM decode 阶段)

**Layer 3:Oryon V2 大核调度 + INT4 量化 + mmap**
- 进程绑大核:`taskset 0xC0`(第 7、6 核)
- 1.2GB 模型走 `use_mmap=true`,边加载边推理
- 预热缓存:首次加载后保留 kv cache

**MNN 编译参数**(推荐用官方脚本,不要手写 cmake):
```bash
# 推荐:官方 build_64.sh 脚本,默认就开启了 SME2 + LLM
cd MNN/project/android
./build_64.sh    # 默认 -DMNN_SME2=ON -DMNN_ARM82=ON -DMNN_BUILD_LLM=ON -DMNN_OPENCL=ON
```

若需要手动 cmake,关键参数:
```bash
cmake .. -DMNN_BUILD_LLM=ON \
         -DMNN_SME2=ON \
         -DMNN_ARM82=ON \
         -DMNN_OPENCL=ON \
         -DMNN_LOW_MEMORY=ON
```

**模型转换调优参数(取自 MNN 实战参考文章)**:
| 参数 | 含义 | 你的选择 | 理由 |
|---|---|---|---|
| `--quant_bit` | 权重量化位数 | **4** | 1.2GB 模型,手机友好,精度够用 |
| `--quant_block` | 量化分块大小 | **32** | 精度优先(128 更小但精度掉) |
| `-hqq` | 混合精度量化 | **开** | 推荐,精度提升 |
| `--awq` | AWQ 量化(需校准集) | 跳过 | 转换慢,我们时间紧 |
| `--lm_quant_bit` | lm_head 单独量化 | **4** | 减重 ~50MB,精度损失小 |

**运行时调优(改 config.json)**:
```json
{
  "backend_type": "cpu",          // LLM 主体走 CPU(SME2 加速)
                                  // Vision Encoder 部分在代码里指定 opencl
  "thread_num": 6,                // Oryon V2 6 个大核
  "precision": "low",             // FP16
  "use_mmap": true,               // 1.2GB 模型边加载边推理
  "sampler_type": "greedy",       // 招牌识别要确定性
  "max_new_tokens": 256           // 输出 JSON 限长
}
```

**验证 SME2 是否生效(🏆 关键证据采集)**:
```bash
# PC 端(若 adb pull 出来的 .so 不能直接跑,先 push 到手机)
adb push llm_bench /data/local/tmp/
adb shell chmod +x /data/local/tmp/llm_bench
adb shell /data/local/tmp/llm_bench -m /sdcard/Download/qwen2-vl-2b-mnn/config.json
# 观察输出里的 Prefill 阶段 token/s 和 "backend" 字段
```

输出关键看:
- `Backend: SME2` ✅ → 生效,讲 SME2 加速
- `Backend: ARM82` / `i8mm` → 回退,讲"MNN 跨设备兼容 + Adreno 加速"

**演示证据**(用于创意文档 + DEMO 视频):
1. **MNN `llm_bench` 输出截图**:展示 Prefill token/s(比纯 CPU 提升 2-3 倍)
2. **Adreno GPU Profiler 截图**:GPU 跑 Vision Encoder,利用率 80%+
3. **perfetto/systrace 截图**:展示 CPU SME2 指令 + GPU compute kernel 并行
4. **速度对比表**:SME2 加速 / i8mm 回退 / Adreno GPU 三组数据
5. **功耗截图**:`dumpsys batterystats` 单次识别耗电 < 0.5%

> **结论**:**不要纠结"我的芯片到底支不支持 SME2"** — MNN 框架会**自动检测并调度**,我们只需要:
> 1. 编译时确保 `-DMNN_SME2=ON`(官方默认就是)
> 2. 跑 `llm_bench` 验证,采集真实数据
> 3. 创意文档里讲"MNN 跨设备兼容"这个**框架工程价值**(比赛加分点)
>
> 这是比"SME2 一定生效"更扎实的工程故事。

### 3.6 工程路线 — **官方教程实战路径**(🏆 风险最低)

**重大调整**:放弃"fork MNN Chat 二次开发"思路,改用**官方三篇教程给出的实战路径**。理由:
- 教程 2 明确推荐 **Native C++ Android 工程 + JNI 桥接**(MNN Chat 是 Compose 封装,反而增加调试难度)
- 教程 1 提供了完整的 libMNN.so 编译 + libMNN.so JNI 加载顺序(`System.loadLibrary("MNN")` 必须先于业务 .so)
- 教程 3 给出了 SME2 真实数据(Prefill +81%),不需要自己造数据

**官方工程结构**(教程 1 提到的 karminski/eyes-on-my-phone):
```
android-app/
├── app/
│   ├── src/main/
│   │   ├── jniLibs/arm64-v8a/libMNN.so   # MNN 引擎动态库(自己编)
│   │   ├── java/...                       # Kotlin 业务
│   │   └── cpp/eyes_mnn_bridge.cpp        # JNI 桥接
│   └── build.gradle
├── MNN-master/                           # MNN 头文件(只读)
└── build.gradle
```

**对应到我们项目**:
```
m-qwen/
├── app/                                   # Android Studio Native C++ 工程
│   ├── src/main/
│   │   ├── jniLibs/arm64-v8a/libMNN.so    # 自己编的 MNN 引擎
│   │   ├── java/.../ScanActivity.kt        # 拍照 + 调 MNN
│   │   └── cpp/qwen_bridge.cpp             # JNI 桥接
│   └── build.gradle
├── MNN/                                   # MNN 源码(只读)
└── docs/                                  # 项目文档
```

**调整后的 Phase 0/1 流程**:
1. Day 1 上午:阿里云 + DashScope + 设备确认
2. Day 1 下午:Android Studio 创建 **Native C++** 工程 + NDK + Git + 真机
3. Day 1 晚上:**下载 Qwen3-VL-2B-Instruct-MNN**(官方预转换,1.3GB)
4. Day 2 上午:**编译 MNN 源码**(教程 1 完整路径,用 `-DMNN_SEP_BUILD=OFF` 关键参数)
5. Day 2 下午:`adb push` 模型 + .so 到手机,跑通 `llm_demo` 命令行
6. Day 2 晚上:Phase 0 验收(拍照 → 端云识别 → 跑 `llm_bench` 验证 SME2)

> **对比 v0.4**:从"MNN Chat 二次开发"改回"Native C++ + JNI 桥接",代码量略多但**路径更清晰、数据更扎实**(教程 3 给了 SME2 真实数据,直接用)。

### 3.7 团购数据(初赛版)
- **PoC 阶段**:用 `assets/mock_deals.json` 写死 20-30 个真实餐厅的 mock 团购数据
- 后续接云端:见 §10 决赛路线

### 3.8 Android 端技术栈
| 层 | 选型 |
|---|---|
| 语言 | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| 相机 | CameraX |
| 异步 | Coroutines + Flow |
| 本地存储 | Room(历史)+ DataStore(偏好)+ assets(mock 团购) |
| 端侧推理 | MNN(.so + LLM,Native C++ JNI 桥接) |
| 演示计时 | 内部 stopwatch + systrace |
| **基线项目** | **Android Studio Native C++ 模板 + 教程 1/2 实战** |

---

## 4. PoC 9 天冲刺 WBS(⏰ 6.13 → 6.22)

### Phase 0: 环境 + 编译 + 模型准备(Day 1-2,2 天) 🏆

- [ ] Day 1 上午(2h):DashScope + OSS Bucket + 设备确认(走 `phase0-tasks.md` 任务 1.1-1.3)
- [ ] Day 1 下午(3h):Android Studio 创建 **Native C++** 模板工程 + NDK r27 安装 + Git + 真机 adb
- [ ] Day 1 晚上(2h):**下载 Qwen3-VL-2B-Instruct-MNN** 到 `models/MNN/`(~1.3GB,后台下载)
- [ ] Day 2 上午(3h):**编译 MNN libMNN.so** —— 教程 1 + 教程 3 完整路径
  - 克隆 MNN 源码
  - 配置 NDK 环境变量
  - cmake 参数:`-DMNN_BUILD_LLM=true -DLLM_SUPPORT_AUDIO=true -DMNN_BUILD_OPENCV=true -DMNN_SEP_BUILD=OFF`
  - make install
  - 验证:`build_64/lib/libMNN.so` 存在,大小 > 5MB(基础库就是几 MB,带 LLM 的至少 10MB+)
- [ ] Day 2 下午(3h):
  - `adb push` libMNN.so 到 `android-app/app/src/main/jniLibs/arm64-v8a/`
  - `adb push` 模型文件夹到 `/data/local/tmp/mnn_models/qwen3-vl-2b/`
  - **用 `llm_demo` 命令行先跑通一次**,验证模型能加载、能推理
  - 推 `llm_bench` + 跑 Prefill 测试
- [ ] Day 2 晚上(1h):**采集 SME2 数据**(`adb logcat | grep "device supports"` 截图)
- [ ] Day 2 晚上(0.5h):写 `docs/phase0-acceptance.md`

**关键额外任务**:
- [ ] **Day 1 设备确认**:iQOO Neo 11 跑 adb + 装 Snapdragon Profiler
- [ ] **Day 1 晚上**:MNN 源码同步下载(后台,4GB+)

### Phase 1: Native C++ JNI 桥接 + App 集成(Day 3-5,3 天) 🏆

> 完全按教程 2 路径走,加自定义的招牌识别逻辑。

- [ ] **Day 3 上午**:在 Native C++ 工程里写 `qwen_bridge.cpp`(JNI 桥)
  - 加载 libMNN.so(`System.loadLibrary("MNN")` 优先)
  - 封装 `nativeRunVlm(prompt, imagePath) -> String` 接口
  - Bitmap 转 MNN Tensor(教程 2 第 9 步核心)
  - MultimodalPrompt 构造
- [ ] **Day 3 下午**:Kotlin 端 `ScanActivity`,CameraX 拍照 → 调 JNI → 解析 JSON
- [ ] **Day 4 上午**:Compose UI 三页(扫码 → 加载 → 结果)
- [ ] **Day 4 下午**:JSON 解析为 `Merchant` 数据类 + mock 团购匹配
- [ ] **Day 5 上午**:`config.json` 调优(`backend_type`, `precision=low`, `use_mmap=true`)
- [ ] **Day 5 下午**:**输出截断补丁**(教程 1 踩坑经验:短于 48 字符忽略 EOS 信号)
- [ ] **Day 5 晚上**:Phase 1 验收:拍招牌 2 秒出结果

**验收标准**:
- iQOO Neo 11:2-3 秒出结果
- 模型加载时间 < 3s(二次)
- 识别准确率 ≥ 80%(测试 10 张招牌)
- 抓取 SME2 device supports 日志截图(关键素材)
- 抓取 `llm_bench` Prefill/Decode token/s 截图(创意文档核心数据)

---

### Phase 2: UI + Mock 团购 + 比价(Day 6-7,2 天,极简版)

- [ ] **Day 6 上午**:准备 20 个真实餐厅的 mock 团购 JSON(assets/mock_deals.json)
- [ ] **Day 6 下午**:Compose 三页(扫码 → 加载 → 结果/比价)极简实现
- [ ] **Day 7 上午**:比价页(左抖音/右点评,顶部"省 XX 元")+ 端到端联调
- [ ] **Day 7 下午**:整链路压测(各种招牌照,记录识别率 + 速度)

**关键**:mock 数据要用真实店名 + 真实套餐类型(比如"瑞幸咖啡 9.9 元券")看起来才真实。

---

### Phase 3: Demo 视频 + 文档(Day 8,1 天)⏰ 极限压缩

- [ ] **Day 8 上午**:精修创意方案文档(从本文档改,加 PoC 截图 + SME2 数据)
- [ ] **Day 8 中午**:录视频素材(街边拍 3-5 张招牌,流程演示 + 飞行模式演示)
- [ ] **Day 8 下午**:剪映剪辑,出 2-3 分钟视频
- [ ] **Day 8 晚上**:写小红书图文(种草风格,带视频链接)
- [ ] **Day 8 晚上**:三件套自检(时长/格式/链接)

---

### Phase 4: 提交(Day 9,6.22)

- [ ] **Day 9 上午**:上传视频 B 站/YouTube
- [ ] **Day 9 中午**:发布小红书
- [ ] **Day 9 下午**:钉钉表单填写 + 提交
- [ ] **Day 9 晚上**:截图存档,等结果

---

## 5. 9 天极限时间表(⏰ 速查)

| Day | 任务 | 关键产出 |
|---|---|---|
| **1** | 阿里云 + Android Studio + Native C++ 工程 | 工程可建,NDK 装好 |
| **2** | MNN libMNN.so 编译 + 模型 push + llm_bench | 引擎就位,SME2 数据采集 |
| **3-5** | Native C++ JNI 桥 + 拍照 + UI 极简 | App 能拍招牌 2 秒出结果 |
| **6-7** | mock 团购 + 比价 UI + 端到端联调 | 完整 PoC 跑通 |
| **8** | 视频 + 文档 + 小红书 | 三件套成稿 |
| **9** | **6.22 提交** | **初赛交付完成** |

> **极限状态**:单日工时 10-12h,周末不休,每天砍掉一切非核心需求。
> **滑期预案**:Phase 2 滑期 → 砍 mock 比价,只做"识别出商家名"单页;Phase 3 滑期 → 用手机自带相机录屏 + 简单剪辑。

---

## 6. DEMO 视频脚本(初赛版,2-3 分钟)

```
[00:00-00:15]  开场(15s)
  画面:街边一家餐厅的招牌特写
  字幕:"街边看到一家好吃的,想知道抖音和大众点评哪家团购更便宜?"
  BGM:轻快

[00:15-00:30]  痛点(15s)
  画面:用户分别打开抖音和点评,翻列表截图
  字幕:"打开两个 App 翻半天,流量没了,隐私也没了"

[00:30-00:50]  产品介绍(20s)
  画面:App Logo + 简单动画
  字幕:"扫招牌:端侧 AI · 完全离线 · 隐私不出手机"
  关键 tag:#端侧AI #MNN #Qwen

[00:50-01:40]  主流程演示(50s)🏆 核心
  画面 1:用户举起手机对准一家瑞幸(0:50-1:05)
  画面 2:App 拍照后 2 秒弹出结果"瑞幸咖啡 · 咖啡 · 置信度 92%"(1:05-1:20)
  画面 3:点"比价"→ 展示"抖音 9.9 折 vs 点评 9.5 折 → 在抖音下单更划算"(1:20-1:40)
  字幕:实时显示推理耗时

[01:40-02:10]  端侧亮点(30s)🏆 关键卖点
  画面 1:打开手机飞行模式(1:40-1:45)
  画面 2:再次拍招牌(1:45-1:55)
  画面 3:App 仍然能识别出商家(1:55-2:10)
  字幕:"完全离线 · 端侧推理 · 隐私 0 出手机"

[02:10-02:30]  技术亮点(20s)
  画面 1:`llm_bench` Prefill/Decode 性能截图
  画面 2:`adb logcat` 显示 "The device supports: i8mm:1, sve2:1, sme2:1"
  字幕:"基于 MNN + Arm SME2 指令集,官方实测 Prefill 提速 81%"

[02:30-03:00]  结尾(30s)
  画面:团队 Logo + 决赛路线图(云端协同、真实团购、跨平台)
  字幕:"决赛见!" + 提交入口二维码
  BGM:渐强结束
```

---

## 7. 创意方案文档大纲(初赛版,5-15 页)

```
1. 项目概述(1 页)
   - 痛点 + 目标用户 + 一句话解决方案

2. 技术方案(3-4 页)🏆 重点
   - 端云分工(用 §2 架构图)
   - 端侧 Qwen3-VL-2B-MNN 选型理由
   - **MNN + Arm SME2 加速方案**(教程 3 实战数据:Prefill +81%)
     - MNN_SEP_BUILD=OFF 编译策略
     - `adb logcat` device supports 截图
     - `llm_bench` Prefill/Decode token/s 对比
   - MNN 编译参数(贴关键 cmake 命令)
   - 性能数据表(冷启动、推理耗时、内存、SME2 加速比)

3. 原型演示(2-3 页)
   - 关键流程截图(6-8 张)
   - 离线模式演示截图
   - 视频二维码(小红书/B 站链接)

4. 端云协同路线图(2-3 页)
   - 决赛计划:云端 DashScope Qwen-VL-Max 协同
   - 团购聚合(抖音/点评方案 A+C)
   - iOS 端(基于 MNN 跨平台)
   - **MCP + Agent 拓展**(教程 1 提到的"开发板千里眼/3D 打印保安"场景,作为附加想象空间)

5. 团队 & 时间线(1 页)
   - 1 人 9 天极限冲刺的故事
   - 6.22 → 8.14 决赛规划
```

---

## 8. 风险与应对(初赛版)

| 风险 | 概率 | 影响 | 应对 |
|---|---|---|---|
| Qwen2-VL-2B 转换失败 | 中 | 致命 | 降级到 Qwen3-0.6B + OCR 端到端 |
| ~~设备不支持 SME2~~ | 已识别 | 已处理 | MNN 框架自动检测:支持就走 SME2,不支持自动回退 i8mm/dotprod,**业务代码零修改**;详见 §3.5 |
| Adreno 830 OpenCL 后端不稳 | 低 | 中 | Plan B 用 Hexagon NPU(QNN SDK);Plan C 改 Vulkan |
| 19 天内某 Phase 滑期 | 中 | 高 | 优先砍 Phase 3 mock UI,保留主流程 |
| 演示视频录得不顺 | 中 | 中 | Day 15-16 留 2 天缓冲 |
| 钉钉表单/小红书链接失效 | 低 | 低 | 提前 1 天发布,验证可访问 |
| 真实招牌识别率低 | 中 | 高 | 演示用 5-10 张"已验证能识别"的招牌 |

---

## 9. 已确认决策(更新)

| # | 决策点 | 选定方案 |
|---|---|---|
| 1 | 目标用户群 | 普通消费者 |
| 2 | 比赛 | 参加"端侧 AI 创新挑战赛",**6.22 提交初赛** |
| 3 | 后端预算 | **初赛不需要后端,等决赛再上** |
| 4 | 开发模式 | 单人 |
| 5 | MVP 团购数据源 | **初赛 mock,决赛再接 A+C** |
| 6 | iOS 端 | 决赛阶段做 |
| 7 | 目标设备 | **iQOO Neo 11(骁龙 8 Elite,16G+1T)——Oryon V2 + Adreno 830** |
| 8 | 演示数据 | 真实餐厅 + 写死 mock 团购 |
| 9 | 主卖点 | **"完全离线 + 端侧推理 + 隐私 0 出手机"** |
| 10 | 关键加分项 | **MNN 框架 Arm SME2 自动调度(支持就加速,不支持自动回退)+ Adreno 830 GPU 加速 Vision Encoder** |

---

## 10. 决赛路线图(轻量,8.14 前)

决赛 6.29-8.14,7 周时间,可做的事:

### 决赛目标
1. **接云端协同**:端云结果对比,展示"端侧快 + 云端准"
2. **真实团购聚合**:实现方案 A(百度搜索)+ C(第三方聚合)
3. **真实比价**:同店匹配 + 同款套餐对齐
4. **iOS 端**:基于 MNN 跨平台支持
5. **正式上架**:至少 1 个应用商店

### 时间分配(8 周,决赛)
- Week 1:云端服务搭建(后端 + 商家识别)
- Week 2:团购适配器(抖音 + 点评)
- Week 3:比价引擎 + UI 精修
- Week 4:iOS 端开发
- Week 5:端云协同演示 + 性能调优
- Week 6:用户测试 + Bug 修复
- Week 7:答辩材料 + 上架准备
- Week 8(8.15):线下答辩

### 决赛演示设备
准备 2-3 台不同档位手机,展示 SME2 在不同芯片上的加速比。

---

## 11. 立即可执行(Day 1)⏰ 今天是 Day 1

**从现在开始**(今天 6.13,剩 9 天):

1. **先读教程 1 + 教程 3**(各 20 分钟)—— 知道官方具体怎么走的
2. **打开 `phase0-tasks.md`**,从 Day 1 上午 1.1 开始
3. **设备已确认**:**iQOO Neo 11(骁龙 8 Elite,16G+1T)**
4. **Day 1 关键验证**:
   - `adb shell getprop ro.boot.hardware.platform` 确认是 SM8750
   - `adb shell getprop ro.product.cpu.abilist` 应该看到 `arm64-v8a` 之类
   - 装好 Adreno GPU Profiler 工具(Google Play 搜"Snapdragon Profiler")
5. **后台并行任务**(立刻做,不占白天):
   - 下载 MNN 源码(4GB+)
   - 下载 Qwen3-VL-2B-Instruct-MNN(1.3GB)
4. **注册端侧 AI 创新挑战赛选手群**(如果还没):公众号"通义实验室"找入口

---

## 12. 参考资料

**官方教程(必看):**
- 教程 1《让手机拥有视觉感知能力》:https://mp.weixin.qq.com/s/AlNKFllxdsdQwKsZg75zRw
- 教程 2《Agent 辅助开发,一站式打通 Qwen3-VL Android 端侧推理》:https://mp.weixin.qq.com/s/VhxGZp9FNCLbi3e_aoBRzg
- 教程 3《端侧 AI 提速 80%?如何让 Qwen3-VL 在手机起飞》:https://mp.weixin.qq.com/s/QNl4pn5JzxpEeFAEvq88ZA
- 官方 SME2 指南 v1.1:已发到群(本地 `C:\Users\40314\Downloads\【指南】在_MNN_框架中开启_SME2_加速_CPU_推理_v1-1 1.txt`)
- 官方示例项目 karminski/eyes-on-my-phone:https://github.com/karminski/eyes-on-my-phone

**技术资源:**
- MNN 官方:https://github.com/alibaba/MNN
- MNN-LLM 转换脚本:`transformers/llmexport/`
- **Qwen3-VL-2B-Instruct-MNN(预转换)**:https://modelscope.cn/models/MNN/Qwen3-VL-2B-Instruct-MNN
- PaddleOCR:https://github.com/PaddlePaddle/PaddleOCR
- 骁龙 8 Elite 技术规格:https://www.qualcomm.com/products/mobile/snapdragon-8-series-mobile-platforms
- Adreno GPU Profiler 工具(原名 Snapdragon Profiler):https://developer.qualcomm.com/software/snapdragon-profiler

**比赛相关:**
- 端侧 AI 创新挑战赛:见参赛必看群通知
- 提交入口:https://alidocs.dingtalk.com/notable/share/form/v01ZWGl05mxxMxEBn34_dv19yqvsgs3oebp3pcjys_1qX0QQ0

---

> **一句话总结(2026-06-13 更新)**:今天起 **9 天做一个能讲故事的 PoC**,核心是**官方教程 1/2/3 给出的实战路径** + 端侧 **Qwen3-VL-2B** + **MNN Arm SME2**。云端和真实团购留给决赛。立刻打开 phase0-tasks.md 开干。
