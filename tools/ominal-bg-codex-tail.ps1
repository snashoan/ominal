param(
    [string]$Workspace = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path,
    [int]$Lines = 80
)

$stateDir = Join-Path $Workspace 'build-logs\codex-bg'
$pidFile = Join-Path $stateDir 'runner.pid'
$log = Join-Path $stateDir 'session.log'

if (Test-Path -LiteralPath $pidFile) {
    $pidText = (Get-Content -LiteralPath $pidFile -Raw).Trim()
    $pidValue = 0
    if ([int]::TryParse($pidText, [ref]$pidValue)) {
        $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
        if ($process) { Write-Output "RUNNER_ACTIVE PID=$pidValue" }
        else { Write-Output "RUNNER_NOT_ACTIVE lastPid=$pidValue" }
    }
} else {
    Write-Output "RUNNER_NOT_STARTED"
}

Write-Output "LOG=$log"
if (Test-Path -LiteralPath $log) {
    Get-Content -LiteralPath $log -Tail $Lines
}
