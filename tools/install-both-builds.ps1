param(
    [string]$Device = ""
)

$ErrorActionPreference = "Stop"

$repo = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$ominalApk = Join-Path $repo "app\build\outputs\apk\debug\termux-app_apt-android-7-debug_arm64-v8a.apk"
$chatShellApk = Join-Path $repo "external-apks\gptmobile-fdroid.apk"

if (!(Test-Path $adb)) {
    throw "adb not found at $adb"
}
if (!(Test-Path $ominalApk)) {
    throw "Ominal APK not found. Run .\gradlew.bat assembleDebug first."
}
if (!(Test-Path $chatShellApk)) {
    throw "GPTMobile APK not found at $chatShellApk"
}

if (!$Device) {
    $deviceLines = & $adb devices -l
    $deviceLine = $deviceLines | Select-String -Pattern "`tdevice$" | Select-Object -First 1
    if (!$deviceLine) {
        $unauthorized = $deviceLines | Select-String -Pattern "`tunauthorized$"
        if ($unauthorized) {
            Write-Host "ADB sees an unauthorized device. Unlock the phone and accept the USB debugging prompt, then rerun this script."
        } else {
            Write-Host "No authorized ADB device connected."
        }
        & $adb devices -l
        exit 3
    }
    $Device = ($deviceLine.ToString() -split "\s+")[0]
}

Write-Host "Installing Ominal on $Device"
& $adb -s $Device install -r $ominalApk

Write-Host "Installing GPTMobile shell candidate on $Device"
& $adb -s $Device install -r $chatShellApk

Write-Host "Installed packages:"
& $adb -s $Device shell pm list packages | Select-String -Pattern "com.ominal|dev.chungjungsoo.gptmobile|com.openai.chatgpt"

Write-Host "Launching Ominal"
& $adb -s $Device shell am start -W -n com.ominal/.app.OringutanActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
