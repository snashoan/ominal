param(
    [int]$WaitMinutes = 30
)

$ErrorActionPreference = "Stop"

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$device = "O7ON59OZEY7LOVQG"

Set-Location -Path $PSScriptRoot
New-Item -ItemType Directory -Force -Path "build-logs" | Out-Null

Write-Host "Building APK..."
.\gradlew.bat assembleDebug

Write-Host "Waiting for ADB device $device..."
$deadline = (Get-Date).AddMinutes($WaitMinutes)
while ((Get-Date) -lt $deadline) {
    $out = & $adb devices -l
    $line = $out | Select-Object -Skip 1 | Where-Object { $_ -match $device } | Select-Object -First 1
    if ($line -match "\bdevice\b") {
        break
    }
    if ($line -match "\bunauthorized\b") {
        Write-Host "Device unauthorized. Unlock phone and allow USB debugging."
    }
    Start-Sleep -Seconds 5
}

$out = & $adb devices -l
$line = $out | Select-Object -Skip 1 | Where-Object { $_ -match $device } | Select-Object -First 1
if ($line -notmatch "\bdevice\b") {
    Write-Host $out
    throw "ADB device not ready."
}

Write-Host "Installing APK..."
& $adb -s $device install -r "app\build\outputs\apk\debug\ominal-app_apt-android-7-debug_arm64-v8a.apk"

Write-Host "Pushing display helper..."
& $adb -s $device push "tools\ominal-display-start.sh" "/data/local/tmp/ominal-display-start"
& $adb -s $device shell run-as com.ominal cp /data/local/tmp/ominal-display-start /data/data/com.ominal/files/usr/bin/ominal-display-start
& $adb -s $device shell run-as com.ominal chmod 700 /data/data/com.ominal/files/usr/bin/ominal-display-start
& $adb -s $device shell run-as com.ominal /data/data/com.ominal/files/usr/bin/env PREFIX=/data/data/com.ominal/files/usr HOME=/data/data/com.ominal/files/home PATH=/data/data/com.ominal/files/usr/bin:/system/bin OMINAL_DISPLAY_GEOMETRY=540x1096x24 /data/data/com.ominal/files/usr/bin/ominal-display-start

Write-Host "Launching Codex..."
& $adb -s $device shell am force-stop com.ominal
& $adb -s $device shell am start -W -n com.ominal/.app.OringutanActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
Start-Sleep -Seconds 5

Write-Host "Capturing screenshot..."
& $adb -s $device exec-out screencap -p > build-logs\ominal-current-installed.png

Write-Host "Done. Screenshot: build-logs\ominal-current-installed.png"
