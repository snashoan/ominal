param(
    [string]$TermuxPackagesDir,
    [string]$TermuxPackagesRemote = "https://github.com/termux/termux-packages.git",
    [string]$Ref = "master",
    [string]$AppPackageName = "com.ominal",
    [string]$Architectures = "aarch64",
    [string]$AdditionalPackages = "",
    [switch]$AllArchitectures,
    [switch]$ForceRebuild
)

$ErrorActionPreference = "Stop"

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
if (-not $TermuxPackagesDir) {
    $TermuxPackagesDir = Join-Path $repoRoot "external\termux-packages"
}
$TermuxPackagesDir = [System.IO.Path]::GetFullPath($TermuxPackagesDir)
$bootstrapDir = Join-Path $repoRoot "app\src\main\cpp"
$manifestPath = Join-Path $bootstrapDir "bootstrap-ominal.sha256"

if ($AppPackageName -notmatch "^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$") {
    throw "Invalid Android package name: $AppPackageName"
}

if ($AllArchitectures) {
    $Architectures = "aarch64,arm,i686,x86_64"
}

function Invoke-Git {
    param(
        [string[]]$Arguments,
        [string]$WorkingDirectory = $repoRoot
    )
    & git @Arguments 2>&1 | Write-Host
    if ($LASTEXITCODE -ne 0) {
        throw "git $($Arguments -join ' ') failed with exit code $LASTEXITCODE"
    }
}

function Invoke-BashInDirectory {
    param(
        [string]$Directory,
        [string]$Command
    )

    $isWindowsHost = [System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform(
        [System.Runtime.InteropServices.OSPlatform]::Windows)
    $wsl = Get-Command wsl -ErrorAction SilentlyContinue
    if ($isWindowsHost -and $wsl) {
        $wslDir = (& $wsl.Source wslpath -a $Directory).Trim()
        & $wsl.Source bash -lc "cd '$wslDir' && $Command"
        if ($LASTEXITCODE -ne 0) {
            throw "wsl bash command failed with exit code $LASTEXITCODE"
        }
        return
    }

    $bash = Get-Command bash -ErrorAction SilentlyContinue
    if ($bash) {
        Push-Location -LiteralPath $Directory
        try {
            & $bash.Source -lc $Command
            if ($LASTEXITCODE -ne 0) {
                throw "bash command failed with exit code $LASTEXITCODE"
            }
        } finally {
            Pop-Location
        }
        return
    }

    if ($wsl) {
        $wslDir = (& $wsl.Source wslpath -a $Directory).Trim()
        & $wsl.Source bash -lc "cd '$wslDir' && $Command"
        if ($LASTEXITCODE -ne 0) {
            throw "wsl bash command failed with exit code $LASTEXITCODE"
        }
        return
    }

    throw "No bash or wsl executable found. The termux-packages bootstrap builder requires a bash + Docker environment."
}

function Replace-PropertyAssignment {
    param(
        [string]$Path,
        [string]$Name,
        [string]$Value
    )

    $text = Get-Content -LiteralPath $Path -Raw
    $pattern = "$Name=`"[^`"]*`""
    $replacement = "$Name=`"$Value`""
    $updated = [regex]::Replace($text, $pattern, $replacement, 1)
    if ($updated -eq $text) {
        throw "Could not find property assignment for $Name in $Path"
    }
    Set-Content -LiteralPath $Path -Value $updated -NoNewline
}

if (-not (Test-Path -LiteralPath $TermuxPackagesDir)) {
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $TermuxPackagesDir) | Out-Null
    Invoke-Git -Arguments @("clone", $TermuxPackagesRemote, $TermuxPackagesDir)
}

Invoke-Git -Arguments @("-C", $TermuxPackagesDir, "fetch", "--all", "--tags")
Invoke-Git -Arguments @("-C", $TermuxPackagesDir, "checkout", $Ref)

$propertiesPath = Join-Path $TermuxPackagesDir "scripts\properties.sh"
if (-not (Test-Path -LiteralPath $propertiesPath)) {
    throw "termux-packages scripts/properties.sh not found at $propertiesPath"
}

Replace-PropertyAssignment -Path $propertiesPath -Name "TERMUX__NAME" -Value "Ominal"
Replace-PropertyAssignment -Path $propertiesPath -Name "TERMUX_APP__PACKAGE_NAME" -Value $AppPackageName

$buildArgs = "./scripts/build-bootstraps.sh --architectures $Architectures"
if ($AdditionalPackages) {
    $buildArgs += " --add $AdditionalPackages"
}
if ($ForceRebuild) {
    $buildArgs += " -f"
}

Invoke-BashInDirectory -Directory $TermuxPackagesDir -Command "./scripts/run-docker.sh $buildArgs"

$expectedArchitectures = $Architectures.Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
foreach ($arch in $expectedArchitectures) {
    $builtArchive = Join-Path $TermuxPackagesDir "bootstrap-$arch.zip"
    if (-not (Test-Path -LiteralPath $builtArchive)) {
        throw "Expected bootstrap archive was not produced: $builtArchive"
    }
    Copy-Item -LiteralPath $builtArchive -Destination (Join-Path $bootstrapDir "bootstrap-$arch.zip") -Force
}

$manifestLines = foreach ($archive in Get-ChildItem -LiteralPath $bootstrapDir -Filter "bootstrap-*.zip" | Sort-Object Name) {
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $archive.FullName).Hash.ToLowerInvariant()
    "$hash  $($archive.Name)"
}
Set-Content -LiteralPath $manifestPath -Value $manifestLines

Write-Host "Ominal bootstrap archives copied to $bootstrapDir"
Write-Host "Checksum manifest written to $manifestPath"
