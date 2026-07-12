param(
    [string]$Workspace = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Continue'
$stateDir = Join-Path $Workspace 'build-logs\codex-bg'
$inbox = Join-Path $stateDir 'inbox.jsonl'
$cursorFile = Join-Path $stateDir 'cursor.txt'
$log = Join-Path $stateDir 'session.log'
$pidFile = Join-Path $stateDir 'runner.pid'

New-Item -ItemType Directory -Force -Path $stateDir | Out-Null
if (-not (Test-Path -LiteralPath $inbox)) { New-Item -ItemType File -Path $inbox | Out-Null }
if (-not (Test-Path -LiteralPath $cursorFile)) { Set-Content -LiteralPath $cursorFile -Value '0' -Encoding UTF8 }
Set-Content -LiteralPath $pidFile -Value $PID -Encoding UTF8

function Write-RunnerLog {
    param([string]$Message)
    $stamp = Get-Date -Format o
    Add-Content -LiteralPath $log -Value "[$stamp] $Message" -Encoding UTF8
}

function Get-CodexCommand {
    $cmd = Get-Command codex.cmd -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $cmd = Get-Command codex -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

Write-RunnerLog "Background Codex runner started. Workspace=$Workspace PID=$PID"

while ($true) {
    try {
        $cursorText = '0'
        if (Test-Path -LiteralPath $cursorFile) {
            $cursorText = (Get-Content -LiteralPath $cursorFile -Raw).Trim()
        }
        $cursor = 0
        [void][int]::TryParse($cursorText, [ref]$cursor)

        $lines = @()
        if (Test-Path -LiteralPath $inbox) {
            $lines = @(Get-Content -LiteralPath $inbox -ErrorAction SilentlyContinue)
        }

        while ($cursor -lt $lines.Count) {
            $line = $lines[$cursor]
            $cursor++
            Set-Content -LiteralPath $cursorFile -Value $cursor -Encoding UTF8
            if ([string]::IsNullOrWhiteSpace($line)) { continue }

            try {
                $job = $line | ConvertFrom-Json
                $prompt = [string]$job.prompt
                if ([string]::IsNullOrWhiteSpace($prompt)) {
                    Write-RunnerLog "Skipping empty prompt at inbox line $cursor."
                    continue
                }

                Write-RunnerLog "PROMPT_BEGIN line=$cursor id=$($job.id)"
                Add-Content -LiteralPath $log -Value $prompt -Encoding UTF8
                Write-RunnerLog "PROMPT_END line=$cursor id=$($job.id)"

                $codex = Get-CodexCommand
                if (-not $codex) {
                    Write-RunnerLog "Codex command not found. Prompt left logged but not executed."
                    continue
                }

                Push-Location $Workspace
                try {
                    Write-RunnerLog "CODEX_EXEC_BEGIN id=$($job.id)"
                    $prompt | & $codex exec `
                        --skip-git-repo-check `
                        --dangerously-bypass-approvals-and-sandbox `
                        -C $Workspace `
                        - 2>&1 | ForEach-Object {
                            Add-Content -LiteralPath $log -Value ([string]$_) -Encoding UTF8
                        }
                    $exit = $LASTEXITCODE
                    Write-RunnerLog "CODEX_EXEC_END id=$($job.id) exit=$exit"
                } finally {
                    Pop-Location
                }
            } catch {
                Write-RunnerLog "Runner job error: $($_.Exception.Message)"
            }
        }
    } catch {
        Write-RunnerLog "Runner loop error: $($_.Exception.Message)"
    }

    Start-Sleep -Seconds 2
}
