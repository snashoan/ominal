param(
    [string]$Workspace = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'
$stateDir = Join-Path $Workspace 'build-logs\codex-bg'
$pidFile = Join-Path $stateDir 'runner.pid'
$log = Join-Path $stateDir 'session.log'
$runner = Join-Path $PSScriptRoot 'ominal-bg-codex-runner.ps1'

New-Item -ItemType Directory -Force -Path $stateDir | Out-Null

if (Test-Path -LiteralPath $pidFile) {
    $oldPidText = (Get-Content -LiteralPath $pidFile -Raw).Trim()
    $oldPid = 0
    if ([int]::TryParse($oldPidText, [ref]$oldPid)) {
        $existing = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
        if ($existing) {
            Write-Output "RUNNER_ALREADY_ACTIVE PID=$oldPid"
            Write-Output "LOG=$log"
            exit 0
        }
    }
}

$pwsh = (Get-Command pwsh.exe -ErrorAction SilentlyContinue).Source
if (-not $pwsh) { $pwsh = (Get-Command powershell.exe).Source }

$args = @(
    '-NoProfile',
    '-ExecutionPolicy', 'Bypass',
    '-File', $runner,
    '-Workspace', $Workspace
)

$process = Start-Process -FilePath $pwsh -ArgumentList $args -WindowStyle Hidden -PassThru
Start-Sleep -Milliseconds 500
Write-Output "RUNNER_STARTED PID=$($process.Id)"
Write-Output "LOG=$log"
Write-Output "INBOX=$(Join-Path $stateDir 'inbox.jsonl')"
