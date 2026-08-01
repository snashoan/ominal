param(
    [string]$Workspace = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path,
    [string]$WslUser = 'root',
    [string]$AppDir = '/mnt/c/Users/saura/skynet/termux-app',
    [string]$TermuxPackagesDir = '/root/ominal/termux-packages-ominal',
    [string]$Architectures = 'aarch64',
    [string]$BuildLog = '/root/ominal/bootstrap-build.log',
    [ValidateRange(1, 64)]
    [int]$BuildJobs = 4,
    [string]$TaskName = 'OminalBootstrapBuild',
    [switch]$Resume
)

$ErrorActionPreference = 'Stop'

$stateDir = Join-Path $Workspace 'build-logs'
$statusFile = Join-Path $stateDir 'ominal-bootstrap-status.txt'
$foregroundRunner = Join-Path $PSScriptRoot 'run-ominal-bootstrap-build-windows.ps1'

New-Item -ItemType Directory -Force -Path $stateDir | Out-Null

$existingTask = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
if ($existingTask -and $existingTask.State -eq 'Running') {
    Write-Output "BOOTSTRAP_ALREADY_ACTIVE TASK=$TaskName"
    Write-Output "STATUS=$statusFile"
    Write-Output "WSL_LOG=$BuildLog"
    exit 0
}
if ($existingTask) {
    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
}

$actionArguments = @(
    '-NoProfile',
    '-WindowStyle Hidden',
    '-ExecutionPolicy Bypass',
    "-File `"$foregroundRunner`"",
    "-WslUser `"$WslUser`"",
    "-AppDir `"$AppDir`"",
    "-TermuxPackagesDir `"$TermuxPackagesDir`"",
    "-Architectures `"$Architectures`"",
    "-BuildLog `"$BuildLog`"",
    "-BuildJobs $BuildJobs"
)
if ($Resume) {
    $actionArguments += '-Resume'
}

$powershellCommand = Get-Command pwsh.exe -ErrorAction SilentlyContinue
if (-not $powershellCommand) {
    $powershellCommand = Get-Command powershell.exe -ErrorAction Stop
}
$powershell = $powershellCommand.Source
$action = New-ScheduledTaskAction -Execute $powershell -Argument ($actionArguments -join ' ')
$trigger = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(5)
$settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit ([TimeSpan]::Zero) `
    -StartWhenAvailable
$principal = New-ScheduledTaskPrincipal `
    -UserId ([System.Security.Principal.WindowsIdentity]::GetCurrent().Name) `
    -LogonType Interactive `
    -RunLevel Limited

Set-Content -LiteralPath $statusFile -Value 'starting' -Encoding UTF8
Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Principal $principal `
    -Description 'Build and validate the ARM64 Ominal Android bootstrap.' | Out-Null
Start-ScheduledTask -TaskName $TaskName
Start-Sleep -Seconds 3

$task = Get-ScheduledTask -TaskName $TaskName
Write-Output "BOOTSTRAP_TASK_STARTED TASK=$TaskName STATE=$($task.State) RESUME=$($Resume.IsPresent)"
Write-Output "STATUS=$statusFile"
Write-Output "WSL_LOG=$BuildLog"
