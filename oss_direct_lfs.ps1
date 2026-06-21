# Direct upload llm.mnn.weight and visual.mnn.weight using ossutil with progress
$ErrorActionPreference = 'Continue'
$ProgressPreference   = 'SilentlyContinue'

$LogFile = 'D:\B-AI\m-qwen\tools\oss-direct-lfs.log'
$Ossutil = 'D:\B-AI\m-qwen\tools\ossutil64\ossutil64.exe'
$Bucket  = 'ganfansheng-models-2026'
$ModelDir = 'D:\B-AI\m-qwen\models\Qwen3-VL-2B-Instruct-MNN'

function Log([string]$msg) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

# Two large LFS files, in priority order: llm.mnn.weight is the killer.
$targets = @(
    @{ name = 'visual.mnn.weight'; size = 238226780 },
    @{ name = 'llm.mnn.weight';    size = 1231860194 }
)

foreach ($t in $targets) {
    $local  = Join-Path $ModelDir $t.name
    $remote = "oss://$Bucket/qwen3-vl-2b-mnn/$($t.name)"
    $sizeMb = [math]::Round($t.size/1MB, 1)

    # Check if already uploaded
    $existing = & $Ossutil ls "$remote" 2>&1 | Where-Object { $_ -match 'Standard' -or $_ -match $t.name }
    if ($existing -match $t.name) {
        Log "$($t.name) already on OSS, skipping"
        continue
    }

    Log "Uploading $sizeMb MB $($t.name) ..."
    # ossutil64 doesn't support --upload-threads. Use --parallel=N instead.
    $args = @('cp', "`"$local`"", "`"$remote`"", '--force',
              '--parallel=20',
              '--part-size=104857600',        # 100MB parts
              "--checkpoint-dir=D:\B-AI\m-qwen\tools\.oss_ckpt_$($t.name)")

    Log "  starting ossutil with $($args.Count) args"
    $proc = Start-Process -FilePath $Ossutil -ArgumentList $args -RedirectStandardOutput "$LogFile.out.$($t.name)" -WindowStyle Hidden -PassThru
    Log "  PID: $($proc.Id)"

    while (-not $proc.HasExited) {
        Start-Sleep -Seconds 10
        $running = Get-Process -Id $proc.Id -ErrorAction SilentlyContinue
        if (-not $running) { break }
        Log "  alive: CPU=$([math]::Round($running.CPU,1))s WS=$([math]::Round($running.WorkingSet64/1MB,0))MB"
        # Also tail the ossutil output file if it exists
        $tail = Get-Content "$LogFile.out.$($t.name)" -ErrorAction SilentlyContinue | Select-Object -Last 3
        if ($tail) { Log "  | $_" }
    }

    $proc | Wait-Process -Timeout 7200 -ErrorAction SilentlyContinue
    Log "  exit=$($proc.ExitCode)"
}

Log "=== final verify ==="
& $Ossutil ls "oss://$Bucket/qwen3-vl-2b-mnn/" 2>&1 | ForEach-Object { Log "  $_" }
Log "=== done ==="