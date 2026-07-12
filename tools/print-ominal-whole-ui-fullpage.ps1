param(
    [string]$PrinterName = "EPSON L3210 Series",
    [string]$ChatScreenshot = "build-logs\ominal-native-surface-chat.png",
    [string]$HistoryScreenshot = "build-logs\ominal-native-surface-history.png",
    [string]$DisplayScreenshot = "build-logs\ominal-phone-desktop-display2.png",
    [string]$OutputImage = "build-logs\ominal-whole-ui-experience-fullpage.jpg",
    [string]$LogPath = "build-logs\ominal-whole-ui-experience-fullpage.log"
)

$ErrorActionPreference = "Stop"

function Write-PrintLog([string]$Message) {
    $stamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Add-Content -Path $LogPath -Value "$stamp $Message"
}

function Draw-FillPanel($Graphics, $Image, [int]$X, [int]$Y, [int]$W, [int]$H) {
    $scale = [Math]::Max($W / $Image.Width, $H / $Image.Height)
    $dw = [int]($Image.Width * $scale)
    $dh = [int]($Image.Height * $scale)
    $dx = [int]($X + (($W - $dw) / 2))
    $dy = [int]($Y + (($H - $dh) / 2))
    $oldClip = $Graphics.Clip
    $Graphics.SetClip((New-Object System.Drawing.Rectangle $X, $Y, $W, $H))
    $Graphics.FillRectangle([System.Drawing.Brushes]::Black, $X, $Y, $W, $H)
    $Graphics.DrawImage($Image, $dx, $dy, $dw, $dh)
    $Graphics.Clip = $oldClip
}

New-Item -ItemType Directory -Force -Path (Split-Path $LogPath) | Out-Null
Write-PrintLog "starting fullpage print helper"

Add-Type -AssemblyName System.Drawing

$canvasW = 2400
$canvasH = 1700
$gap = 18
$panelW = [int](($canvasW - ($gap * 2)) / 3)
$bmp = New-Object System.Drawing.Bitmap $canvasW, $canvasH
$bmp.SetResolution(200, 200)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$g.Clear([System.Drawing.Color]::Black)

$paths = @(
    (Resolve-Path $ChatScreenshot).Path,
    (Resolve-Path $HistoryScreenshot).Path,
    (Resolve-Path $DisplayScreenshot).Path
)

for ($i = 0; $i -lt 3; $i++) {
    $image = [System.Drawing.Image]::FromFile($paths[$i])
    $x = $i * ($panelW + $gap)
    Draw-FillPanel $g $image $x 0 $panelW $canvasH
    $image.Dispose()
}

$g.Dispose()

$outputPath = Join-Path (Resolve-Path (Split-Path $OutputImage)).Path (Split-Path $OutputImage -Leaf)
$encoder = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq "image/jpeg" }
$params = New-Object System.Drawing.Imaging.EncoderParameters 1
$params.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality), 90L
$bmp.Save($outputPath, $encoder, $params)
$bmp.Dispose()
Write-PrintLog "created fullpage sheet $outputPath"

$printImage = [System.Drawing.Image]::FromFile($outputPath)
$doc = New-Object System.Drawing.Printing.PrintDocument
$doc.DocumentName = "Ominal whole UI fullpage"
$doc.PrinterSettings.PrinterName = $PrinterName
$doc.DefaultPageSettings.Landscape = $true
$doc.DefaultPageSettings.Margins = New-Object System.Drawing.Printing.Margins 0, 0, 0, 0
$doc.OriginAtMargins = $false
$doc.PrintController = New-Object System.Drawing.Printing.StandardPrintController
$doc.add_PrintPage({
    param($sender, $e)
    $e.Graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $e.Graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $e.Graphics.TranslateTransform(-$e.PageSettings.HardMarginX, -$e.PageSettings.HardMarginY)
    $page = $e.PageBounds
    $scale = [Math]::Max($page.Width / $printImage.Width, $page.Height / $printImage.Height)
    $dw = [int]($printImage.Width * $scale)
    $dh = [int]($printImage.Height * $scale)
    $x = [int](($page.Width - $dw) / 2)
    $y = [int](($page.Height - $dh) / 2)
    $e.Graphics.DrawImage($printImage, $x, $y, $dw, $dh)
    $e.HasMorePages = $false
})

$doc.Print()
$printImage.Dispose()
$doc.Dispose()
Write-PrintLog "submitted fullpage job to $PrinterName"
