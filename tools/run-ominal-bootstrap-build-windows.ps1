param(
    [string]$WslUser = 'root',
    [string]$AppDir = '/mnt/c/Users/saura/skynet/termux-app',
    [string]$TermuxPackagesDir = '/root/ominal/termux-packages-ominal',
    [string]$Architectures = 'aarch64',
    [string]$BuildLog = '/root/ominal/bootstrap-build.log',
    [ValidateRange(1, 64)]
    [int]$BuildJobs = 4,
    [switch]$Resume
)

$ErrorActionPreference = 'Stop'

$windowsLog = Join-Path (Join-Path $PSScriptRoot '..\build-logs') 'ominal-bootstrap-windows.log'
function Write-RunnerMilestone([string]$Message) {
    $timestamp = Get-Date -Format 'o'
    Add-Content -LiteralPath $windowsLog -Value "$timestamp $Message" -Encoding UTF8
}

Write-RunnerMilestone "START resume=$($Resume.IsPresent) jobs=$BuildJobs source=$TermuxPackagesDir"
$linuxRunner = "$AppDir/tools/run-ominal-bootstrap-build-wsl.sh"
$forceRebuild = if ($Resume) { '0' } else { '1' }
$bashArgs = @($linuxRunner, $AppDir, $TermuxPackagesDir, $Architectures, $BuildLog) |
    ForEach-Object { "'$_'" }
$bashCommand = "exec env OMINAL_BOOTSTRAP_BUILD_JOBS='$BuildJobs' " +
    "OMINAL_BOOTSTRAP_FORCE_REBUILD='$forceRebuild' bash " + ($bashArgs -join ' ')

Write-RunnerMilestone 'SPAWN_WSL'
& wsl.exe -u $WslUser -- bash -lc $bashCommand
Write-RunnerMilestone "WSL_EXIT code=$LASTEXITCODE"
if ($LASTEXITCODE -ne 0) {
    throw "Ominal bootstrap build failed with WSL exit code $LASTEXITCODE."
}
