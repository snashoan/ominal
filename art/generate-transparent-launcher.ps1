param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$blackPath = Join-Path $ProjectRoot 'app\src\main\res\drawable-nodpi\gir_final_logo.png'
$whitePath = Join-Path $ProjectRoot 'app\src\main\res\drawable-nodpi\gir_final_logo_white.png'
$blackMark = [System.Drawing.Image]::FromFile($blackPath)
$whiteMark = [System.Drawing.Image]::FromFile($whitePath)

function Write-TransparentIcon([int]$Size, [string]$Destination) {
    $bitmap = [System.Drawing.Bitmap]::new(
        $Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceOver
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

        $outlineMargin = [int][Math]::Round($Size * 8 / 108)
        $markMargin = [int][Math]::Round($Size * 10 / 108)
        $graphics.DrawImage($blackMark, $outlineMargin, $outlineMargin,
            $Size - (2 * $outlineMargin), $Size - (2 * $outlineMargin))
        $graphics.DrawImage($whiteMark, $markMargin, $markMargin,
            $Size - (2 * $markMargin), $Size - (2 * $markMargin))
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
    $blackMark.Dispose()
    $whiteMark.Dispose()
}
