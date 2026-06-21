$LogFile = 'D:\B-AI\m-qwen\tools\oss-upload-fast.log'
$Ossutil = 'D:\B-AI\m-qwen\tools\ossutil64\ossutil64.exe'
$Bucket = 'ganfansheng-models-2026'
$Region = 'oss-cn-hangzhou'
$ModelDir = 'D:\B-AI\m-qwen\models\Qwen3-VL-2B-Instruct-MNN'

function Log([string]$msg) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

Log "=== OSS fast parallel upload ==="

# Strategy: split each large file into chunks for parallel upload,
# OR rely on ossutil internal multipart with more threads.
# Use --upload-threads=10 and --part-size=50MB for big files.

$files = Get-ChildItem $ModelDir -File | Sort-Object Length -Descending

foreach ($f in $files) {
    $remote = "oss://$Bucket/qwen3-vl-2b-mnn/$($f.Name)"
    $sizeMb = [math]::Round($f.Length/1MB, 1)
    Log "upload $($f.Name) ($sizeMb MB)"

    if ($f.Length -gt 100MB) {
        # large file: use parallel multipart
        $args = @('cp', "`"$($f.FullName)`"", "`"$remote`"", '--force',
                  '--upload-threads=10', '--part-size=52428800', '--checkpoint-dir=D:\B-AI\m-qwen\tools\.oss_checkpoint')
        Log "  big-file args: $($args -join ' ')"
        $proc = Start-Process -FilePath $Ossutil -ArgumentList $args -RedirectStandardOutput "$LogFile.out" -WindowStyle Hidden -PassThru
        # Wait for it (this is a foreground wait, blocking)
        $proc | Wait-Process -Timeout 1800 -ErrorAction SilentlyContinue
        if ($proc.HasExited -and $proc.ExitCode -ne 0) {
            Log "  FAIL exit=$($proc.ExitCode)"
        } else {
            Log "  OK"
        }
    } else {
        & $Ossutil cp "$($f.FullName)" "$remote" --force 2>&1 | ForEach-Object { Log "    $_" }
    }
}

Log "=== verify ==="
& $Ossutil ls "oss://$Bucket/qwen3-vl-2b-mnn/" 2>&1 | ForEach-Object { Log "  $_" }
Log "=== done ==="