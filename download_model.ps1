# Qwen3-VL-2B-Instruct-MNN downloader v3 (simple serial stream download)
# Usage: powershell -ExecutionPolicy Bypass -File download_model.ps1

$ErrorActionPreference = 'Continue'
$ProgressPreference   = 'SilentlyContinue'

$ModelId    = 'MNN/Qwen3-VL-2B-Instruct-MNN'
$Revision   = 'master'
$TargetRoot = if ($env:MODEL_TARGET) { $env:MODEL_TARGET } else {
    Join-Path $PSScriptRoot 'models\Qwen3-VL-2B-Instruct-MNN'
}
$LogFile    = Join-Path $PSScriptRoot 'logs\download-model-v2.log'

New-Item -Path $TargetRoot -ItemType Directory -Force | Out-Null
$logDir = Split-Path $LogFile -Parent
New-Item -Path $logDir -ItemType Directory -Force | Out-Null

function Log([string]$msg) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

Log "=== ModelScope model download (v3 serial stream) ==="
Log "Model: $ModelId @ $Revision"
Log "Target: $TargetRoot"

$apiUrl = "https://www.modelscope.cn/api/v1/models/$ModelId/repo/files?Revision=$Revision&Recursive=True"
Log "Fetching file list: $apiUrl"

try {
    $resp = Invoke-WebRequest -Uri $apiUrl -UseBasicParsing -TimeoutSec 30
    $files = ($resp.Content | ConvertFrom-Json).Data.Files
    Log "Got $($files.Count) files, total $([math]::Round(($files | Measure-Object -Property Size -Sum).Sum / 1MB, 1)) MB"
} catch {
    Log "FATAL: failed to fetch file list: $_"
    exit 1
}

# Sort small first
$files = $files | Sort-Object Size

Add-Type -AssemblyName System.Net.Http

$totalBytes = ($files | Measure-Object -Property Size -Sum).Sum
$dlBytes    = 0L
$doneBytes  = 0L

foreach ($f in $files) {
    $name = $f.Name
    $size = [long]$f.Size
    $url  = "https://www.modelscope.cn/models/$ModelId/resolve/$Revision/$([uri]::EscapeDataString($name))"
    $dst  = Join-Path $TargetRoot $name
    $tmp  = "$dst.partial"

    # already complete?
    if (Test-Path $dst) {
        $existing = (Get-Item $dst).Length
        if ($existing -eq $size) {
            Log "  SKIP $name ($([math]::Round($size/1MB,1)) MB, present)"
            $doneBytes += $size
            continue
        } else {
            Remove-Item $dst -Force -ErrorAction SilentlyContinue
        }
    }
    if (Test-Path $tmp) { Remove-Item $tmp -Force -ErrorAction SilentlyContinue }

    $maxAttempts = 3
    $success = $false
    for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
        try {
            $startedAt = Get-Date
            Log "  [$attempt/$maxAttempts] GET $name ($([math]::Round($size/1MB,1)) MB)"

            $client = New-Object System.Net.Http.HttpClient
            $client.Timeout = [TimeSpan]::FromHours(2)
            $client.DefaultRequestHeaders.Add('User-Agent', 'ModelScope-Downloader/1.0')

            $response = $client.GetAsync($url, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
            if (-not $response.IsSuccessStatusCode) {
                throw "HTTP $($response.StatusCode) $($response.ReasonPhrase)"
            }

            $input  = $response.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
            $output = [System.IO.File]::OpenWrite($tmp)

            $buffer = New-Object byte[] 65536
            $read = 0
            $lastReport = Get-Date
            $sessionBytes = 0
            while (($read = $input.Read($buffer, 0, $buffer.Length)) -gt 0) {
                $output.Write($buffer, 0, $read)
                $sessionBytes += $read
                $now = Get-Date
                if (($now - $lastReport).TotalSeconds -ge 5) {
                    $pct = if ($size -gt 0) { [math]::Round(($sessionBytes * 100.0) / $size, 1) } else { 0 }
                    $mbps = [math]::Round($sessionBytes / 1MB / ($now - $lastReport).TotalSeconds, 2)
                    $cur = (Get-Item $tmp -ErrorAction SilentlyContinue).Length
                    Log "      $name $pct% ($([math]::Round($cur/1MB,0))/$([math]::Round($size/1MB,0)) MB, $mbps MB/s)"
                    $lastReport = $now
                    $sessionBytes = 0
                }
            }
            $output.Close(); $output.Dispose()
            $input.Close(); $input.Dispose()
            $response.Dispose(); $client.Dispose()

            $finalSize = (Get-Item $tmp).Length
            if ($finalSize -ne $size) {
                throw "size mismatch: expected $size, got $finalSize"
            }
            Move-Item $tmp $dst -Force
            $elapsed = (Get-Date) - $startedAt
            $mbps = [math]::Round($size / 1MB / $elapsed.TotalSeconds, 2)
            Log "  OK  $name in $([math]::Round($elapsed.TotalSeconds,1))s ($mbps MB/s)"
            $success = $true
            $doneBytes += $size
            break
        } catch {
            Log "  FAIL $name attempt $attempt : $_"
            if (Test-Path $tmp) { Remove-Item $tmp -Force -ErrorAction SilentlyContinue }
            Start-Sleep -Seconds (10 * $attempt)
        }
    }
    if (-not $success) {
        Log "  GIVEUP $name after $maxAttempts attempts"
    }
}

Log "=== Download finished ==="
Log "Total downloaded: $([math]::Round($doneBytes / 1MB, 0)) / $([math]::Round($totalBytes / 1MB, 0)) MB"
Get-ChildItem $TargetRoot | ForEach-Object {
    $size = [math]::Round($_.Length / 1MB, 1)
    Log "  $($_.Name) ($size MB)"
}