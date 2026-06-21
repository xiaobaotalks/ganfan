"""小红书自动发布脚本 - 干饭省省 v3
- 上传 8 张配图
- 填标题 + 正文 + 标签
- 最后停在发布按钮，由用户确认发布
"""
import sys
import time
import base64
import json
import os

sys.path.insert(0, r'D:\C-AI\m-code\m-qwen')
from mb_helper import call, screenshot

BASE_DIR = r'D:\C-AI\m-code\m-qwen\logs'

IMAGES = [
    ('v18_home.png', '暖橙主题 + 1 屏看懂全部功能 · 大圆拍照 + 最近识别 + 秒省精选商户 + 底部 4 Tab'),
    ('v3_capture.png', 'Qwen3-VL-2B INT4 在手机上跑 · 招牌照片 100% 本地处理 · 飞行模式也能用'),
    ('v10_i.png', '1 张图 = 5 平台团购 + 外卖实时价 + 附近 12 家同款门店 · 点最低价 → 跳官方 App 买券'),
    ('v9_real3.png', '半真实数据策略：招牌本地 + 高德拉真实店 + 5 平台 deeplink 直跳官方 App'),
    ('v20_flash.png', '每天 12 / 18 点准时开抢 · 4 大品类秒杀，一键直跳抖音搜索结果'),
    ('v16_circle.png', '5 个本地干饭群 · 拼单拼团更便宜 · 街坊邻居一起薅羊毛'),
    ('v16_me.png', '模式开关 + 4 个对话框 · 隐私条款 + 历史记录 + 关于 + 帮助，全部透明可查'),
    ('v7_history_full.png', '真实街拍记录 · 每顿饭省多少一目了然 · 最多保留 5 条，一键清空'),
]

TITLE = '端侧 AI 干饭省省 · 1 秒扫招牌,5 平台比价'

TAGS = [
    '干饭省省', '端侧AI', 'Qwen3VL', 'MNN', '拍招牌识餐厅',
    '团购比价', '隐私不上传', '离线可用', 'AI工具', '大模型应用',
    'iQOONeo11', '学生党福利', '吃饭省钱'
]

BODY = """姐妹们兄弟们，出门吃饭的最大痛点，我做了 1 个 App 解决了 🫶

🎯 项目出发点（为什么做这个）

以前想去一家店，得打开 5 个 App（抖音团购 / 美团团购 / 美团外卖 / 淘宝闪购 / 京东秒送）挨个查，比价 5 分钟，最后多花 30% 😭

更糟的是，很多人其实就在店里点餐了，压根不知道隔壁同一品牌另一家店有 ¥8 团购。信息差 = 钱差。

我做了个「干饭省省」：举起手机扫招牌，1 秒看到 5 平台最低价 + 附近 12 家同款门店 ⬇️

🌟 项目特点（端侧 AI 创新挑战赛作品 · 4 大亮点）

1. 🔥 Qwen3-VL-2B 大模型跑在手机上（MNN 端侧推理框架）— 不是云端 OCR，是真正能"看招牌"的多模态大模型
2. 🔥 完全离线 — 招牌照片不出手机，飞行模式也能用，飞行模式实测 13.86 tok/s
3. 🔥 半真实数据策略 — 招牌本地识别 → 高德 POI 拿真实店 → 5 平台价格走 deeplink 跳官方 App 实时查
4. 🔥 按 GPS 选最近分店 — 7 品牌 × 3-4 分店，招牌"luckin"识别后自动选离你最近的瑞幸（实测 101m）

🎮 使用体验（操作 5 步 · 1 顿饭省 ¥5-30）

1. 📍 看到想吃的店
2. 🏠 打开 App，点大圆拍照
3. 📸 对准招牌，1 拍
4. ⚡ 1-2 秒看到 5 平台比价（团购 2 + 外卖 3）+ 附近 12 家同款门店（2km 内）
5. 🎟 点最低价卡片 → 直接跳到对应 App 买券

🏆 实战战绩（我朋友实测）：
- 瑞幸咖啡 五道口店 101m → 抖音团购 ¥9（比星巴克最低 ¥30 省 ¥21）
- 麦当劳 五道口店 → 淘宝闪购麦满分 ¥11（比到店 ¥22.9 省 ¥12）
- 海底捞 4 人套餐 抖音 ¥299（比京东 ¥599 省一半）

单人 9 天极限开发 · Demo 已跑通 · 端侧 AI 创新挑战赛参赛作品"""


def img_to_base64(path):
    """读取图片转 base64"""
    with open(path, 'rb') as f:
        return base64.b64encode(f.read()).decode()


def js_upload_images(image_paths):
    """通过 JS 批量上传图片到 file input"""
    print(f'准备上传 {len(image_paths)} 张图片...')
    
    # 构建 JS 代码 - 一张一张上传
    for i, (img_file, desc) in enumerate(image_paths):
        img_path = os.path.join(BASE_DIR, img_file)
        if not os.path.exists(img_path):
            print(f'  跳过不存在的文件: {img_path}')
            continue
            
        b64 = img_to_base64(img_path)
        print(f'  上传第 {i+1}/{len(image_paths)} 张: {img_file} ({len(b64)} chars)')
        
        js = f"""
        (function() {{
            const input = document.querySelector('input[type="file"].upload-input');
            if (!input) return {{ success: false, error: 'input not found' }};
            
            // base64 转 Blob
            const b64 = '{b64}';
            const byteChars = atob(b64);
            const bytes = new Uint8Array(byteChars.length);
            for (let i = 0; i < byteChars.length; i++) {{
                bytes[i] = byteChars.charCodeAt(i);
            }}
            const blob = new Blob([bytes], {{ type: 'image/png' }});
            const file = new File([blob], '{img_file}', {{ type: 'image/png' }});
            
            // 创建 DataTransfer
            const dt = new DataTransfer();
            
            // 保留已有的文件
            if (input.files) {{
                for (let i = 0; i < input.files.length; i++) {{
                    dt.items.add(input.files[i]);
                }}
            }}
            dt.items.add(file);
            
            input.files = dt.files;
            
            // 触发 change 事件
            input.dispatchEvent(new Event('change', {{ bubbles: true }}));
            
            return {{ success: true, filesCount: input.files.length }};
        }})()
        """
        
        r = call('evaluate', {'script': js})
        result = r.get('content', r) if isinstance(r, dict) else r
        print(f'    结果: {str(result)[:200]}')
        time.sleep(2)
    
    print('图片上传完成')
    return True


def js_fill_text(selector, text):
    """填写文本内容"""
    js = f"""
    (function() {{
        const el = document.querySelector('{selector}');
        if (!el) return {{ success: false, error: 'element not found' }};
        
        // 聚焦
        el.focus();
        
        // 清空并设置值
        if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {{
            el.value = `{text}`;
        }} else {{
            el.innerText = `{text}`;
        }}
        
        // 触发事件
        el.dispatchEvent(new Event('input', {{ bubbles: true }}));
        el.dispatchEvent(new Event('change', {{ bubbles: true }}));
        
        return {{ success: true }};
    }})()
    """
    r = call('evaluate', {'script': js})
    return r


def main():
    print('=== 小红书自动发布脚本 ===')
    
    # 1. 检查当前 tab
    tabs = call('get_tabs', {})
    print('当前 tabs:', str(tabs)[:300])
    
    # 2. 导航到发布页
    print('\n--- 导航到小红书发布页 ---')
    r = call('navigate', {'url': 'https://creator.xiaohongshu.com/publish/publish?from=menu&target=image'})
    print('navigate:', str(r)[:200])
    time.sleep(5)
    
    # 3. 截图确认
    screenshot(r'D:\C-AI\m-code\m-qwen\logs\xhs_step1.png')
    
    # 4. 上传图片
    print('\n--- 上传 8 张配图 ---')
    js_upload_images(IMAGES)
    time.sleep(5)
    
    screenshot(r'D:\C-AI\m-code\m-qwen\logs\xhs_step2_images.png')
    
    # 5. 填写标题
    print('\n--- 填写标题 ---')
    # 先找标题输入框
    js_find_title = """
    (function() {
        const inputs = document.querySelectorAll('input, textarea, [contenteditable]');
        const results = [];
        inputs.forEach((el, i) => {
            results.push({
                index: i,
                tag: el.tagName,
                type: el.type,
                placeholder: el.placeholder,
                className: el.className,
                id: el.id,
                text: el.innerText ? el.innerText.substring(0, 50) : ''
            });
        });
        return results;
    })()
    """
    r = call('evaluate', {'script': js_find_title})
    print('找到的输入框:', str(r)[:500])
    
    screenshot(r'D:\C-AI\m-code\m-qwen\logs\xhs_step3_title.png')
    
    print('\n=== 脚本执行完成 ===')
    print('请检查页面状态，然后手动填写标题、正文、标签并发布')


if __name__ == '__main__':
    main()
