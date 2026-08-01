[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern("^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?$")]
    [string]$VersionName,

    [Parameter(Mandatory = $true)]
    [ValidateRange(1, 2100000000)]
    [int]$VersionCode,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$Changelog
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$versionFile = Join-Path $repoRoot "version.properties"
$changelogDirectory = Join-Path $repoRoot "fastlane\metadata\android\en-US\changelogs"
$current = Get-Content -LiteralPath $versionFile -Raw | ConvertFrom-StringData
$currentCode = [int]$current.versionCode

if ($VersionCode -le $currentCode) {
    throw "Version code must increase beyond $currentCode."
}

$highestChangelog = Get-ChildItem -LiteralPath $changelogDirectory -Filter "*.txt" -File |
    Where-Object { $_.BaseName -match "^\d+$" } |
    ForEach-Object { [int]$_.BaseName } |
    Measure-Object -Maximum |
    Select-Object -ExpandProperty Maximum
if ($highestChangelog -and $VersionCode -le $highestChangelog) {
    throw "Version code must increase beyond recorded changelog $highestChangelog."
}

$cleanChangelog = $Changelog.Trim()
if ($cleanChangelog.Length -gt 500) {
    throw "Google Play changelogs must be at most 500 characters."
}

$changelogFile = Join-Path $changelogDirectory "$VersionCode.txt"
if (Test-Path -LiteralPath $changelogFile) {
    throw "Changelog already exists: $changelogFile"
}

$versionTemporary = "$versionFile.tmp"
$changelogTemporary = "$changelogFile.tmp"
try {
    Set-Content -LiteralPath $versionTemporary -Encoding ASCII -NoNewline `
        -Value "versionName=$VersionName`nversionCode=$VersionCode`n"
    Set-Content -LiteralPath $changelogTemporary -Encoding UTF8 -NoNewline `
        -Value "$cleanChangelog`n"
    Move-Item -LiteralPath $versionTemporary -Destination $versionFile -Force
    Move-Item -LiteralPath $changelogTemporary -Destination $changelogFile
} finally {
    Remove-Item -LiteralPath $versionTemporary -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $changelogTemporary -Force -ErrorAction SilentlyContinue
}

Write-Host "Version advanced to $VersionName ($VersionCode)."
Write-Host "Changelog: $changelogFile"
