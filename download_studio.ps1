# Android Studio zip downloader
# Usage: powershell -ExecutionPolicy Bypass -File download_studio.ps1

$ErrorActionPreference = 'Continue'
$ProgressPreference   = 'SilentlyContinue'

$Url = 'https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2024.2.2.13/android-studio-2024.2.2.13-windows.zip'
$TargetDir  = 'D:\B-AI\m-qwen\tools'
$TargetPath = Join-Path $TargetDir 'android-studio.zip'
$LogFile    = Join-Path $TargetDir 'studio-dl.log'

New-Item -Path $TargetDir -ItemType Directory -Force | Out-Null

function Log([string]$msg) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

if (Test-Path $TargetPath) {
    $existing = (Get-Item $TargetPath).Length
    Log "Android Studio zip already exists: $([math]::Round($existing/1MB,0)) MB"
    if ($existing -gt 800MB) {
        Log "Looks complete, skipping download"
        exit 0
    }
    Remove-Item $TargetPath -Force
}

Log "Downloading Android Studio 2024.2.2.13 zip from Google CDN..."
Log "URL: $Url"
Log "Target: $TargetPath"

Add-Type -AssemblyName System.Net.Http

$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromHours(3)
$client.DefaultRequestHeaders.Add('User-Agent', 'Mozilla/5.0 AndroidStudio-Downloader')

$response = $client.GetAsync($Url, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
$response.EnsureSuccessStatusCode() | Out-Null

$totalBytes = $response.Content.Headers.ContentLength
if ($totalBytes) {
    Log "Content-Length: $([math]::Round($totalBytes/1MB,0)) MB"
}

$input  = $response.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
$output = [System.IO.File]::OpenWrite($TargetPath)

$buffer = New-Object byte[] 1048576
$total = 0L
$lastReport = Get-Date
$sessionBytes = 0

while (($read = $input.Read($buffer, 0, $buffer.Length)) -gt 0) {
    $output.Write($buffer, 0, $read)
    $total += $read
    $sessionBytes += $read
    $now = Get-Date
    if (($now - $lastReport).TotalSeconds -ge 10) {
        $mbps = [math]::Round($sessionBytes / 1MB / ($now - $lastReport).TotalSeconds, 2)
        $pct = if ($totalBytes) { [math]::Round($total * 100.0 / $totalBytes, 1) } else { 0 }
        Log "  $pct% ($([math]::Round($total/1MB,0)) / $([math]::Round($totalBytes/1MB,0)) MB, $mbps MB/s)"
        $lastReport = $now
        $sessionBytes = 0
    }
}

$output.Close(); $output.Dispose()
$input.Close(); $input.Dispose()
$response.Dispose(); $client.Dispose()

Log "Download complete: $([math]::Round($total/1MB,0)) MB"