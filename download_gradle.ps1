$url = 'https://services.gradle.org/distributions/gradle-8.2-bin.zip'
$dst = 'D:\B-AI\m-qwen\tools\gradle-8.2.zip'
$LogFile = 'D:\B-AI\m-qwen\tools\gradle-dl.log'

function Log([string]$msg) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

Log "Downloading Gradle 8.2 ..."
Log "URL: $url"
Log "Dst: $dst"

if (Test-Path $dst) {
    $sz = (Get-Item $dst).Length
    Log "Already present: $([math]::Round($sz/1MB,0)) MB"
    if ($sz -gt 100MB) { exit 0 }
    Remove-Item $dst -Force
}

Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromMinutes(30)
$client.DefaultRequestHeaders.Add('User-Agent', 'Mozilla/5.0')

$resp = $client.GetAsync($url, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
$resp.EnsureSuccessStatusCode() | Out-Null
$total = $resp.Content.Headers.ContentLength
Log "Content-Length: $([math]::Round($total/1MB,0)) MB"

$in = $resp.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
$out = [System.IO.File]::OpenWrite($dst)
$buf = New-Object byte[] 1048576
$downloaded = 0L
$last = Get-Date
while (($read = $in.Read($buf, 0, $buf.Length)) -gt 0) {
    $out.Write($buf, 0, $read)
    $downloaded += $read
    $now = Get-Date
    if (($now - $last).TotalSeconds -ge 10) {
        $mbps = [math]::Round($read / 1MB / 1, 2)
        Log "  $([math]::Round($downloaded/1MB,0)) / $([math]::Round($total/1MB,0)) MB"
        $last = $now
    }
}
$out.Close(); $in.Close()
$resp.Dispose(); $client.Dispose()

Log "Download complete: $([math]::Round($downloaded/1MB,0)) MB"

Log "Extracting to D:\B-AI\m-qwen\tools\gradle-8.2 ..."
$extractDir = 'D:\B-AI\m-qwen\tools\gradle-8.2'
if (Test-Path $extractDir) { cmd /c "rmdir /s /q $extractDir" 2>&1 | Out-Null }
New-Item -Path $extractDir -ItemType Directory -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory($dst, $extractDir)
# Move nested gradle-8.2 to top
$inner = Get-ChildItem $extractDir -Directory | Where-Object { $_.Name -match '^gradle-' } | Select-Object -First 1
if ($inner) {
    cmd /c "robocopy `"$($inner.FullName)`" `"$extractDir`" /E /MOVE" 2>&1 | Out-Null
}

$gradleBat = "$extractDir\bin\gradle.bat"
if (Test-Path $gradleBat) {
    Log "Gradle ready at $gradleBat"
    cmd /c "`"$gradleBat`" --version" 2>&1 | ForEach-Object { Log "  $_" }
}

Log "=== Done ==="