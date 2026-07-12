param(
    [string]$PrinterName = "EPSON L3210 Series",
    [string]$ChatScreenshot = "build-logs\ominal-ios-director-final-chat.png",
    [string]$DisplayScreenshot = "build-logs\ominal-ios-director-final-display-reopened-wait.png",
    [string]$OutputImage = "build-logs\ominal-current-ui-print.png",
    [string]$LogPath = "build-logs\ominal-current-ui-print.log"
)

$ErrorActionPreference = "Stop"

function Write-PrintLog([string]$Message) {
    $stamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Add-Content -Path $LogPath -Value "$stamp $Message"
}

New-Item -ItemType Directory -Force -Path (Split-Path $LogPath) | Out-Null
Write-PrintLog "starting print helper"

$chatPath = (Resolve-Path $ChatScreenshot).Path
$displayPath = (Resolve-Path $DisplayScreenshot).Path
$outputPath = Join-Path (Resolve-Path (Split-Path $OutputImage)).Path (Split-Path $OutputImage -Leaf)

Add-Type -AssemblyName System.Drawing

$canvasW = 2480
$canvasH = 3508
$bmp = New-Object System.Drawing.Bitmap $canvasW, $canvasH
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.Clear([System.Drawing.Color]::White)

$titleFont = New-Object System.Drawing.Font("Segoe UI", 46, [System.Drawing.FontStyle]::Bold)
$titleBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(24, 24, 27))
$g.DrawString("Ominal current UI", $titleFont, $titleBrush, 150, 110)

function Draw-FitImage($Graphics, $Image, [int]$X, [int]$Y, [int]$W, [int]$H) {
    $scale = [Math]::Min($W / $Image.Width, $H / $Image.Height)
    $dw = [int]($Image.Width * $scale)
    $dh = [int]($Image.Height * $scale)
    $dx = [int]($X + (($W - $dw) / 2))
    $dy = [int]($Y + (($H - $dh) / 2))
    $Graphics.FillRectangle([System.Drawing.Brushes]::Black, $dx, $dy, $dw, $dh)
    $Graphics.DrawImage($Image, $dx, $dy, $dw, $dh)
}

$chatImage = [System.Drawing.Image]::FromFile($chatPath)
$displayImage = [System.Drawing.Image]::FromFile($displayPath)
Draw-FitImage $g $chatImage 180 260 960 3000
Draw-FitImage $g $displayImage 1340 260 960 3000
$chatImage.Dispose()
$displayImage.Dispose()
$titleFont.Dispose()
$titleBrush.Dispose()
$g.Dispose()
$bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-PrintLog "created page $outputPath"

$printImage = [System.Drawing.Image]::FromFile($outputPath)
$doc = New-Object System.Drawing.Printing.PrintDocument
$doc.DocumentName = "Ominal current UI"
$doc.PrinterSettings.PrinterName = $PrinterName
$doc.DefaultPageSettings.Landscape = $false
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
