# ossutil64 download + OSS bucket + upload model
$ErrorActionPreference = 'Continue'
$ProgressPreference   = 'SilentlyContinue'

$ToolsDir     = 'D:\B-AI\m-qwen\tools'
$OssutilPath  = "$ToolsDir\ossutil64.exe"
$OssutilUrl   = 'https://gosspublic.alicdn.com/ossutil/1.7.18/ossutil64.zip'
$OssutilZip   = "$ToolsDir\ossutil.zip"
$LogFile      = "$ToolsDir\oss-setup.log"
$AccessKeyId     = $env:OSS_ACCESS_KEY_ID
$AccessKeySecret = $env:OSS_ACCESS_KEY_SECRET

if (-not $AccessKeyId -or -not $AccessKeySecret) {
    Write-Error "请先设置环境变量 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET"
    exit 1
}
$BucketName   = 'ganfansheng-models-2026'
$Region       = 'oss-cn-hangzhou'
$Endpoint     = "$Region.aliyuncs.com"
$ModelDir     = 'D:\B-AI\m-qwen\models\Qwen3-VL-2B-Instruct-MNN'

function Log([string]$msg) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

Log "=== OSS bucket + model upload ==="

# ---- 1. download ossutil64 ----
if (-not (Test-Path $OssutilPath)) {
    if (-not (Test-Path $OssutilZip)) {
        Log "Downloading ossutil64 ..."
        Add-Type -AssemblyName System.Net.Http
        $client = New-Object System.Net.Http.HttpClient
        $client.Timeout = [TimeSpan]::FromMinutes(5)
        $client.DefaultRequestHeaders.Add('User-Agent', 'Mozilla/5.0')
        $resp = $client.GetAsync($OssutilUrl, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
        $resp.EnsureSuccessStatusCode() | Out-Null
        $in = $resp.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
        $out = [System.IO.File]::OpenWrite($OssutilZip)
        $buf = New-Object byte[] 1048576
        $total = 0L
        while (($read = $in.Read($buf, 0, $buf.Length)) -gt 0) {
            $out.Write($buf, 0, $read); $total += $read
        }
        $out.Close(); $in.Close()
        $resp.Dispose(); $client.Dispose()
        Log "ossutil64.zip: $([math]::Round($total/1MB,1)) MB"
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($OssutilZip, $ToolsDir)
    # ossutil64.exe is at the root of the zip
    $extracted = Get-ChildItem "$ToolsDir\ossutil64*" -File | Select-Object -First 1
    if ($extracted -and $extracted.Name -ne 'ossutil64.exe') {
        Move-Item $extracted.FullName $OssutilPath -Force
    }
    Remove-Item $OssutilZip -Force -ErrorAction SilentlyContinue
    Log "ossutil64.exe ready at $OssutilPath"
}

# ---- 2. configure ossutil ----
Log "Configuring ossutil with AccessKey..."
$cfgFile = "$env:USERPROFILE\.ossutilconfig"
$cfg = @"
[Credentials]
language=EN
endpoint=$Endpoint
accessKeyID=$AccessKeyId
accessKeySecret=$AccessKeySecret
stsToken=
"@
Set-Content -Path $cfgFile -Value $cfg -Encoding UTF8
Log "Config written to $cfgFile"

# ---- 3. test connection ----
Log "Listing existing buckets (smoke test)..."
& $OssutilPath ls oss:// 2>&1 | ForEach-Object { Log "  $_" }

# ---- 4. create bucket (ignore if already exists) ----
Log "Creating bucket $BucketName in $Region ..."
& $OssutilPath mb "oss://$BucketName" --acl public-read --region $Region 2>&1 | ForEach-Object {
    Log "  $_"
}

# ---- 5. upload model ----
Log "Uploading model files from $ModelDir ..."
foreach ($f in Get-ChildItem $ModelDir -File) {
    $local = $f.FullName
    $remote = "oss://$BucketName/qwen3-vl-2b-mnn/$($f.Name)"
    Log "  upload $($f.Name) ($([math]::Round($f.Length/1MB,1)) MB) -> $remote"
    & $OssutilPath cp "$local" "$remote" --force 2>&1 | ForEach-Object { Log "    $_" }
}

Log "=== OSS setup done ==="
Log "Public URL pattern: https://$BucketName.$Endpoint/qwen3-vl-2b-mnn/<filename>"
& $OssutilPath ls "oss://$BucketName/qwen3-vl-2b-mnn/" 2>&1 | ForEach-Object { Log "  $_" }