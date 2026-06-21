$url = 'https://github.com/alibaba/MNN/releases/download/3.6.0/mnn_3.6.0_android_armv7_armv8_cpu_opencl_vulkan.zip'
$dst = 'D:\B-AI\m-qwen\tools\mnn-3.6.0-android.zip'
$LogFile = 'D:\B-AI\m-qwen\tools\mnn-prebuilt-dl.log'

function Log([string]$msg) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

Log "Downloading MNN 3.6.0 Android arm64 (CPU+OpenCL+Vulkan) ..."
Log "URL: $url"
Log "Dst: $dst"

if (Test-Path $dst) {
    $sz = (Get-Item $dst).Length
    Log "Already downloaded: $([math]::Round($sz/1MB,0)) MB"
    if ($sz -gt 50MB) {
        Log "Looks complete, skipping"
        exit 0
    }
    Remove-Item $dst -Force
}

Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromHours(2)
$client.DefaultRequestHeaders.Add('User-Agent', 'Mozilla/5.0')

$resp = $client.GetAsync($url, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
$resp.EnsureSuccessStatusCode() | Out-Null

$total = $resp.Content.Headers.ContentLength
Log "Content-Length: $([math]::Round($total/1MB,0)) MB"

$in = $resp.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
$out = [System.IO.File]::OpenWrite($dst)

$buf = New-Object byte[] 1048576
$downloaded = 0L
$lastReport = Get-Date
$sessionBytes = 0

while (($read = $in.Read($buf, 0, $buf.Length)) -gt 0) {
    $out.Write($buf, 0, $read)
    $downloaded += $read
    $sessionBytes += $read
    $now = Get-Date
    if (($now - $lastReport).TotalSeconds -ge 10) {
        $mbps = [math]::Round($sessionBytes / 1MB / ($now - $lastReport).TotalSeconds, 2)
        $pct = if ($total -gt 0) { [math]::Round($downloaded * 100.0 / $total, 1) } else { 0 }
        Log "  $pct% ($([math]::Round($downloaded/1MB,0)) / $([math]::Round($total/1MB,0)) MB, $mbps MB/s)"
        $lastReport = $now
        $sessionBytes = 0
    }
}

$out.Close(); $out.Dispose()
$in.Close();  $in.Dispose()
$resp.Dispose(); $client.Dispose()

Log "Download complete: $([math]::Round($downloaded/1MB,0)) MB"

# extract and find libMNN.so for arm64-v8a
Log "Extracting..."
$extractDir = 'D:\B-AI\m-qwen\tools\mnn-prebuilt'
if (Test-Path $extractDir) { cmd /c "rmdir /s /q $extractDir" 2>&1 | Out-Null }
New-Item -Path $extractDir -ItemType Directory -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory($dst, $extractDir)

Log "Looking for libMNN.so ..."
$so = Get-ChildItem -Path $extractDir -Recurse -Filter 'libMNN.so' -File -ErrorAction SilentlyContinue
$so | ForEach-Object { Log "  found: $($_.FullName) ($([math]::Round($_.Length/1MB,1)) MB)" }

if ($so) {
    # Prefer arm64-v8a (aarch64) over armv7
    $soAarch64 = $so | Where-Object { $_.DirectoryName -match 'aarch64|arm64|arm64-v8a' } | Select-Object -First 1
    if (-not $soAarch64) { $soAarch64 = $so | Sort-Object Length -Descending | Select-Object -First 1 }
    $dest = 'D:\alimnn\app\src\main\jniLibs\arm64-v8a\libMNN.so'
    $destDir = Split-Path $dest -Parent
    if (-not (Test-Path $destDir)) { New-Item -Path $destDir -ItemType Directory -Force | Out-Null }
    Copy-Item $soAarch64.FullName $dest -Force
    Log "Copied to $dest ($([math]::Round((Get-Item $dest).Length/1MB,1)) MB)"
} else {
    Log "WARNING: libMNN.so not found in archive!"
    cmd /c "dir $extractDir /s /b" 2>&1 | ForEach-Object { Log "  $_" }
}

Log "=== Done ==="