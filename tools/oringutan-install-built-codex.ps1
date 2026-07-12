param(
    [string] $DeviceId = "O7ON59OZEY7LOVQG",
    [string] $SshUser = "u0_a660",
    [int] $SshPort = 8022,
    [int] $WaitForDeviceMinutes = 0,
    [int] $PollSeconds = 15
)

$ErrorActionPreference = "Stop"

$TermuxAppRoot = Split-Path -Parent $PSScriptRoot
$SkynetRoot = Split-Path -Parent $TermuxAppRoot
$CodexRsDir = Join-Path $SkynetRoot "codex\codex-rs"
$LogDir = Join-Path $TermuxAppRoot "build-logs"
$BuiltBinary = Join-Path $CodexRsDir "target\aarch64-linux-android\release\codex"
$StrippedBinary = Join-Path $LogDir "codex-android-aarch64-stripped"

function Get-AndroidSdkRoot {
    if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) { return $env:ANDROID_HOME }
    if ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) { return $env:ANDROID_SDK_ROOT }

    $localSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $localSdk) { return $localSdk }

    throw "Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT."
}

function Get-NewestNdk {
    $sdk = Get-AndroidSdkRoot
    $ndkRoot = Join-Path $sdk "ndk"
    $ndk = Get-ChildItem -Path $ndkRoot -Directory | Sort-Object Name -Descending | Select-Object -First 1
    if (-not $ndk) { throw "No Android NDK found under $ndkRoot." }
    return $ndk.FullName
}

function Invoke-Checked {
    param(
        [string] $Label,
        [string] $FilePath,
        [string[]] $Arguments
    )

    Write-Host "[$Label]"
    Write-Host "> $FilePath $($Arguments -join ' ')"
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE"
    }
}

function Prepare-InstallBinary {
    if (-not (Test-Path $BuiltBinary)) {
        throw "Built Codex binary not found: $BuiltBinary"
    }

    New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
    Copy-Item -LiteralPath $BuiltBinary -Destination $StrippedBinary -Force

    $strip = Join-Path (Get-NewestNdk) "toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-strip.exe"
    if (Test-Path $strip) {
        Invoke-Checked -Label "Strip Codex binary" -FilePath $strip -Arguments @($StrippedBinary)
    }

    return $StrippedBinary
}

function Wait-ForAdbDevice {
    if ($WaitForDeviceMinutes -le 0) {
        Invoke-Checked -Label "Check ADB device" -FilePath $adb -Arguments @("-s", $DeviceId, "get-state")
        return
    }

    $deadline = (Get-Date).AddMinutes($WaitForDeviceMinutes)
    while ((Get-Date) -lt $deadline) {
        Write-Host "[Waiting for ADB device $DeviceId]"
        & $adb devices -l
        & $adb -s $DeviceId get-state
        if ($LASTEXITCODE -eq 0) {
            Write-Host "ADB device ready: $DeviceId"
            return
        }
        Start-Sleep -Seconds $PollSeconds
    }

    throw "ADB device $DeviceId was not available within $WaitForDeviceMinutes minutes."
}

$adb = Join-Path (Get-AndroidSdkRoot) "platform-tools\adb.exe"
if (-not (Test-Path $adb)) { $adb = "adb" }

$installBinary = Prepare-InstallBinary
Write-Host "Install binary: $installBinary"
Write-Host "Install size: $((Get-Item $installBinary).Length) bytes"

Wait-ForAdbDevice
Invoke-Checked -Label "Bring Termux to foreground" -FilePath $adb -Arguments @("-s", $DeviceId, "shell", "am", "start", "-n", "com.ominal/.app.TermuxActivity")
Start-Sleep -Seconds 2
Invoke-Checked -Label "Request Termux sshd start" -FilePath $adb -Arguments @("-s", $DeviceId, "shell", "input", "text", "sshd")
Invoke-Checked -Label "Submit Termux sshd command" -FilePath $adb -Arguments @("-s", $DeviceId, "shell", "input", "keyevent", "ENTER")
Start-Sleep -Seconds 2
Invoke-Checked -Label "Ensure Termux SSH forward" -FilePath $adb -Arguments @("-s", $DeviceId, "forward", "tcp:$SshPort", "tcp:8022")

$remoteTmp = "/data/data/com.ominal/files/home/codex-cross-built"
$remoteBin = "/data/data/com.ominal/files/usr/bin/codex"
$sshTarget = "$SshUser@127.0.0.1"

Invoke-Checked `
    -Label "Verify Termux SSH" `
    -FilePath "ssh" `
    -Arguments @("-p", "$SshPort", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10", "-o", "StrictHostKeyChecking=accept-new", $sshTarget, "echo ssh-ok")

Invoke-Checked `
    -Label "Copy Codex binary to Termux home" `
    -FilePath "scp" `
    -Arguments @("-P", "$SshPort", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10", "-o", "StrictHostKeyChecking=accept-new", $installBinary, "${sshTarget}:$remoteTmp")

Invoke-Checked `
    -Label "Install Codex binary into Termux prefix" `
    -FilePath "ssh" `
    -Arguments @("-p", "$SshPort", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10", "-o", "StrictHostKeyChecking=accept-new", $sshTarget, "install -m 700 $remoteTmp $remoteBin && codex --version && codex exec --help | head")

Write-Host "Codex installed and smoke-tested at $remoteBin"
