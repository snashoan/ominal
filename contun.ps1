param(
    [int]$Lines = 120
)

$ErrorActionPreference = 'Stop'
$workspace = $PSScriptRoot
$start = Join-Path $workspace 'tools\ominal-bg-codex-start.ps1'
$tail = Join-Path $workspace 'tools\ominal-bg-codex-tail.ps1'

& $start -Workspace $workspace
Write-Output ''
Write-Output 'Reattach/status command:'
Write-Output '  .\contun.ps1'
Write-Output ''
Write-Output 'Send another instruction to the background worker:'
Write-Output '  .\tools\ominal-bg-codex-send.ps1 -Message "continue from OMINAL_TASK_HANDOUT.md"'
Write-Output ''
& $tail -Workspace $workspace -Lines $Lines
