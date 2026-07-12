param(
    [string]$ApkPath = ""
)

$ErrorActionPreference = "Stop"

$repo = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$aapt = Get-ChildItem -Path (Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools") -Recurse -Filter aapt.exe -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName

$targets = @(
    [pscustomobject]@{ Package = "com.openai.chatgpt"; Label = "ChatGPT" },
    [pscustomobject]@{ Package = "dev.chungjungsoo.gptmobile"; Label = "GPTMobile" }
)

Write-Host "Bridge targets"
foreach ($target in $targets) {
    Write-Host ("- {0}: {1}" -f $target.Label, $target.Package)
}

if ($ApkPath) {
    if (!(Test-Path $ApkPath)) {
        throw "APK not found: $ApkPath"
    }
    if (!$aapt) {
        throw "aapt.exe not found under $env:LOCALAPPDATA\Android\Sdk\build-tools"
    }

    Write-Host ""
    Write-Host "APK metadata"
    & $aapt dump badging $ApkPath | Select-String -Pattern "package:|launchable-activity:|sdkVersion:|targetSdkVersion:|uses-permission:"
}

if (Test-Path $adb) {
    Write-Host ""
    Write-Host "ADB packages"
    $deviceLines = & $adb devices -l
    $authorized = $deviceLines | Select-String -Pattern "`tdevice$" | Select-Object -First 1
    if ($authorized) {
        $device = ($authorized.ToString() -split "\s+")[0]
        Write-Host "Device: $device"
        $packages = & $adb -s $device shell pm list packages
        foreach ($target in $targets) {
            $present = $packages | Select-String -SimpleMatch $target.Package
            Write-Host ("- {0}: {1}" -f $target.Package, $(if ($present) { "installed" } else { "missing" }))
        }
    } else {
        $unauthorized = $deviceLines | Select-String -Pattern "`tunauthorized$" | Select-Object -First 1
        if ($unauthorized) {
            Write-Host "ADB device is unauthorized. Unlock the phone and accept the USB debugging prompt."
        } else {
            Write-Host "No ADB device connected."
        }
    }
} else {
    Write-Host ""
    Write-Host "adb.exe not found."
}
