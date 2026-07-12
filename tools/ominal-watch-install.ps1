param(
    [string]$Workspace = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path,
    [string]$ExpectedDevice = 'O7ON59OZEY7LOVQG'
)

$ErrorActionPreference = 'Continue'
$stateDir = Join-Path $Workspace 'build-logs\ominal-watch'
$log = Join-Path $stateDir 'watch.log'
New-Item -ItemType Directory -Force -Path $stateDir | Out-Null

function Write-Log([string]$Message) {
    Add-Content -LiteralPath $log -Value ("[{0}] {1}" -f (Get-Date -Format o), $Message) -Encoding UTF8
}

$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$apk = Get-ChildItem -Recurse -File -Path (Join-Path $Workspace 'app\build\outputs\apk\debug') -Filter '*universal*.apk' |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) {
    Write-Log "No APK found. Exiting."
    exit 1
}

Write-Log "Watcher started. APK=$($apk.FullName)"

while ($true) {
    try {
        $devices = & $adb devices -l 2>$null
        $deviceLine = $devices | Where-Object { $_ -match '\bdevice\b' -and $_ -notmatch '^List of devices' } | Select-Object -First 1

        if ($deviceLine) {
            $serial = ($deviceLine -split '\s+')[0]
            Write-Log "Device detected: $serial"
            & $adb -s $serial install -r $apk.FullName | Out-String | ForEach-Object { if ($_) { Write-Log $_.TrimEnd() } }
            if ($LASTEXITCODE -eq 0) {
                Write-Log "Install succeeded on $serial"
                & $adb -s $serial shell am force-stop com.ominal | Out-Null
                & $adb -s $serial shell am start -W -n com.ominal/.app.OringutanActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER | Out-String | ForEach-Object { if ($_) { Write-Log $_.TrimEnd() } }
                Write-Log "Launch command sent. Stopping watcher."
                break
            } else {
                Write-Log "Install failed on $serial exit=$LASTEXITCODE"
            }
        } else {
            Write-Log "No device detected."
        }

        $printer = Get-CimInstance Win32_Printer -Filter "Name='EPSON L3210 Series'" -ErrorAction SilentlyContinue
        if ($printer) {
            Write-Log ("Printer status: Offline={0} Status={1} PrinterStatus={2}" -f $printer.WorkOffline, $printer.Status, $printer.PrinterStatus)
        }
    } catch {
        Write-Log "Watcher error: $($_.Exception.Message)"
    }

    Start-Sleep -Seconds 10
}
