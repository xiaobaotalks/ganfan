# OSS bucket + model upload v2 (correct ossutil path)
$ErrorActionPreference = 'Continue'
$ProgressPreference   = 'SilentlyContinue'

$LogFile      = "D:\B-AI\m-qwen\tools\oss-setup.log"
$OssutilPath  = "D:\B-AI\m-qwen\tools\ossutil64\ossutil64.exe"
$BucketName   = 'ganfansheng-models-2026'
$Region       = 'oss-cn-hangzhou'
$Endpoint     = "$Region.aliyuncs.com"
$ModelDir     = 'D:\B-AI\m-qwen\models\Qwen3-VL-2B-Instruct-MNN'

function Log([string]$msg) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

if (-not (Test-Path $OssutilPath)) {
    Log "FATAL: ossutil64.exe not at $OssutilPath"
    exit 1
}

Log "=== OSS setup v2 ==="

# create bucket (acl=public-read, ignore if exists)
Log "Creating bucket $BucketName ..."
$mb_out = & $OssutilPath mb "oss://$BucketName" --acl public-read 2>&1
foreach ($l in $mb_out) { Log "  mb: $l" }
Log "mb exit: $LASTEXITCODE"

# upload model files one-by-one with progress
Get-ChildItem $ModelDir -File | ForEach-Object {
    $local = $_.FullName
    $remote = "oss://$BucketName/qwen3-vl-2b-mnn/$($_.Name)"
    $sizeMb = [math]::Round($_.Length/1MB, 1)
    Log "upload $($_.Name) ($sizeMb MB) ..."
    $cp_out = & $OssutilPath cp "$local" "$remote" --force 2>&1
    foreach ($l in $cp_out) { Log "  cp: $l" }
    Log "  $LASTEXITCODE"
}

Log "=== verify ==="
& $OssutilPath ls "oss://$BucketName/qwen3-vl-2b-mnn/" 2>&1 | ForEach-Object { Log "  $_" }

Log "=== Done ==="