param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$sourcePath = Join-Path $ProjectRoot 'fastlane\metadata\android\en-US\images\icon.png'
$sourceIcon = [System.Drawing.Image]::FromFile($sourcePath)

function Write-TransparentIcon([int]$Size, [string]$Destination) {
    $bitmap = [System.Drawing.Bitmap]::new(
        $Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.Color]::FromArgb(255, 5, 5, 6))
        $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

        $graphics.DrawImage($sourceIcon, 0, 0, $Size, $Size)
        $bitmap.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

try {
    $densities = [ordered]@{
        mdpi = 48
        hdpi = 72
        xhdpi = 96
        xxhdpi = 144
        xxxhdpi = 192
    }
    foreach ($entry in $densities.GetEnumerator()) {
        $directory = Join-Path $ProjectRoot "app\src\main\res\mipmap-$($entry.Key)"
        foreach ($name in @('ic_launcher', 'ic_launcher_round',
                'ic_launcher_dark', 'ic_launcher_round_dark')) {
            Write-TransparentIcon $entry.Value (Join-Path $directory "$name.png")
        }
    }
    Write-TransparentIcon 1254 (Join-Path $ProjectRoot `
        'app\src\main\res\drawable-nodpi\gir_launcher_foreground.png')
} finally {
    $sourceIcon.Dispose()
}
