# 干饭省省 · GanFanShengSheng

端侧 AI 干饭比价 App —— 扫一下餐厅招牌，1 秒比价 5 个平台，全程离线不联网。

> 🏆 端侧 AI 创新挑战赛（MNN + Qwen 赛道）参赛作品

## ✨ 功能特性

- 📸 **招牌识别**：对准餐厅招牌拍一张照，端侧 VLM 直接识别品牌
- 💰 **五平台比价**：抖音团购、美团、美团外卖、淘宝闪购、京东秒送
- 📍 **GPS 自动匹配**：根据位置自动选最近的分店
- 🔗 **一键跳转**：deeplink 直达官方 App 买券
- 🔒 **完全离线**：招牌照片不上云，飞行模式也能用
- ⚡ **秒省页**：饭点限时秒杀
- 👥 **省圈页**：本地干饭群拼单拼团

## 🛠 技术栈

| 层级 | 技术 |
|------|------|
| 端侧推理 | MNN + Qwen3-VL-2B-Instruct（INT4 量化） |
| 移动端 | Android + Jetpack Compose |
| 原生层 | C++ / JNI / CMake |
| 网络 | OkHttp（高德 POI） |
| 定位 | Android Location (GPS) |

## 📁 项目结构

```
m-qwen/
├── android-app/ScanDeals/   # Android App 源码
│   ├── app/src/main/
│   │   ├── java/com/mqwen/scandeals/   # Kotlin 源码
│   │   ├── cpp/                         # C++ JNI + MNN 推理
│   │   └── res/                         # 资源
│   └── build.gradle
├── docs/                      # 文档
│   ├── creative-plan.md       # 创意方案
│   ├── development-plan.md    # 开发计划
│   ├── data-strategy.md       # 数据策略
│   ├── model-download.md      # 模型下载指南
│   └── xiaohongshu-post.md    # 发布文案
├── tools/                     # 本地工具链（SDK/NDK/JDK，git 已忽略）
├── models/                    # 模型文件（git 已忽略，从魔搭下载）
├── logs/                      # 截图与产物（git 已忽略）
└── *.py / *.ps1               # 辅助脚本
```

## 🚀 快速开始

### 1. 下载模型

参考 [docs/model-download.md](docs/model-download.md)，从 ModelScope 下载 Qwen3-VL-2B-Instruct-MNN 模型到 `models/` 目录。

### 2. 配置环境

在 `android-app/ScanDeals/local.properties` 中配置：

```properties
sdk.dir=你的AndroidSDK路径
amap.key=你的高德地图Web服务Key  # 可选，不配置则走 Mock 数据
```

### 3. 构建项目

```bash
cd android-app/ScanDeals
./gradlew assembleDebug
```

生成的 APK 在 `android-app/ScanDeals/app/build/outputs/apk/debug/`。

## 📊 性能数据

- **机型**：骁龙 8 Gen 2 及以上
- **推理速度**：飞行模式下 ~13.86 tok/s
- **模型大小**：~1.5GB（INT4 量化）
- **识别耗时**：招牌识别 1-2 秒

## 🔐 隐私说明

- 招牌照片 **100% 本地处理**，不上传任何服务器
- 联网模式下仅请求高德 POI 公开数据（店名+地址+评分）
- 不收集任何个人信息

## 📝 License

本项目为参赛作品，开源协议待补充。

## 🔗 相关链接

- [ModelScope - Qwen3-VL-2B-Instruct-MNN](https://www.modelscope.cn/models/MNN/Qwen3-VL-2B-Instruct-MNN)
- [MNN 端侧推理框架](https://github.com/alibaba/MNN)
