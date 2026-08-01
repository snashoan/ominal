param(
    [string]$TermuxPackagesDir = '/root/ominal/termux-packages-ominal',
    [string]$Architectures = "aarch64",
    [switch]$AllArchitectures,
    [string]$BuildLog = "/root/ominal/bootstrap-build.log",
    [ValidateRange(1, 64)]
    [int]$BuildJobs = 4,
    [switch]$Resume
)

$ErrorActionPreference = "Stop"

if ($AllArchitectures) {
    $Architectures = "aarch64,arm,i686,x86_64"
}

$runner = Join-Path $PSScriptRoot 'run-ominal-bootstrap-build-windows.ps1'
& $runner `
    -TermuxPackagesDir $TermuxPackagesDir `
    -Architectures $Architectures `
    -BuildLog $BuildLog `
    -BuildJobs $BuildJobs `
    -Resume:$Resume
