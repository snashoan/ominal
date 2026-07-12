param(
    [string]$PrinterName = "EPSON L3210 Series",
    [string]$ChatScreenshot = "build-logs\ominal-native-surface-chat.png",
    [string]$HistoryScreenshot = "build-logs\ominal-native-surface-history.png",
    [string]$DisplayScreenshot = "build-logs\ominal-phone-desktop-display2.png",
    [string]$OutputImage = "build-logs\ominal-whole-ui-experience-print-lite.jpg",
    [string]$LogPath = "build-logs\ominal-whole-ui-experience-print-lite.log"
)

$ErrorActionPreference = "Stop"

function Write-PrintLog([string]$Message) {
    $stamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Add-Content -Path $LogPath -Value "$stamp $Message"
}

New-Item -ItemType Directory -Force -Path (Split-Path $LogPath) | Out-Null
Write-PrintLog "starting lite print helper"

Add-Type -AssemblyName System.Drawing

$canvasW = 1754
$canvasH = 1240
$bmp = New-Object System.Drawing.Bitmap $canvasW, $canvasH
$bmp.SetResolution(150, 150)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.Clear([System.Drawing.Color]::White)

$titleFont = New-Object System.Drawing.Font("Segoe UI", 24, [System.Drawing.FontStyle]::Bold)
$labelFont = New-Object System.Drawing.Font("Segoe UI", 16, [System.Drawing.FontStyle]::Bold)
$brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(20, 20, 22))
$g.DrawString("Ominal UI experience", $titleFont, $brush, 60, 35)

function Draw-FitImage($Graphics, $Image, [int]$X, [int]$Y, [int]$W, [int]$H) {
    $scale = [Math]::Min($W / $Image.Width, $H / $Image.Height)
    $dw = [int]($Image.Width * $scale)
    $dh = [int]($Image.Height * $scale)
    $dx = [int]($X + (($W - $dw) / 2))
    $dy = [int]($Y + (($H - $dh) / 2))
    $Graphics.FillRectangle([System.Drawing.Brushes]::Black, $dx, $dy, $dw, $dh)
    $Graphics.DrawImage($Image, $dx, $dy, $dw, $dh)
}

$items = @(
    @{Label="Chat"; Path=$ChatScreenshot; X=60},
    @{Label="History"; Path=$HistoryScreenshot; X=625},
    @{Label="Display"; Path=$DisplayScreenshot; X=1190}
)

foreach ($item in $items) {
    $g.DrawString($item.Label, $labelFont, $brush, [int]$item.X, 105)
    $image = [System.Drawing.Image]::FromFile((Resolve-Path $item.Path).Path)
    Draw-FitImage $g $image ([int]$item.X) 145 500 1010
    $image.Dispose()
}

$titleFont.Dispose()
$labelFont.Dispose()
$brush.Dispose()
$g.Dispose()

$outputPath = Join-Path (Resolve-Path (Split-Path $OutputImage)).Path (Split-Path $OutputImage -Leaf)
$encoder = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq "image/jpeg" }
$params = New-Object System.Drawing.Imaging.EncoderParameters 1
$params.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality), 85L
$bmp.Save($outputPath, $encoder, $params)
$bmp.Dispose()
Write-PrintLog "created lite page $outputPath"

$printImage = [System.Drawing.Image]::FromFile($outputPath)
$doc = New-Object System.Drawing.Printing.PrintDocument
$doc.DocumentName = "Ominal whole UI lite"
$doc.PrinterSettings.PrinterName = $PrinterName
$doc.DefaultPageSettings.Landscape = $true
$doc.PrintController = New-Object System.Drawing.Printing.StandardPrintController
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
Write-PrintLog "submitted lite job to $PrinterName"
