param(
    [Parameter(Mandatory = $true)]
    [string]$Message,
    [string]$Workspace = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'
$stateDir = Join-Path $Workspace 'build-logs\codex-bg'
$inbox = Join-Path $stateDir 'inbox.jsonl'
New-Item -ItemType Directory -Force -Path $stateDir | Out-Null
if (-not (Test-Path -LiteralPath $inbox)) { New-Item -ItemType File -Path $inbox | Out-Null }

$job = [pscustomobject]@{
    id = [guid]::NewGuid().ToString()
    createdAt = (Get-Date -Format o)
    prompt = $Message
}

Add-Content -LiteralPath $inbox -Value ($job | ConvertTo-Json -Compress) -Encoding UTF8
Write-Output "QUEUED id=$($job.id)"
