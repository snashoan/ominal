param(
    [string]$Workspace = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path,
    [string]$Device = "O7ON59OZEY7LOVQG",
    [int]$WaitMinutes = 30
)

$ErrorActionPreference = "Continue"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$logDir = Join-Path $Workspace "build-logs"
$log = Join-Path $logDir "ominal-adb-watch.log"
$install = Join-Path $Workspace "continue-ominal.ps1"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Write-WatchLog {
    param([string]$Message)
    Add-Content -LiteralPath $log -Value "[$(Get-Date -Format o)] $Message" -Encoding UTF8
}

Write-WatchLog "Watcher started. Device=$Device WaitMinutes=$WaitMinutes Workspace=$Workspace"

if (-not (Test-Path -LiteralPath $adb)) {
    Write-WatchLog "ADB not found: $adb"
    exit 2
}

if (-not (Test-Path -LiteralPath $install)) {
    Write-WatchLog "Install script not found: $install"
    exit 2
}

$deadline = (Get-Date).AddMinutes($WaitMinutes)
$lastHeartbeat = [datetime]::MinValue

while ((Get-Date) -lt $deadline) {
    try {
        $devices = & $adb devices -l 2>&1
        $line = $devices | Select-Object -Skip 1 | Where-Object { $_ -match $Device } | Select-Object -First 1

        if ($line -match "\bdevice\b") {
            Write-WatchLog "Device ready: $line"
            Push-Location $Workspace
            try {
                Write-WatchLog "Running continue-ominal.ps1"
                & $install -WaitMinutes 1 2>&1 | ForEach-Object {
                    Add-Content -LiteralPath $log -Value ([string]$_) -Encoding UTF8
                }
                Write-WatchLog "continue-ominal.ps1 exit=$LASTEXITCODE"
                exit $LASTEXITCODE
            } finally {
                Pop-Location
            }
        }

        if ($line -match "\bunauthorized\b") {
            Write-WatchLog "Device unauthorized: unlock phone and allow USB debugging."
        } elseif ((Get-Date) -gt $lastHeartbeat.AddSeconds(20)) {
            Write-WatchLog "Waiting for ADB device. Current adb output: $($devices -join ' | ')"
            $lastHeartbeat = Get-Date
        }
    } catch {
        Write-WatchLog "Watcher loop error: $($_.Exception.Message)"
    }

    Start-Sleep -Seconds 5
}

Write-WatchLog "Timed out waiting for $Device."
exit 1
