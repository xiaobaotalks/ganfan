"""
驱动 mavis browser tool 的小帮手
- 用 subprocess list args 传 JSON(避免 PowerShell 解析问题)
- 提供 query / click / type / set_file / screenshot 等便捷函数
"""
import json
import re
import subprocess
import sys


def call(tool, args, timeout=30000):
    """调一次 mavis browser tool <name> <json-args-string>"""
    if isinstance(args, dict):
        args_str = json.dumps(args, ensure_ascii=False)
    else:
        args_str = args
    cmd = [r'C:\Users\40314\.mavis\bin\mavis.cmd', 'browser', 'tool', tool]
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout,
                       input=args_str, shell=False)
    if r.returncode != 0:
        print(f'[ERR {tool}] stderr={r.stderr[:200]}', file=sys.stderr)
    out = r.stdout
    # 解析
    try:
        return json.loads(out)
    except Exception:
        return out


def screenshot(path):
    """截屏并保存到 path"""
    r = call('screenshot', {})
    content = r.get('content', '') if isinstance(r, dict) else ''
    m = re.match(r'data:image/png;base64,(.+)', content)
    if not m:
        print('screenshot parse fail', r if isinstance(r, str) else r)
        return False
    import base64
    png = base64.b64decode(m.group(1))
    with open(path, 'wb') as f:
        f.write(png)
    print(f'screenshot saved {path} ({len(png)} bytes)')
    return True


if __name__ == '__main__':
    # 简单 smoke
    tabs = call('get_tabs', {})
    print('tabs:', json.dumps(tabs, ensure_ascii=False, indent=2)[:500])
