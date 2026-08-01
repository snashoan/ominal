$ErrorActionPreference = "Stop"

$repo = Split-Path -Parent $PSScriptRoot
$library = Join-Path $repo "app\src\main\jniLibs\arm64-v8a\libominal-display.so"
$expectedHash = "129049EFB9B6FA3F18AF826B9D2C6720289A51956BFCE1BF59B2569BF96A7879"

if (-not (Test-Path -LiteralPath $library)) {
    throw "Native display library is missing: $library"
}

$actualHash = (Get-FileHash -LiteralPath $library -Algorithm SHA256).Hash
if ($actualHash -ne $expectedHash) {
    throw "Native display SHA-256 mismatch: expected $expectedHash, actual $actualHash"
}

$text = [Text.Encoding]::ASCII.GetString([IO.File]::ReadAllBytes($library))
foreach ($forbidden in @("com.termux.x11", "com/termux/x11", "/data/data/com.termux", "TERMUX_X11", "termux-x11")) {
    if ($text.Contains($forbidden)) {
        throw "Native display still contains forbidden runtime identity: $forbidden"
    }
}

$abiSymbol = "Java_com_termux_x11_CmdEntryPoint_start"
if (-not $text.Contains($abiSymbol)) {
    throw "Native display command-entry ABI is missing: $abiSymbol"
}

Write-Output "Ominal native display dependency verified: $actualHash"
