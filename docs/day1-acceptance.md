# Day 1 验收报告

## 完成时间
- 启动: 2026-06-13 21:19
- Day 1 实际跨度: 2026-06-13 ~ 2026-06-14(跨越 2 个会话,期间 daemon 重启 3 次)

## ✅ 完成项

### 后台并行(2026-06-13 完成)
- [x] **ModelScope 1.37.1** 安装(阿里云 PyPI 镜像)
- [x] **Qwen3-VL-2B-Instruct-MNN 模型 1.4GB** 下载完成
  - 路径: `D:\C-AI\m-code\m-qwen\models\Qwen3-VL-2B-Instruct-MNN\MNN\Qwen3-VL-2B-Instruct-MNN\`
  - 12 个文件齐全: config.json / llm.mnn+weight(1.2GB) / visual.mnn+weight(228MB) / tokenizer.txt
- [x] **MNN 源码 326MB** 浅克隆(Gitee 镜像 gitee.com/mirrors/MNN)
  - 路径: `D:\C-AI\m-code\m-qwen\MNN\`
  - 关键子目录: `project/android/`、`transformers/llm/`、`CMakeLists.txt`
  - 关键脚本: `project/android/build_64.sh`

### 工具链(2026-06-14 完成)
- [x] **JDK 21**(从 download.java.net 下 zip,解压到 `D:\C-AI\m-code\m-qwen\tools\jdk-21\`)
- [x] **Android Studio Ladybug 2024.2.2** 全机安装到 `C:\Program Files\Android\Android Studio\`
  - 自带 JBR (JDK 21.0.4) 已就位
- [x] **Android cmdline-tools 146.5MB** 解压到 `D:\C-AI\m-code\m-qwen\tools\cmdline-tools\`
- [x] **Android SDK 全套**(sdkmanager 静默安装):
  - platform-tools (adb 1.0.41) — `D:\C-AI\m-code\m-qwen\tools\android-sdk\platform-tools\`
  - build-tools;34.0.0 (aapt2 OK) — `...\build-tools\34.0.0\`
  - ndk;27.0.12077973 (clang OK) — `...\ndk\27.0.12077973\`
  - cmake;3.22.1 — `...\cmake\3.22.1\bin\cmake.exe`
  - platforms;android-34 — `...\platforms\android-34\`
- [x] **license 全部接受**(All SDK package licenses accepted)

### 工程初始化
- [x] Git init `D:\C-AI\m-code\m-qwen\.git`
- [x] `.gitignore` 创建(排除 MNN/、tools/、build/、APK 等)

## ⏳ 待完成项(Day 2 必做)

### 必做
- [ ] **创建 Android Studio Native C++ 工程**(用 Studio 启动 New Project → Native C++ 模板)
- [ ] 把 SDK 路径配到工程 `local.properties`:
  ```
  sdk.dir=D\:\\C-AI\\m-code\\m-qwen\\tools\\android-sdk
  ndk.dir=D\:\\C-AI\\m-code\\m-qwen\\tools\\android-sdk\\ndk\\27.0.12077973
  ```
- [ ] iQOO Neo 11 设备 adb 联调(`adb devices` 验证)
- [ ] 编译 MNN `libMNN.so`:
  ```bash
  cd D:\C-AI\m-code\m-qwen\MNN\project\android
  # Windows 没 bash,用 Git Bash 或 WSL 跑 build_64.sh
  # 或手动 cmake
  ```
- [ ] adb push 模型到手机 `/data/local/tmp/mnn_models/qwen3-vl-2b/`
- [ ] 跑 `llm_demo` 命令行验证
- [ ] 跑 `llm_bench` + `adb logcat | grep "device supports"` 采集 SME2 数据

### 阿里云账号(用户自己)
- [ ] 拿 DashScope API Key
- [ ] 创建 OSS Bucket(走 phase0-tasks.md 任务 1.3 的 A-F 步骤)

## 环境变量参考(本机值)

```bash
# 写到 PowerShell profile $PROFILE 或直接 export
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"   # 优先用这个(Studio 自带)
# 备选:$env:JAVA_HOME = "D:\C-AI\m-code\m-qwen\tools\jdk-21"
$env:ANDROID_HOME = "D:\C-AI\m-code\m-qwen\tools\android-sdk"
$env:ANDROID_NDK_HOME = "$env:ANDROID_HOME\ndk\27.0.12077973"
$env:Path += ";$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin"
```

## 风险与备注
- **网络**: GitHub 直连不稳(已用 Gitee 镜像 + 国内 CDN)
- **会话不稳定**: Day 1 期间 daemon 重启 3 次,后续如果用长命令,**单次 timeout ≤ 10 分钟** 避免再断
- **NDK 27**: 教程 1 要求的最低版本,已就位
- **JDK 21**: Android Gradle Plugin 8.5+ 兼容,Studio 自带 JBR 21.0.4 验证可用

## Day 2 计划(2026-06-15)
1. 用 Studio 创建 Native C++ 工程(15min)
2. 编译 MNN libMNN.so(30min,可能更长因为 Windows 编译 MNN 比较慢)
3. adb push + 跑 llm_demo 验证(15min)
4. 跑 llm_bench + 采集 SME2 数据(15min)
