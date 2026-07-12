param(
    [Parameter(Mandatory = $true)]
    [string]$Serial,
    [Parameter(Mandatory = $true)]
    [string]$SourcePath,
    [Parameter(Mandatory = $true)]
    [string]$DestinationPath
)

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { throw "adb.exe was not found: $adb" }

$destinationDirectory = Split-Path -Parent $DestinationPath
if ($destinationDirectory) { New-Item -ItemType Directory -Force $destinationDirectory | Out-Null }

$info = [System.Diagnostics.ProcessStartInfo]::new()
$info.FileName = $adb
$info.UseShellExecute = $false
$info.RedirectStandardOutput = $true
$info.RedirectStandardError = $true
foreach ($argument in @("-s", $Serial, "exec-out", "run-as", "com.ominal", "/system/bin/cat", $SourcePath)) {
    [void]$info.ArgumentList.Add($argument)
}

$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $info
if (-not $process.Start()) { throw "Could not start adb" }

$destination = [System.IO.File]::Open($DestinationPath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
try {
    $process.StandardOutput.BaseStream.CopyTo($destination)
} finally {
    $destination.Dispose()
}

$stderr = $process.StandardError.ReadToEnd()
$process.WaitForExit()
if ($process.ExitCode -ne 0) {
    throw "adb exec-out failed with exit code $($process.ExitCode): $stderr"
}
