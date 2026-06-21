import io
import json
import re
import base64
import subprocess

# Run mavis browser tool screenshot and parse output
r = subprocess.run(['mavis', 'browser', 'tool', 'screenshot', '{}'],
                   capture_output=True, text=True, shell=True,
                   cwd='D:\\C-AI\\m-code\\m-qwen')

# output is JSON with content = base64 PNG
out = r.stdout
# Find data: URL
m = re.search(r'"content":\s*"(data:image/png;base64,[A-Za-z0-9+/=]+)"', out)
if m:
    b64 = m.group(1).split(',', 1)[1]
    png = base64.b64decode(b64)
    with open(r'D:\C-AI\m-code\m-qwen\logs\xhs_publish_0.png', 'wb') as f:
        f.write(png)
    print('saved', len(png), 'bytes')
else:
    print('no match, output head:', out[:500])
    print('stderr:', r.stderr[:300])
