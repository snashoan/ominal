param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$Inkscape = 'C:\Program Files\Inkscape\bin\inkscape.com'
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $Inkscape)) {
    throw "Inkscape was not found at $Inkscape"
}

function Export-Png([string]$Source, [string]$Destination, [int]$Width, [int]$Height) {
    & $Inkscape $Source --export-filename=$Destination --export-width=$Width `
        --export-height=$Height --export-background-opacity=0
    if ($LASTEXITCODE -ne 0) {
        throw "Inkscape failed to export $Source"
    }
}

$blackSketch = Join-Path $ProjectRoot 'art\gir-logo-sketch-black.svg'
$whiteSketch = Join-Path $ProjectRoot 'art\gir-logo-sketch-white.svg'
$orbit = Join-Path $ProjectRoot 'docs\brand\experimental\gir-experiment-orbit-cut.svg'
$wallpaper = Join-Path $ProjectRoot 'art\gir-wallpaper-cyan.svg'
$drawableDir = Join-Path $ProjectRoot 'app\src\main\res\drawable-nodpi'

Export-Png $blackSketch (Join-Path $drawableDir 'gir_final_logo.png') 512 512
Export-Png $whiteSketch (Join-Path $drawableDir 'gir_final_logo_white.png') 512 512
Export-Png $orbit (Join-Path $ProjectRoot 'docs\brand\gir-prod-orbit.png') 512 512
Export-Png $wallpaper (Join-Path $ProjectRoot `
    'app\src\main\assets\runtime\gir-final-wallpaper.png') 1080 2400

& (Join-Path $PSScriptRoot 'generate-transparent-launcher.ps1') -ProjectRoot $ProjectRoot

Copy-Item (Join-Path $drawableDir 'gir_final_logo.png') `
    (Join-Path $ProjectRoot 'docs\brand\gir-prod-logo.png') -Force
Copy-Item (Join-Path $drawableDir 'gir_final_logo_white.png') `
    (Join-Path $ProjectRoot 'docs\brand\gir-prod-logo-white.png') -Force
Copy-Item (Join-Path $drawableDir 'gir_launcher_foreground.png') `
    (Join-Path $ProjectRoot 'docs\brand\gir-prod-launcher-foreground.png') -Force
