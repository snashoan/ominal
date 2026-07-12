param(
    [string]$Workspace = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path,
    [string]$WslUser = 'root',
    [string]$AppDir = '/mnt/c/Users/saura/skynet/termux-app',
    [string]$TermuxPackagesDir = '/root/ominal/termux-packages',
    [string]$Architectures = 'aarch64,arm,i686,x86_64',
    [string]$BuildLog = '/root/ominal/bootstrap-build.log'
)

$ErrorActionPreference = 'Stop'

$stateDir = Join-Path $Workspace 'build-logs'
$statusFile = Join-Path $stateDir 'ominal-bootstrap-status.txt'
$hostPidFile = Join-Path $stateDir 'ominal-bootstrap-wsl-host.pid'
$stdoutLog = Join-Path $stateDir 'ominal-bootstrap-wsl-host.out.log'
$stderrLog = Join-Path $stateDir 'ominal-bootstrap-wsl-host.err.log'

New-Item -ItemType Directory -Force -Path $stateDir | Out-Null

if (Test-Path -LiteralPath $hostPidFile) {
    $oldPidText = (Get-Content -LiteralPath $hostPidFile -Raw).Trim()
    $oldPid = 0
    if ([int]::TryParse($oldPidText, [ref]$oldPid)) {
        $existing = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
        if ($existing) {
            Write-Output "BOOTSTRAP_ALREADY_ACTIVE HOST_PID=$oldPid"
            Write-Output "STATUS=$statusFile"
            Write-Output "WSL_LOG=$BuildLog"
            exit 0
        }
    }
}

Set-Content -LiteralPath $statusFile -Value 'starting' -Encoding UTF8

$runner = "$AppDir/tools/run-ominal-bootstrap-build-wsl.sh"
$args = @('-u', $WslUser, 'bash', $runner, $AppDir, $TermuxPackagesDir, $Architectures, $BuildLog)

$process = Start-Process `
    -FilePath 'wsl.exe' `
    -ArgumentList $args `
    -WindowStyle Hidden `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -PassThru

Set-Content -LiteralPath $hostPidFile -Value $process.Id -Encoding UTF8

Write-Output "BOOTSTRAP_STARTED HOST_PID=$($process.Id)"
Write-Output "STATUS=$statusFile"
Write-Output "WSL_LOG=$BuildLog"
Write-Output "HOST_STDOUT=$stdoutLog"
Write-Output "HOST_STDERR=$stderrLog"
