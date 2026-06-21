# 模型与依赖库下载指南

本项目使用 **Qwen3-VL-2B-Instruct-MNN** 端侧多模态模型，需从 ModelScope（魔搭）下载后放入 `models/` 目录。

## 一、模型下载

### 模型信息

- **模型名称**：Qwen3-VL-2B-Instruct-MNN
- **模型来源**：[ModelScope MNN/Qwen3-VL-2B-Instruct-MNN](https://www.modelscope.cn/models/MNN/Qwen3-VL-2B-Instruct-MNN)
- **量化方式**：INT4
- **所需文件**（共 12 个）：
  - `config.json`
  - `llm.mnn` + `llm.mnn.weight`（~1.2GB）
  - `visual.mnn` + `visual.mnn.weight`（~228MB）
  - `tokenizer.txt`
  - `qwen.tiktoken`
  - `slice_0.vocab` ~ `slice_7.vocab`

### 下载方式

#### 方式一：ModelScope CLI（推荐）

```bash
pip install modelscope
modelscope download --model MNN/Qwen3-VL-2B-Instruct-MNN --local_dir ./models/Qwen3-VL-2B-Instruct-MNN
```

#### 方式二：网页下载

访问 https://www.modelscope.cn/models/MNN/Qwen3-VL-2B-Instruct-MNN/files 手动下载所有文件，放到 `models/Qwen3-VL-2B-Instruct-MNN/` 目录。

### 放置目录

```
m-qwen/
└── models/
    └── Qwen3-VL-2B-Instruct-MNN/
        ├── config.json
        ├── llm.mnn
        ├── llm.mnn.weight
        ├── visual.mnn
        ├── visual.mnn.weight
        ├── tokenizer.txt
        ├── qwen.tiktoken
        ├── slice_0.vocab
        ├── ...
        └── slice_7.vocab
```

> 注意：`models/` 目录已加入 `.gitignore`，不会被提交

## 二、MNN 运行库下载

Android 端需要 MNN 动态库（`libMNN.so`）才能运行。可从 MNN 官方发布页下载：

- **下载地址**：[MNN GitHub Releases](https://github.com/alibaba/MNN/releases)
- **所需版本**：最新的 Android arm64-v8a 版本
- **放置位置**：`android-app/ScanDeals/app/src/main/jniLibs/arm64-v8a/libMNN.so`

### 下载步骤

1. 访问 https://github.com/alibaba/MNN/releases
2. 下载最新版本的 `MNN-*-Android-aarch64.zip`（或类似名称）
3. 解压后找到 `libMNN.so`
4. 复制到 `android-app/ScanDeals/app/src/main/jniLibs/arm64-v8a/` 目录下

> 注意：所有 `.so` 文件已加入 `.gitignore`，不会被提交