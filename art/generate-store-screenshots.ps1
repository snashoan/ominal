param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)

Add-Type -AssemblyName System.Drawing

$sourceRoot = Join-Path $RepositoryRoot "release"
$imageRoot = Join-Path $RepositoryRoot "fastlane\metadata\android\en-US\images"
$phoneRoot = Join-Path $imageRoot "phoneScreenshots"
$sevenRoot = Join-Path $imageRoot "sevenInchScreenshots"
$tenRoot = Join-Path $imageRoot "tenInchScreenshots"
$iconPath = Join-Path $imageRoot "icon.png"

@($phoneRoot, $sevenRoot, $tenRoot) | ForEach-Object {
    New-Item -ItemType Directory -Path $_ -Force | Out-Null
}

$screens = @(
    @{
        Source = "store-v181-runtime.png"
        Title = "Choose your runtime"
        Detail = "Clear publisher attribution, with sign-in handled by the selected command."
        CropY = 0
        CropHeight = 10000
    },
    @{
        Source = "ux-v180-scrolled-3.png"
        Title = "Results stay in chat"
        Detail = "See the screen, respond when needed, and continue in chat."
        CropY = 0
        CropHeight = 10000
    },
    @{
        Source = "store-v181-working.png"
        Title = "See work as it happens"
        Detail = "A live GIR mark and concise state updates show when intelligence is active."
        CropY = 0
        CropHeight = 10000
    },
    @{
        Source = "store-v181-display.png"
        Title = "A computer inside every chat"
        Detail = "Open files, terminal, settings, and the browser without leaving the conversation."
        CropY = 0
        CropHeight = 10000
    },
    @{
        Source = "store-v181-history.png"
        Title = "Move between workspaces"
        Detail = "Search history, resume sessions, and keep each task contained."
        CropY = 0
        CropHeight = 10000
    }
)

function New-RoundedPath {
    param(
        [System.Drawing.RectangleF]$Bounds,
        [float]$Radius
    )

    $diameter = $Radius * 2
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddArc($Bounds.Left, $Bounds.Top, $diameter, $diameter, 180, 90)
    $path.AddArc($Bounds.Right - $diameter, $Bounds.Top, $diameter, $diameter, 270, 90)
    $path.AddArc($Bounds.Right - $diameter, $Bounds.Bottom - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($Bounds.Left, $Bounds.Bottom - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function Draw-FittedImage {
    param(
        [System.Drawing.Graphics]$Graphics,
        [System.Drawing.Image]$Image,
        [System.Drawing.RectangleF]$Destination,
        [int]$CropY,
        [int]$CropHeight
    )

    $sourceY = [Math]::Max(0, [Math]::Min($CropY, $Image.Height - 1))
    $sourceHeight = [Math]::Max(1, [Math]::Min($CropHeight, $Image.Height - $sourceY))
    $sourceWidth = $Image.Width
    $sourceRatio = $sourceWidth / [double]$sourceHeight
    $destinationRatio = $Destination.Width / [double]$Destination.Height

    if ($sourceRatio -gt $destinationRatio) {
        $visibleWidth = [int]($sourceHeight * $destinationRatio)
        $sourceX = [int](($sourceWidth - $visibleWidth) / 2)
        $source = [System.Drawing.Rectangle]::new($sourceX, $sourceY, $visibleWidth, $sourceHeight)
    } else {
        $visibleHeight = [int]($sourceWidth / $destinationRatio)
        $trim = [int](($sourceHeight - $visibleHeight) / 2)
        $source = [System.Drawing.Rectangle]::new(0, $sourceY + $trim, $sourceWidth, $visibleHeight)
    }

    $Graphics.DrawImage($Image, $Destination, $source, [System.Drawing.GraphicsUnit]::Pixel)
}

function New-StorePanel {
    param(
        [hashtable]$Screen,
        [string]$OutputPath
    )

    $sourcePath = Join-Path $sourceRoot $Screen.Source
    if (-not (Test-Path $sourcePath)) {
        throw "Missing screenshot source: $sourcePath"
    }

    $canvas = [System.Drawing.Bitmap]::new(1080, 1920,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($canvas)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
    $graphics.Clear([System.Drawing.ColorTranslator]::FromHtml("#F6F7F5"))

    $brandFont = [System.Drawing.Font]::new("Segoe UI Semibold", 22,
        [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
    $titleFont = [System.Drawing.Font]::new("Segoe UI Semibold", 62,
        [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
    $detailFont = [System.Drawing.Font]::new("Segoe UI", 31,
        [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
    $ink = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml("#111211"))
    $muted = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml("#5F625F"))

    $icon = [System.Drawing.Image]::FromFile($iconPath)
    $graphics.DrawImage($icon, [System.Drawing.Rectangle]::new(76, 64, 52, 52))
    $graphics.DrawString("GIR", $brandFont, $ink, 145, 76)

    $titleBounds = [System.Drawing.RectangleF]::new(76, 145, 928, 150)
    $detailBounds = [System.Drawing.RectangleF]::new(76, 286, 900, 112)
    $graphics.DrawString($Screen.Title, $titleFont, $ink, $titleBounds)
    $graphics.DrawString($Screen.Detail, $detailFont, $muted, $detailBounds)

    $frame = [System.Drawing.RectangleF]::new(203, 430, 674, 1425)
    $shadowBounds = [System.Drawing.RectangleF]::new(203, 446, 674, 1425)
    $shadowPath = New-RoundedPath -Bounds $shadowBounds -Radius 48
    $shadowBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(30, 0, 0, 0))
    $graphics.FillPath($shadowBrush, $shadowPath)

    $framePath = New-RoundedPath -Bounds $frame -Radius 48
    $frameBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::Black)
    $graphics.FillPath($frameBrush, $framePath)

    $screenBounds = [System.Drawing.RectangleF]::new(217, 444, 646, 1397)
    $screenPath = New-RoundedPath -Bounds $screenBounds -Radius 38
    $previousClip = $graphics.Clip
    $graphics.SetClip($screenPath)
    $source = [System.Drawing.Image]::FromFile($sourcePath)
    Draw-FittedImage -Graphics $graphics -Image $source -Destination $screenBounds `
        -CropY $Screen.CropY -CropHeight $Screen.CropHeight
    $source.Dispose()
    $graphics.Clip = $previousClip

    $canvas.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)

    $screenPath.Dispose()
    $framePath.Dispose()
    $shadowPath.Dispose()
    $shadowBrush.Dispose()
    $frameBrush.Dispose()
    $icon.Dispose()
    $brandFont.Dispose()
    $titleFont.Dispose()
    $detailFont.Dispose()
    $ink.Dispose()
    $muted.Dispose()
    $graphics.Dispose()
    $canvas.Dispose()
}

function Resize-Panel {
    param(
        [string]$InputPath,
        [string]$OutputPath,
        [int]$Width,
        [int]$Height
    )

    $source = [System.Drawing.Image]::FromFile($InputPath)
    $canvas = [System.Drawing.Bitmap]::new($Width, $Height,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($canvas)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.DrawImage($source, 0, 0, $Width, $Height)
    $canvas.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $canvas.Dispose()
    $source.Dispose()
}

for ($index = 0; $index -lt $screens.Count; $index++) {
    $name = "{0}.png" -f ($index + 1)
    $phonePath = Join-Path $phoneRoot $name
    New-StorePanel -Screen $screens[$index] -OutputPath $phonePath
    Copy-Item -LiteralPath $phonePath -Destination (Join-Path $sevenRoot $name) -Force
    Resize-Panel -InputPath $phonePath -OutputPath (Join-Path $tenRoot $name) `
        -Width 1440 -Height 2560
}

Write-Output "Generated $($screens.Count) phone, 7-inch, and 10-inch store screenshots."
