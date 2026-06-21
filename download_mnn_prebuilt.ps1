# Download prebuilt MNN releases (MNN_ANDROID_ARM64 / MNN_ARM64_LLM)
$ErrorActionPreference = 'Continue'
$ProgressPreference   = 'SilentlyContinue'

$ToolsDir = 'D:\B-AI\m-qwen\tools'
$LogFile  = "$ToolsDir\mnn-prebuilt.log"
$ApiBase  = 'https://api.github.com/repos/alibaba/MNN/releases'

function Log([string]$msg) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

Log "=== Fetch MNN releases metadata ==="
try {
    $releases = Invoke-RestMethod -Uri "$ApiBase?per_page=10" -UseBasicParsing -TimeoutSec 30 -Headers @{
        'User-Agent' = 'Mavis-MNN-Downloader'
    }
} catch {
    Log "FATAL: cannot list releases: $_"
    exit 1
}

if (-not $releases -or $releases.Count -eq 0) {
    Log "FATAL: no releases returned"
    exit 1
}

# Find the latest release asset matching arm64 / android / llm
$candidates = @()
foreach ($rel in $releases) {
    Log "release: $($rel.tag_name)"
    foreach ($asset in $rel.assets) {
        $n = $asset.name.ToLower()
        if ($n -match 'android.*arm.*64|android.*aarch|android.*arm64') {
            $candidates += [pscustomobject]@{
                tag  = $rel.tag_name
                name = $asset.name
                url  = $asset.browser_download_url
                size = $asset.size
            }
        }
    }
}

if ($candidates.Count -eq 0) {
    Log "FATAL: no Android arm64 asset found"
    exit 1
}

Log "Candidates (Android arm64):"
$candidates | ForEach-Object { Log "  $($_.tag) :: $($_.name) :: $([math]::Round($_.size/1MB,1)) MB :: $($_.url)" }

# Prefer assets containing 'llm' or 'mnn' (multi-modal capable)
$pick = $candidates | Where-Object { $_.name -match 'llm|full|release' } | Select-Object -First 1
if (-not $pick) { $pick = $candidates | Select-Object -First 1 }
Log "Picked: $($pick.tag) :: $($pick.name)"

$dst = Join-Path $ToolsDir 'mnn-prebuilt.zip'
if (Test-Path $dst) { Remove-Item $dst -Force }

Log "Downloading $($pick.url) ..."
Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromHours(1)
$client.DefaultRequestHeaders.Add('User-Agent', 'Mozilla/5.0')
$resp = $client.GetAsync($pick.url, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
$resp.EnsureSuccessStatusCode() | Out-Null
$in = $resp.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
$out = [System.IO.File]::OpenWrite($dst)
$buf = New-Object byte[] 1048576
$total = 0L
$lastReport = Get-Date
while (($read = $in.Read($buf, 0, $buf.Length)) -gt 0) {
    $out.Write($buf, 0, $read)
    $total += $read
    $now = Get-Date
    if (($now - $lastReport).TotalSeconds -ge 10) {
        Log "  $([math]::Round($total/1MB,0)) MB"
        $lastReport = $now
    }
}
$out.Close(); $in.Close()
$resp.Dispose(); $client.Dispose()
Log "Saved to $dst ($([math]::Round($total/1MB,1)) MB)"

# Extract and find libMNN.so
Log "Extracting ..."
$extractDir = "$ToolsDir\mnn-prebuilt"
if (Test-Path $extractDir) { cmd /c "rmdir /s /q $extractDir" 2>&1 | Out-Null }
New-Item -Path $extractDir -ItemType Directory -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory($dst, $extractDir)

Log "Looking for libMNN.so inside the archive ..."
$so = Get-ChildItem -Path $extractDir -Recurse -Filter 'libMNN.so' -File -ErrorAction SilentlyContinue | Select-Object -First 5
$so | ForEach-Object {
    Log "  found: $($_.FullName) ($([math]::Round($_.Length/1MB,1)) MB)"
}

if ($so) {
    $dest = 'D:\alimnn\app\src\main\jniLibs\arm64-v8a\libMNN.so'
    $soDir = Split-Path $dest -Parent
    if (-not (Test-Path $soDir)) { New-Item -Path $soDir -ItemType Directory -Force | Out-Null }
    Copy-Item $so[0].FullName $dest -Force
    Log "Copied to $dest"
} else {
    Log "WARNING: libMNN.so not found in archive; check $extractDir"
}

Log "=== Done ==="