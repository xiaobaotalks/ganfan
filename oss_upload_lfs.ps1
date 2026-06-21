#!/usr/bin/env pwsh
# Retry uploading only the 2 missing large LFS files
$ErrorActionPreference = 'Continue'
$ProgressPreference   = 'SilentlyContinue'

$LogFile = 'D:\B-AI\m-qwen\tools\oss-upload-lfs.log'
$Ossutil = 'D:\B-AI\m-qwen\tools\ossutil64\ossutil64.exe'
$Bucket = 'ganfansheng-models-2026'
$ModelDir = 'D:\B-AI\m-qwen\models\Qwen3-VL-2B-Instruct-MNN'

function Log([string]$msg) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

$missing = @('llm.mnn.weight', 'visual.mnn.weight')

foreach ($name in $missing) {
    $local = Join-Path $ModelDir $name
    $size = (Get-Item $local).Length
    $sizeMb = [math]::Round($size/1MB, 1)
    $remote = "oss://$Bucket/qwen3-vl-2b-mnn/$name"

    Log "=== uploading $name ($sizeMb MB) ==="

    # Use 10 parallel threads + 50MB parts
    $args = @('cp', "`"$local`"", "`"$remote`"", '--force',
              '--upload-threads=10',
              '--part-size=52428800',
              '--checkpoint-dir=D:\B-AI\m-qwen\tools\.oss_checkpoint_$name')
    Log "  ossutil args: $($args -join ' ')"

    $proc = Start-Process -FilePath $Ossutil -ArgumentList $args -WindowStyle Hidden -PassThru
    Log "  PID: $($proc.Id)"

    # poll progress
    while (-not $proc.HasExited) {
        Start-Sleep -Seconds 15
        $running = Get-Process -Id $proc.Id -ErrorAction SilentlyContinue
        if (-not $running) { break }
        $cpu = [math]::Round($running.CPU, 1)
        $ws = [math]::Round($running.WorkingSet64/1MB, 0)
        Log "  alive cpu=$cpu ws=${ws}MB"
    }

    $proc | Wait-Process -Timeout 7200 -ErrorAction SilentlyContinue
    Log "  exit code: $($proc.ExitCode)"
}

Log "=== verify ==="
& $Ossutil ls "oss://$Bucket/qwen3-vl-2b-mnn/" --limited-num 100 2>&1 | ForEach-Object { Log "  $_" }
Log "=== done ==="