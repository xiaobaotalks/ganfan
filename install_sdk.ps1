# Android SDK components installer v3 (using wrapper)
$ErrorActionPreference = 'Continue'
$ProgressPreference   = 'SilentlyContinue'

$ToolsDir    = 'D:\B-AI\m-qwen\tools'
$SdkDir      = "$ToolsDir\android-sdk"
$LogFile     = "$ToolsDir\install-sdk.log"
$WrapperBat  = "$ToolsDir\sdkmanager_wrapper.bat"

function Log([string]$msg) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

Log "=== SDK install v3 (wrapper-based) ==="

# Accept licenses by piping "y" responses
Log "Accepting licenses..."
$y_input = "y`n" * 30
$lic_out = $y_input | cmd /c "`"$WrapperBat`" --sdk_root=`"$SdkDir`" --licenses" 2>&1
Log "licenses stdout: $($lic_out | Select-Object -First 3 | Out-String)"

$pkgs = @(
    'platform-tools'
    'platforms;android-34'
    'build-tools;34.0.0'
    'ndk;27.0.12077973'
    'cmake;3.22.1'
)

foreach ($p in $pkgs) {
    Log "Installing $p ..."
    & cmd /c "`"$WrapperBat`" --sdk_root=`"$SdkDir`" --install `"$p`"" 2>&1 | ForEach-Object {
        Log "  | $_"
    }
    Log "  done $p"
}

Log "=== Done. Listing installed: ==="
& cmd /c "`"$WrapperBat`" --sdk_root=`"$SdkDir`" --list_installed" 2>&1 | ForEach-Object {
    Log "  $_"
}