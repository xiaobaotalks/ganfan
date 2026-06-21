# Phase 0 子任务清单:1~2 天开干版

> 对应主计划文档 `development-plan.md` 的 Phase 0
> 单人 1~2 天可完成,按时间块组织,做完一个勾一个
> 验收标准:**能在 PC 端用 Qwen2-VL-2B 识别一张餐厅招牌并输出 JSON 商家信息**——这意味着后面所有环节的基础打通了

---

## Day 1(共 8~10 小时)

### 🌅 上午 0:设备确认(15min,Day 1 第一件事)

你的设备是 **iQOO Neo 11(骁龙 8 Elite,16G+1T)**,完美目标机型。开机后先验证:

```bash
adb devices                                # 应该看到 SM8750 或类似设备
adb shell getprop ro.boot.hardware.platform  # 应是 SM8750 / Lahaina 等
adb shell getprop ro.product.cpu.abilist     # 应有 arm64-v8a
```

**装机必备**:
- [ ] Snapdragon Profiler(Google Play 搜"Snapdragon Profiler",Adreno GPU Profiler 工具,用于抓 GPU kernel)
- [ ] 装到手机的"开发者选项"打开 → 启用 GPU 渲染分析(`adb shell setprop debug.hwui.profile true`)
- [ ] 准备一个能产生持续负载的测试 App(用 Settings 或自己写一个空白 OpenGL App),验证 adb 能抓到 systrace

### 🌅 上午 1:阿里云账号 + DashScope(1.5h)

#### 任务 1.1:注册并实名阿里云
- 打开 https://cn.aliyun.com/,注册账号
- 完成个人/企业实名认证(扫脸或支付宝,5 分钟)
- **验收**:能登录阿里云控制台首页

#### 任务 1.2:开通 DashScope(通义千问)
- 控制台搜"百炼"或直接进 https://dashscope.aliyun.com/
- 开通服务 → 领免费额度(新用户一般有 100 万 token)
- 创建 API Key,**复制保存到密码管理器**(只显示一次!)
- **验收**:用 curl 调通一次 `qwen-turbo`,返回非空

```bash
curl -X POST https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions \
  -H "Authorization: Bearer $DASHSCOPE_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen-turbo",
    "messages": [{"role":"user","content":"你好"}]
  }'
```

#### 任务 1.3:开通对象存储 OSS + 配 Bucket(45min,📦 模型分发基础设施)

**Step A:领免费资源包(5min)**
- 进 https://free.aliyun.com/?product=1358,1359
- 找到"对象存储 OSS - 标准-同城冗余存储包"卡片
- 规格:**20GB 存储 + 2GB 下行流量 + 20 万次请求 / 3 个月 / 个人新用户免费**
- 点"立即试用" → 完成 0 元下单
- **注意**:与"本地冗余存储"二选一,不可同时领

**Step B:创建 Bucket(10min)**
- 控制台 → 对象存储 OSS → Bucket 列表 → 创建 Bucket
  - **名称**:`qwen-deal-finder-models`(全网唯一,如果冲突就加随机后缀,如 `-2026`)
  - **区域**:**华东1(杭州)**(离你近,下载快;后续改也方便)
  - **存储类型**:**标准存储**
  - **冗余类型**:**同城冗余**(已经领了同城冗余资源包,这里必须选同城)
  - **读写权限**:**公共读**(App 端匿名下载,免鉴权)
  - **版本控制**:**关**
  - **加密**:**关**(免费档)
  - **实时日志查询**:**关**
  - **生命周期规则**:**关**(避免自动转归档,反而扣费)

**Step C:关停扣费陷阱(5min)**
- 控制台 → 费用中心 → 预警设置 → **关闭余额预警短信**(避免半夜短信炸弹)
- 控制台 → 对象存储 OSS → Bucket 列表 → 你刚创建的 Bucket → 域名管理
  - 确认**传输加速**:关
  - 确认**跨区复制**:关
  - 确认**镜像回源**:关
  - 确认**静态网站托管**:关

**Step D:安装 ossutil 命令行工具(5min)**
```bash
# Windows 下载(挑一个方式)
# 方式 1:Chocolatey(推荐,装了的话)
choco install ossutil

# 方式 2:手动下载
# https://help.aliyun.com/document_detail/120075.html
# 解压到 C:\Program Files\ossutil\,加到 PATH
```

**Step E:配置 ossutil(3min)**
```bash
ossutil config
# 会问 4 件事:
# endpoint: oss-cn-hangzhou.aliyuncs.com
# accessKeyID: <从阿里云控制台 RAM 访问控制拿>
# accessKeySecret: <同上>
# stsToken: 留空,直接回车
```

**Step F:测试上传 + 公网访问(10min)**
```bash
# 1. 上传一张测试图
ossutil cp ./test-signboard.jpg oss://qwen-deal-finder-models/test/test-signboard.jpg

# 2. 列目录确认
ossutil ls oss://qwen-deal-finder-models/test/

# 3. 公网访问验证(关键!必须 200 不是 403)
curl -I "https://qwen-deal-finder-models.oss-cn-hangzhou.aliyuncs.com/test/test-signboard.jpg"
# 期望返回:HTTP/1.1 200 OK
```

**验收**:
- [ ] Bucket 创建成功,同城冗余
- [ ] ossutil 配置可用
- [ ] 测试图上传成功
- [ ] 公网 URL 返回 200(非常重要,403 就 App 端拉不到模型)
- [ ] 流量预警关闭
- [ ] 传输加速/跨区复制/镜像回源 都已关

**省钱小贴士**:
- 录视频时模型演示用 `adb push` 推到手机,不走 OSS 下行(2GB 流量留给真实内测)
- 决赛规模大时再换付费资源包(¥9/月级别)
- 3 个月到期前 1 周检查用量,主动决定是续费还是删 Bucket

---

### 🌞 下午 2:Android 开发环境(1.5h)

#### 任务 2.1:安装 Android Studio
- 下载 https://developer.android.com/studio(选 Ladybug | 2024.2.1+)
- 首次启动:Standard 安装,SDK 选 34,NDK 选 26.1+,CMake 3.22+
- **验收**:能创建一个空 Activity 并在模拟器跑起来

#### 任务 2.2:配置真机调试
- 手机开发者模式:`设置 → 关于手机 → 连续点 7 下版本号`
- USB 调试:`开发者选项 → USB 调试`
- 连电脑,授权 RSA 指纹
- **验收**:终端 `adb devices` 能看到你的设备

#### 任务 2.3:Git 仓库
- GitHub / Gitee 创建一个私有仓库 `qwen-deal-finder`(或你喜欢的名字)
- 本地:
  ```bash
  cd D:\C-AI\m-code\m-qwen
  git init
  git remote add origin <your-repo-url>
  # 用 Android Studio 创建一个空项目,提交
  git add .
  git commit -m "chore: initial android project"
  git push -u origin main
  ```
- 分支策略: `main` + `feat/*`(功能) + `fix/*`(修复)
- **验收**:能在 GitHub 看到初始 commit

---

### 🌙 晚上 3:基于 MNN Chat 二次开发(3h)🏆 **关键路径**

> 这一步完成 = Phase 0 直接 50% 完成,后面 Phase 1 也能压到 3 天。

#### 任务 3.0:检查 ModelScope 是否有 Qwen2-VL-2B-MNN 预转换版(0.5h)
- 打开 https://modelscope.cn/organization/MNN
- 搜索 `Qwen2-VL-2B` 或 `qwen2-vl`
- **如果有**:直接下载,记下路径,跳到任务 3.4
- **如果没有**:记下来,Day 2 上午自己 `llmexport.py` 转

#### 任务 3.1:Fork MNN 仓库
```bash
# 在 GitHub 上 fork alibaba/MNN
# 然后本地 clone 你的 fork
git clone https://github.com/<your-name>/MNN.git
cd MNN
git remote add upstream https://github.com/alibaba/MNN.git
git checkout 2.9.0  # 或更新稳定版
```

#### 任务 3.2:编译 MNN Chat Android 工程
- 进入 `apps/Android/MnnChat/`
- 用 Android Studio 打开这个工程
- 等待 Gradle 同步 + 自动下载依赖
- Build → Make Project(首次会编译 MNN `.so`,可能要 10-20 分钟)
- **关键参数确认**:在 `build.gradle` 里找 `MNN_BUILD_LLM`、`MNN_OPENCL` 是否开启(默认应该开了)
- **验收**:能 Run 到 iQOO Neo 11,App 启动到"模型市场"页面

#### 任务 3.3:从 ModelScope 拉一个能跑的 MNN 模型到手机测试
- 打开 MNN Chat APP → 模型市场 → 找一个现成的小模型(比如 Qwen3-0.6B-MNN)下载测试
- 验证对话能跑通
- **验收**:APP 完整跑通拍照/选图 → 多模态问答流程
- **意义**:确认 fork 的工程在自己机器上完整可用,后面替换模型 + 改业务就是水到渠成

#### 任务 3.4:备份一份当前可用的 MNN Chat APK
```bash
adb shell pm list packages | grep mnnchat
adb pull /data/app/.../base.apk ./backup-mnnchat.apk
```
万一后面改了崩了,有 backup 能快速回滚

> ⚠️ **卡点预案**:
> - Gradle 同步失败:检查代理 + Maven 仓库配置(用阿里云镜像)
> - MNN 编译失败:检查 NDK 版本(26.1+)和 CMake 版本(3.22+)
> - App 装上但加载模型崩溃:先用一个官方预下载的小模型验证基础链路

#### 备选路线(若 Phase 0 晚上时间不够)
- 直接从 https://github.com/alibaba/MNN/releases 下载预编译的 `.so`
- 用现成的 APK 加新功能(虽然不能改源码,但至少能跑通基础链路)
- 这样至少 Phase 0 验收能过,后面再补编译

---

## Day 2(共 6~8 小时)

### 🌅 上午 4:PaddleOCR MNN 模型(1.5h)

#### 任务 4.1:下载 PaddleOCR Mobile MNN 模型
- 进 https://modelscope.cn/,搜 `paddleocr mobile mnn`
- 下载文件:
  - `ch_PP-OCRv4_det_infer.mnn`(检测)
  - `ch_PP-OCRv4_rec_infer.mnn`(识别)
  - `ppocr_keys_v1.txt`(字符集)
- 存到 `assets/models/ocr/` 目录
- **验收**:三个文件齐全,共约 10MB

#### 任务 4.2:PC 端 OCR 跑通(命令行)
- MNN 自带 demo 或 PaddleOCR-MNN 仓库提供 `ocr_demo`
- 拿一张餐厅招牌图测试,输出文字 + 坐标
- **验收**:命令行跑出中文文字,坐标正确

```bash
./ocr_demo \
  --det_model ch_PP-OCRv4_det_infer.mnn \
  --rec_model ch_PP-OCRv4_rec_infer.mnn \
  --keys ppocr_keys_v1.txt \
  --image test_signboard.jpg
```

---

### 🌞 上午 4:Qwen2-VL-2B MNN 模型准备(1.5h,🏆 比赛核心)

> 决策:Day 1 晚上任务 3.0 已经查过有没有预转换版。
> - ✅ 有 → 跳到 4.3
> - ❌ 没有 → 走 4.1+4.2 转换

#### 任务 4.1(若无预转换版):下载原始模型
- 进 https://modelscope.cn/models/qwen/Qwen2-VL-2B-Instruct
- 下载完整模型文件夹(~4GB,FP16)
- **验收**:本地有 `Qwen2-VL-2B-Instruct/` 目录,含 `config.json` 和权重

#### 任务 4.2(若无预转换版):转 MNN 格式 + INT4 量化
- 装 Python 依赖:`pip install transformers torch mnn`
- 用 MNN-LLM 转换脚本(参考文章里强调的 `-hqq` 选项也加上):
  ```bash
  cd MNN/transformers/llmexport
  python llmexport.py \
    --path /path/to/Qwen2-VL-2B-Instruct \
    --export mnn \
    --quant_bit 4 \
    --quant_block 32 \
    -hqq \
    --lm_quant_bit 4 \
    --vlm  # 关键参数,多模态
  ```
- **验收**:生成 `qwen2-vl-2b-mnn/` 目录,含 MNN 权重 + config
- 预期产物大小:~1.2GB

> ⚠️ **卡点预案 1**:若 `--vlm` 参数报错,说明 MNN 版本不够新,升级到 2.9.0+ 或用 GitHub main 分支最新代码
> ⚠️ **卡点预案 2**:若 2B 转换后端侧仍跑不动,降级到 `Qwen3-0.6B` + 独立 OCR 端到端(精度差但能跑)

#### 任务 4.3:`adb push` 模型到手机
- MNN Chat 默认从以下目录找模型(看代码确认,可能是 `/sdcard/MNN/models/` 或 `getExternalFilesDir()`)
- 把 `qwen2-vl-2b-mnn/` 文件夹整个 push 进去:
  ```bash
  adb push qwen2-vl-2b-mnn /sdcard/MNN/models/
  ```
- **验收**:在 MNN Chat APP 的"我的模型"里能看到

#### 任务 4.4(可选):PaddleOCR 准备
> PoC 阶段可以先不用 OCR(用 Qwen2-VL-2B 看图就行),但准备着以防万一。
- 进 https://modelscope.cn/,搜 `paddleocr mobile mnn`
- 下载 `ch_PP-OCRv4_det_infer.mnn` + `ch_PP-OCRv4_rec_infer.mnn` + `ppocr_keys_v1.txt`
- 存到 `assets/models/ocr/`(先不集成,备用)

---

### 🌞 下午 5:在 MNN Chat 里跑通招牌识别(2h,🏆 第一次见曙光)

#### 任务 5.1:替换默认模型
- 在 MNN Chat APP 里选"我的模型" → 选你 push 进去的 Qwen2-VL-2B-MNN
- 或者直接修改 MNN Chat 代码,让默认加载我们的模型

#### 任务 5.2:替换 prompt
- 在 `MainActivity.kt`(或类似)里找到默认 prompt
- 替换为招牌识别专用 prompt:
  ```
  你是一个餐厅招牌识别助手。请仔细看这张图片,识别出餐厅信息,以 JSON 格式输出:
  {
    "店名": "string",
    "菜系": "string",
    "招牌文字": ["string", "string"],
    "招牌颜色": "string",
    "装修风格": "string",
    "置信度": 0.0-1.0
  }
  只输出 JSON,不要其他内容。
  ```

#### 任务 5.3:跑一张招牌图
- 准备 3-5 张测试招牌(网上找清晰大图,瑞幸/海底捞/星巴克这种)
- 在 MNN Chat 里上传图片 + 提问
- 验证输出是合法 JSON,店名识别对
- **验收**:模型输出结构化 JSON,店名识别正确

#### 任务 5.4(可选):上传模型到 OSS 备用
- 把 `qwen2-vl-2b-mnn/` 整个文件夹传到阿里云 OSS(任务 1.3 创建的 Bucket)
- 生成签名 URL(临时或永久,看你安全策略)
- **意义**:决赛阶段要做"模型按需下载"时,这里已有现成的源
- **验收**:能从公网 URL 下载到这个文件夹的 zip

---

### 🌙 晚上 6:联通性验证 + 收尾(1h)

#### 任务 6.1:Phase 0 总结演练
- 拿一张餐厅招牌图(网上找一张清晰大图)
- 跑通链路:在 MNN Chat 里 → 选我们的模型 → 上传招牌图 → 提问
- 确认输出 JSON 包含 {店名, 菜系, 招牌文字, 置信度}
- 在 main 分支提一个 `chore: phase 0 complete` 合并
- **验收**:有清晰的输出 + 截图存档(为后面 Phase 6 DEMO 视频留素材)

#### 任务 6.2:验证 MNN Arm SME2 是否在 iQOO Neo 11 上生效(🏆 关键证据)

这是创意文档和 DEMO 视频的核心技术素材,Day 2 必做。

```bash
# 1. 编译 MNN 的 llm_bench 工具(若未编译)
# 在 MNN 仓库根目录
cd MNN
mkdir -p build && cd build
cmake -DMNN_BUILD_LLM=ON -DMNN_BUILD_BENCHMARK=ON -DMNN_SME2=ON -DMNN_ARM82=ON ..
make llm_bench -j$(nproc)

# 2. 推到手机
adb push llm_bench /data/local/tmp/
adb shell chmod +x /data/local/tmp/llm_bench

# 3. 跑 benchmark(模型路径用你 push 进手机的)
adb shell /data/local/tmp/llm_bench -m /sdcard/Download/qwen2-vl-2b-mnn/config.json
```

**观察输出**:
- `Backend: SME2` ✅ → SME2 生效,采集 Prefill 阶段 token/s 数据,这就是 DEMO 视频的核心素材
- `Backend: ARM82` / `i8mm` → 自动回退,正常,讲"MNN 跨设备兼容"故事
- `Backend: NEON` → 回退到更老的指令集,仍然能跑

**截图存档**(为 Phase 6 视频准备):
- 完整 benchmark 输出(含 backend 字段、Prefill/Decode token/s)
- 关键数据记到 `docs/phase0-acceptance.md` 里

#### 任务 6.3:写 Phase 0 验收报告
在 `docs/phase0-acceptance.md` 写一份简短的报告,包含:
- MNN 模型的本地路径 / OSS URL
- 一张成功识别的招牌截图
- MNN Chat APK 安装包路径
- **`llm_bench` 输出关键数据**(SME2 是否生效、Prefill token/s)
- 环境配置摘要(Android Studio 版本、NDK 版本、MNN commit)

---

## 验收检查表(Day 2 结束前自检)

- [ ] 阿里云账号 + DashScope API Key 已保存
- [ ] OSS Bucket 已创建,Qwen-VL 模型已上传
- [ ] Android Studio + NDK + 真机调试 OK
- [ ] Git 仓库已建立,初始 commit 已推
- [ ] MNN 在 PC 端能编译,`llm_demo --help` 正常
- [ ] MNN Android `.so` 能成功加载
- [ ] PaddleOCR Mobile MNN 模型已下载,PC 端能跑出中文文字
- [ ] Qwen2-VL-2B MNN INT4 已转换完成,PC 端能识别招牌输出 JSON
- [ ] 文档 `docs/phase0-acceptance.md` 已写

**全部勾完 = Phase 0 完成,可以进入 Phase 1**

---

## 必备工具清单(Day 1 前装好)

| 工具 | 用途 |
|---|---|
| Android Studio Ladybug+ | Android 开发 |
| Git for Windows | 代码管理 |
| Visual Studio 2022 Community(选 C++ 桌面开发 + CMake) | MNN 编译 |
| Python 3.10+ | 模型转换 |
| 7-Zip / WinRAR | 模型文件解压 |
| 一个能翻墙的稳定网络 | 拉 GitHub / 下载模型 |
| 备用梯子 | DashScope 海外模型 endpoint |

---

## 遇到问题怎么排查

| 症状 | 优先排查 |
|---|---|
| MNN 编译报 NDK 错 | 升级 NDK 到 26.1+;检查 `local.properties` 路径 |
| MNN 转换 VLM 报 shape 错 | 检查 PyTorch 版本(要 2.1+);降级 transformers 到 4.40 |
| PaddleOCR 端侧识别乱码 | 字符集文件路径错;模型文件损坏(重新下) |
| Qwen2-VL-2B 转换 OOM | 至少 16GB 内存;关其他应用 |
| adb devices 找不到手机 | 换 USB 线(数据线,非充电线);装手机驱动 |
| DashScope API 401 | API Key 复制错了;环境变量没 export |

---

> **Phase 0 完成后**,在群里 @我一下,我把 Phase 1(端侧 OCR 模块)也拆成日级任务清单,你就可以无缝衔接了。
