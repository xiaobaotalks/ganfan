"""
一键录制 3 段演示视频(干饭省省 · 端侧 AI 创新挑战赛)
- 段 1: 模型本地加载(飞行模式 + 我的页 + 模型文件夹 + logcat 加载 log)
- 段 2: 推理输入输出(拍照 + logcat token 输出)
- 段 3: 核心交互(开网络 + 5 平台比价 + deeplink 跳官方 App)
"""
import subprocess
import time
import os
import sys

ADB = r"D:\C-AI\m-code\m-qwen\tools\android-sdk\platform-tools\adb.exe"
PKG = "com.mqwen.scandeals"
DEMO_DIR = r"D:\C-AI\m-code\m-qwen\logs\demo"


def run(args, timeout=30, check=False):
    """跑 adb 命令"""
    r = subprocess.run([ADB] + args, capture_output=True, text=True, timeout=timeout)
    if check and r.returncode != 0:
        print(f'[ERR] {args} -> {r.stderr[:200]}', file=sys.stderr)
    return r


def shell(cmd_str, timeout=30):
    """run adb shell <string>"""
    return run(["shell", cmd_str], timeout=timeout)


def tap(x, y, label=""):
    print(f'  [tap] {label or ""} ({x},{y})')
    shell(f"input tap {x} {y}")


def back():
    shell("input keyevent KEYCODE_BACK")


def home():
    shell("input keyevent KEYCODE_HOME")


def screencap(path):
    shell(f"screencap -p /sdcard/{os.path.basename(path)}")
    run(["pull", f"/sdcard/{os.path.basename(path)}", path])


def launch_main():
    """启动 MainActivity (force-stop 先)"""
    shell(f"am force-stop {PKG}")
    time.sleep(1)
    shell(f"am start -n {PKG}/.MainActivity")
    time.sleep(3)


def open_url_via_deeplink():
    """模拟点击最低价卡片 → deeplink 跳抖音(段 3 末尾)"""
    # 段 3 末:点抖音团购卡片(估计位置),deeptest 跳抖音
    # 这里用"立即抢购"或者直接 start 抖音
    shell("am start -a android.intent.action.VIEW -d 'snssdk1234://'")
    time.sleep(2)
    back()


def start_screenrecord(out_path, duration_sec=300):
    """后台启动录屏 (默认 5 分钟)"""
    print(f'\n[录屏] 开始 -> {out_path} ({duration_sec}s)')
    p = subprocess.Popen(
        [ADB, "shell", "screenrecord",
         "--time-limit", str(duration_sec),
         "--bit-rate", "8000000",
         "--size", "1080x1920",
         "/sdcard/demo_raw.mp4"],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE,
    )
    return p


def stop_and_pull(recorder, out_path):
    """等录屏结束 + pull"""
    print('[录屏] 等待结束...')
    try:
        recorder.wait(timeout=600)
    except subprocess.TimeoutExpired:
        recorder.kill()
    time.sleep(2)
    run(["pull", "/sdcard/demo_raw.mp4", out_path])
    print(f'[录屏] 已保存 {out_path}')


def start_logcat_capture(out_path):
    """后台捕获 logcat 到文件(后期剪到视频里作辅助)"""
    f = open(out_path, 'wb')
    p = subprocess.Popen(
        [ADB, "logcat", "-v", "time"],
        stdout=f, stderr=subprocess.PIPE,
    )
    return p, f


def stop_logcat(p, f):
    p.terminate()
    try:
        p.wait(timeout=3)
    except subprocess.TimeoutExpired:
        p.kill()
    f.close()


def set_airplane(on=True):
    """切换飞行模式(0=关,1=开)"""
    val = "1" if on else "0"
    shell(f"settings put global airplane_mode_on {val}")
    shell(f"am broadcast -a android.intent.action.AIRPLANE_MODE --ez state {'true' if on else 'false'}")
    time.sleep(2)


def main():
    os.makedirs(DEMO_DIR, exist_ok=True)
    logcat_path = os.path.join(DEMO_DIR, "demo_logcat.txt")
    mp4_path = os.path.join(DEMO_DIR, "demo_raw.mp4")

    # 后台 logcat 捕获
    print('[prep] 启动 logcat 捕获...')
    log_proc, log_f = start_logcat_capture(logcat_path)

    # 后台录屏
    rec = start_screenrecord(mp4_path, duration_sec=300)

    time.sleep(2)

    # === 段 0: App 启动首页 ===
    print('\n=== 段 0: 启动 App (App 实际默认显示秒省,但我们手动切到首页) ===')
    launch_main()
    # 首页 tab: x=109, y=1812 (在安全区下沿)
    tap(109, 1812, "首页 Tab")
    time.sleep(1)
    # 滚到顶
    shell("input swipe 500 800 500 1500 300")
    time.sleep(1)

    # === 段 1: 模型本地加载 (飞行模式 + 我的页) ===
    print('\n=== 段 1: 飞行模式 + 模型本地加载 ===')
    set_airplane(on=True)
    time.sleep(1)
    # 启动我的页
    tap(763, 1812, "我的 Tab")
    time.sleep(2)
    # 滚到顶(我的页可能很长)
    shell("input swipe 500 800 500 1500 300")
    time.sleep(1)
    # 截图证明离线模式
    screencap(os.path.join(DEMO_DIR, "01_me_offline.png"))
    time.sleep(1)
    # 退出 App
    home()
    time.sleep(1)
    # 打开文件管理器看模型
    # 启动 iQOO 自带文件管理
    shell("am start -n com.android.fileexplorer/.FileExplorerActivity 2>/dev/null")
    time.sleep(2)
    # 退而求其次:用 App 看模型路径 (我用 dumpsys 拿到 app data dir)
    shell(f"run-as {PKG} ls -la /data/data/{PKG}/files/ 2>/dev/null")
    # 用 setprop 显示模型大小
    shell("ls -la /sdcard/Android/data/com.mqwen.scandeals/files/qwen3-vl-2b/ 2>/dev/null")
    time.sleep(2)
    back()
    time.sleep(1)

    # === 段 2: 推理输入输出(拍照 + logcat token 输出) ===
    print('\n=== 段 2: 推理输入输出 (飞行模式下识别) ===')
    launch_main()
    tap(109, 1812, "首页 Tab")
    time.sleep(1)
    # 点大圆拍照
    tap(437, 950, "大圆 CTA")
    time.sleep(3)
    # 拍照页里点快门(中心附近)
    tap(437, 1600, "快门")
    time.sleep(2)
    # 注入 debug image 走识别
    shell(f"am start -n {PKG}/.MainActivity --es debug_image /sdcard/Download/test_sign_luckin.jpg --ed debug_lat 39.9920 --ed debug_lng 116.3360")
    time.sleep(8)  # 等 VLM 推理 + 结果页加载
    screencap(os.path.join(DEMO_DIR, "02_result_offline.png"))
    time.sleep(1)
    back()
    back()
    time.sleep(1)

    # === 段 3: 核心交互流程(开网络 + 5 平台 + 跳转) ===
    print('\n=== 段 3: 核心交互 (联网 + 5 平台 + deeplink 跳官方 App) ===')
    set_airplane(on=False)
    time.sleep(2)
    launch_main()
    tap(109, 1812, "首页 Tab")
    time.sleep(1)
    tap(437, 950, "大圆 CTA")
    time.sleep(3)
    tap(437, 1600, "快门")
    time.sleep(2)
    # 用 debug 入口跑(联网模式)
    shell(f"am start -n {PKG}/.MainActivity --es debug_image /sdcard/Download/test_sign_luckin.jpg --ed debug_lat 39.9920 --ed debug_lng 116.3360")
    time.sleep(8)
    screencap(os.path.join(DEMO_DIR, "03_result_online.png"))
    time.sleep(1)
    # 滚到顶看附近 12 家
    shell("input swipe 500 800 500 200 300")
    time.sleep(1)
    shell("input swipe 500 800 500 200 300")
    time.sleep(1)
    screencap(os.path.join(DEMO_DIR, "04_nearby_12.png"))
    time.sleep(1)
    # 点最低价(估计在 4 平台 grid 中,具体位置看实际)
    # 这里不强求点中,留作用户手动
    time.sleep(2)

    # === 收尾 ===
    print('\n=== 收尾: 停止 logcat + 录屏 ===')
    stop_logcat(log_proc, log_f)
    time.sleep(2)
    # 录屏自动结束 (--time-limit 300)
    stop_and_pull(rec, mp4_path)

    # 整理
    print(f'\n[done] 文件清单:')
    for f in sorted(os.listdir(DEMO_DIR)):
        full = os.path.join(DEMO_DIR, f)
        size = os.path.getsize(full)
        print(f'  {f}  {size/1024:.1f} KB')


if __name__ == '__main__':
    main()
