param(
    [string]$PrinterName = "EPSON L3210 Series",
    [string]$ChatScreenshot = "build-logs\ominal-native-surface-chat.png",
    [string]$HistoryScreenshot = "build-logs\ominal-native-surface-history.png",
    [string]$DisplayScreenshot = "build-logs\ominal-phone-desktop-display2.png",
    [string]$OutputImage = "build-logs\ominal-whole-ui-experience-print.png",
    [string]$LogPath = "build-logs\ominal-whole-ui-experience-print.log"
)

$ErrorActionPreference = "Stop"

function Write-PrintLog([string]$Message) {
    $stamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Add-Content -Path $LogPath -Value "$stamp $Message"
}

function Resolve-OutputPath([string]$Path) {
    $parent = Split-Path $Path
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
    return Join-Path (Resolve-Path $parent).Path (Split-Path $Path -Leaf)
}

New-Item -ItemType Directory -Force -Path (Split-Path $LogPath) | Out-Null
Write-PrintLog "starting whole UI print helper"

$chatPath = (Resolve-Path $ChatScreenshot).Path
$historyPath = (Resolve-Path $HistoryScreenshot).Path
$displayPath = (Resolve-Path $DisplayScreenshot).Path
$outputPath = Resolve-OutputPath $OutputImage

Add-Type -AssemblyName System.Drawing

$canvasW = 3508
$canvasH = 2480
$bmp = New-Object System.Drawing.Bitmap $canvasW, $canvasH
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$g.Clear([System.Drawing.Color]::White)

$titleFont = New-Object System.Drawing.Font("Segoe UI", 46, [System.Drawing.FontStyle]::Bold)
$labelFont = New-Object System.Drawing.Font("Segoe UI", 26, [System.Drawing.FontStyle]::Bold)
$metaFont = New-Object System.Drawing.Font("Segoe UI", 16, [System.Drawing.FontStyle]::Regular)
$titleBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(20, 20, 22))
$labelBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(42, 42, 46))
$metaBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(96, 96, 102))

$g.DrawString("Ominal UI experience", $titleFont, $titleBrush, 120, 70)
$g.DrawString("native chat surface + diffused history + phone-shaped Linux display", $metaFont, $metaBrush, 124, 135)

function Draw-FitImage($Graphics, $Image, [int]$X, [int]$Y, [int]$W, [int]$H) {
    $scale = [Math]::Min($W / $Image.Width, $H / $Image.Height)
    $dw = [int]($Image.Width * $scale)
    $dh = [int]($Image.Height * $scale)
    $dx = [int]($X + (($W - $dw) / 2))
    $dy = [int]($Y + (($H - $dh) / 2))
    $shadow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(36, 0, 0, 0))
    $Graphics.FillRectangle($shadow, $dx + 12, $dy + 16, $dw, $dh)
    $shadow.Dispose()
    $Graphics.FillRectangle([System.Drawing.Brushes]::Black, $dx, $dy, $dw, $dh)
    $Graphics.DrawImage($Image, $dx, $dy, $dw, $dh)
}

$panels = @(
    @{ Label = "Chat"; Path = $chatPath; X = 120 },
    @{ Label = "History"; Path = $historyPath; X = 1254 },
    @{ Label = "Display"; Path = $displayPath; X = 2388 }
)

foreach ($panel in $panels) {
    $g.DrawString($panel.Label, $labelFont, $labelBrush, [int]$panel.X, 205)
    $img = [System.Drawing.Image]::FromFile($panel.Path)
    Draw-FitImage $g $img ([int]$panel.X) 270 1000 2040
    $img.Dispose()
}

$titleFont.Dispose()
$labelFont.Dispose()
$metaFont.Dispose()
$titleBrush.Dispose()
$labelBrush.Dispose()
$metaBrush.Dispose()
$g.Dispose()
$bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-PrintLog "created landscape page $outputPath"

$printImage = [System.Drawing.Image]::FromFile($outputPath)
$doc = New-Object System.Drawing.Printing.PrintDocument
$doc.DocumentName = "Ominal whole UI experience"
$doc.PrinterSettings.PrinterName = $PrinterName
$doc.DefaultPageSettings.Landscape = $true
$doc.add_PrintPage({
    param($sender, $e)
    $bounds = $e.MarginBounds
    $scale = [Math]::Min($bounds.Width / $printImage.Width, $bounds.Height / $printImage.Height)
    $dw = [int]($printImage.Width * $scale)
    $dh = [int]($printImage.Height * $scale)
    $x = [int]($bounds.Left + (($bounds.Width - $dw) / 2))
    $y = [int]($bounds.Top + (($bounds.Height - $dh) / 2))
    $e.Graphics.DrawImage($printImage, $x, $y, $dw, $dh)
    $e.HasMorePages = $false
})

$doc.Print()
$printImage.Dispose()
$doc.Dispose()
Write-PrintLog "submitted to printer $PrinterName"
