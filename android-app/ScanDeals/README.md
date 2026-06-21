# ScanDeals · 拍照比团购 Android App

> 端侧 AI · 完全离线 · 隐私不出手机

## 一句话说明

拍照餐厅招牌 → 端侧 Qwen3-VL-2B 识别商家 → mock 比对抖音 / 大众点评团购。

## 快速开始

### 1. 在 Android Studio 中打开

```
File → Open → 选择 D:\C-AI\m-code\m-qwen\android-app\ScanDeals
```

第一次打开时,Studio 会:
- 自动下载 Gradle wrapper(可能 5-10 分钟)
- 自动下载 Android Gradle Plugin 8.5.2
- 同步项目依赖

### 2. 配置 SDK

在 `android-app/ScanDeals/local.properties` 中确保:

```properties
sdk.dir=D\:\\C-AI\\m-code\\m-qwen\\tools\\android-sdk
ndk.dir=D\:\\C-AI\\m-code\\m-qwen\\tools\\android-sdk\\ndk\\27.2.12479018
```

(Studio 通常会自动写入,但确认下。)

### 3. 推送模型到手机(必需)

```powershell
adb push D:\C-AI\m-code\m-qwen\models\Qwen3-VL-2B-Instruct-MNN\MNN\Qwen3-VL-2B-Instruct-MNN C:\Users\40314\Downloads\qwen3-vl-2b
```

(我之前已经 push 到 `/data/local/tmp/mnn_models/qwen3-vl-2b/`,但代码期望 `/sdcard/Download/qwen3-vl-2b/`,
重新 push 到这个位置,或者改 `SignScanActivity.MODEL_PATH`。)

### 4. Build → Run

Studio 里选 Run → Run 'app',选 iQOO Neo 11 设备。

## 工程结构

```
ScanDeals/
├── build.gradle                # 顶层 Gradle
├── settings.gradle
├── gradle.properties
└── app/
    ├── build.gradle            # 模块 Gradle
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/mqwen/scandeals/
        │   ├── MainActivity.kt           # 首页
        │   ├── SignScanActivity.kt       # 拍照 + MNN 推理
        │   ├── ResultActivity.kt         # 比价结果
        │   ├── MnnBridge.kt              # JNI 桥封装
        │   └── MockDeals.kt              # mock 团购数据库
        ├── cpp/
        │   ├── CMakeLists.txt
        │   ├── qwen_bridge.cpp           # JNI native 实现
        │   ├── include/MNN/              # MNN 公开头
        │   └── llm/                      # MNN-LLM 头
        └── jniLibs/arm64-v8a/
            ├── libMNN.so                 # MNN 引擎(自己编译)
            └── libscandeals.so           # 构建时自动生成
```

## 关键命令

### 重新编译 MNN(如果改了模型 / 参数)

```bash
cd D:\C-AI\m-code\m-qwen\MNN\project\android
rm -rf build_64 && mkdir build_64 && cd build_64
cmake ../.. \
  -DCMAKE_TOOLCHAIN_FILE=../build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DMNN_BUILD_LLM=ON -DMNN_OPENCL=ON -DMNN_ARM82=ON -DMNN_SME2=ON \
  -DLLM_SUPPORT_VISION=ON -DMNN_SEP_BUILD=OFF -DMNN_LOW_MEMORY=ON \
  -DCMAKE_BUILD_TYPE=Release
make -j4
```

产物: `build_64/libMNN.so`,然后复制到 `app/src/main/jniLibs/arm64-v8a/`。

### 性能基准

```bash
# Neo 11 实测:~13.86 tok/s
adb shell "cd /data/local/tmp && LD_LIBRARY_PATH=/data/local/tmp ./llm_bench \
  -m /data/local/tmp/mnn_models/qwen3-vl-2b/Qwen3-VL-2B-Instruct-MNN/config.json \
  -n 30"
```

## 已知问题 / TODO

- [ ] AndroidManifest 缺 mipmap 图标(用了系统默认 sym_def_app_icon)
- [ ] SignScanActivity 用图库选择照片代替相机(MVP 简化)
- [ ] mock 数据 6 个商家,真实数据接入需后端
- [ ] Compose 滚动没显式 enable(加 verticalScroll)