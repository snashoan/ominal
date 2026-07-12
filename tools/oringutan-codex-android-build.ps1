param(
    [string] $DeviceId = "O7ON59OZEY7LOVQG",
    [string] $AndroidApi = "35"
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$TermuxAppRoot = Split-Path -Parent $PSScriptRoot
$SkynetRoot = Split-Path -Parent $TermuxAppRoot
$CodexDir = Join-Path $SkynetRoot "codex"
$CodexRsDir = Join-Path $CodexDir "codex-rs"
$LogDir = Join-Path $TermuxAppRoot "build-logs"
$TranscriptPath = Join-Path $LogDir "oringutan-local-codex-android.log"
$StatusPath = Join-Path $LogDir "oringutan-local-codex-android-status.json"
$PidPath = Join-Path $LogDir "oringutan-local-codex-android.pid"
$ErrorPath = Join-Path $LogDir "oringutan-local-codex-android-error.txt"

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$PID | Set-Content -Path $PidPath -Encoding ASCII
if (Test-Path $ErrorPath) {
    Remove-Item -LiteralPath $ErrorPath -Force
}

function Write-OringutanStatus {
    param(
        [string] $Phase,
        [string] $Message,
        [hashtable] $Extra = @{}
    )

    $payload = [ordered]@{
        updatedAt = (Get-Date).ToString("o")
        phase = $Phase
        message = $Message
    }

    foreach ($key in $Extra.Keys) {
        $payload[$key] = $Extra[$key]
    }

    $payload | ConvertTo-Json -Depth 6 | Set-Content -Path $StatusPath -Encoding UTF8
    Write-Host "[$Phase] $Message"
}

function Invoke-Oringutan {
    param(
        [string] $Label,
        [string] $FilePath,
        [string[]] $Arguments,
        [string] $WorkingDirectory = $SkynetRoot,
        [hashtable] $Environment = @{}
    )

    Write-OringutanStatus -Phase "running" -Message $Label
    Push-Location $WorkingDirectory
    try {
        foreach ($key in $Environment.Keys) {
            Set-Item -Path "Env:$key" -Value $Environment[$key]
        }

        Write-Host "> $FilePath $($Arguments -join ' ')"
        & $FilePath @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "$Label failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

function Get-AndroidSdkRoot {
    if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) { return $env:ANDROID_HOME }
    if ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) { return $env:ANDROID_SDK_ROOT }

    $localSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $localSdk) { return $localSdk }

    throw "Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT."
}

function Get-NewestNdk {
    $sdk = Get-AndroidSdkRoot
    $ndkRoot = Join-Path $sdk "ndk"
    $ndk = Get-ChildItem -Path $ndkRoot -Directory | Sort-Object Name -Descending | Select-Object -First 1
    if (-not $ndk) { throw "No Android NDK found under $ndkRoot." }
    return $ndk.FullName
}

function Convert-ToForwardPath {
    param([string] $Path)
    return $Path -replace "\\", "/"
}

try {
    Start-Transcript -Path $TranscriptPath -Append | Out-Null

    Write-OringutanStatus -Phase "starting" -Message "Starting local Android arm64 Codex build handoff"

    $adb = Join-Path (Get-AndroidSdkRoot) "platform-tools\adb.exe"
    if (-not (Test-Path $adb)) { $adb = "adb" }

    $ndk = Get-NewestNdk
    $toolchainBin = Join-Path $ndk "toolchains\llvm\prebuilt\windows-x86_64\bin"
    $linker = Join-Path $toolchainBin "aarch64-linux-android$AndroidApi-clang.cmd"
    $clang = Join-Path $toolchainBin "clang.exe"
    $clangxx = Join-Path $toolchainBin "clang++.exe"
    $ar = Join-Path $toolchainBin "llvm-ar.exe"
    $ranlib = Join-Path $toolchainBin "llvm-ranlib.exe"
    if (-not (Test-Path $linker)) {
        throw "Android linker not found: $linker"
    }
    if (-not (Test-Path $clang)) {
        throw "Android clang not found: $clang"
    }
    if (-not (Test-Path $clangxx)) {
        throw "Android clang++ not found: $clangxx"
    }
    if (-not (Test-Path $ar)) {
        throw "Android llvm-ar not found: $ar"
    }
    if (-not (Test-Path $ranlib)) {
        throw "Android llvm-ranlib not found: $ranlib"
    }

    Write-OringutanStatus -Phase "toolchain" -Message "Using NDK $ndk"

    Invoke-Oringutan -Label "Install Rust Android target" -FilePath "rustup" -Arguments @("target", "add", "aarch64-linux-android")

    if (-not (Test-Path $CodexDir)) {
        Invoke-Oringutan -Label "Clone OpenAI Codex repo" -FilePath "git" -Arguments @("clone", "--depth", "1", "https://github.com/openai/codex.git", $CodexDir)
    } elseif (Test-Path (Join-Path $CodexDir ".git")) {
        $currentRevision = (& git -C $CodexDir rev-parse --short HEAD).Trim()
        if ($LASTEXITCODE -ne 0) {
            throw "Could not read Codex repo revision from $CodexDir"
        }
        Write-OringutanStatus -Phase "source" -Message "Using existing Codex repo at $currentRevision" @{
            codexDir = $CodexDir
            revision = $currentRevision
        }
    } else {
        throw "Codex path exists but is not a git repo: $CodexDir"
    }

    if (-not (Test-Path $CodexRsDir)) {
        throw "Codex Rust workspace not found: $CodexRsDir"
    }

    Invoke-Oringutan `
        -Label "Install Rust Android target for Codex toolchain" `
        -FilePath "rustup" `
        -Arguments @("target", "add", "aarch64-linux-android") `
        -WorkingDirectory $CodexRsDir

    $gitUsrBin = Join-Path $env:ProgramFiles "Git\usr\bin"
    $opensslPerl = Join-Path $gitUsrBin "perl.exe"
    $opensslPerlForMake = "C:/PROGRA~1/Git/usr/bin/perl.exe"
    $buildPath = "$toolchainBin;$env:PATH"
    if (Test-Path $opensslPerl) {
        $buildPath = "$gitUsrBin;$buildPath"
        Write-OringutanStatus -Phase "toolchain" -Message "Using Git/MSYS Perl for vendored OpenSSL" @{
            perl = $opensslPerl
        }
    }

    $envOverrides = @{
        "ANDROID_HOME" = (Get-AndroidSdkRoot)
        "ANDROID_SDK_ROOT" = (Get-AndroidSdkRoot)
        "ANDROID_NDK_HOME" = $ndk
        "CARGO_BUILD_JOBS" = "1"
        "PATH" = $buildPath
        "CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER" = $linker
        "CARGO_TARGET_AARCH64_LINUX_ANDROID_AR" = (Convert-ToForwardPath $ar)
        "AR_aarch64_linux_android" = (Convert-ToForwardPath $ar)
        "RANLIB_aarch64_linux_android" = (Convert-ToForwardPath $ranlib)
        "CC_aarch64_linux_android" = (Convert-ToForwardPath $clang)
        "CXX_aarch64_linux_android" = (Convert-ToForwardPath $clangxx)
        "CARGO_PROFILE_RELEASE_LTO" = "false"
        "CARGO_PROFILE_RELEASE_CODEGEN_UNITS" = "16"
        "CARGO_NET_GIT_FETCH_WITH_CLI" = "true"
        "OPENSSL_STATIC" = "1"
        "MSYS2_ARG_CONV_EXCL" = "*"
        "MSYS2_ENV_CONV_EXCL" = "PERL5LIB"
    }
    $python = "C:\Python313\python.exe"
    if (Test-Path $python) {
        $envOverrides["PYTHON"] = $python
    }
    if (Test-Path $opensslPerl) {
        if (Test-Path "C:\PROGRA~1\Git\usr\bin\perl.exe") {
            $envOverrides["PERL"] = $opensslPerlForMake
        } else {
            $envOverrides["PERL"] = $opensslPerl
        }
    }
    $strawberrySimpleModule = "C:\Strawberry\perl\lib\Locale\Maketext\Simple.pm"
    $strawberryExtUtils = "C:\Strawberry\perl\lib\ExtUtils"
    $strawberryPod = "C:\Strawberry\perl\lib\Pod"
    if ((Test-Path $opensslPerl) -and (Test-Path $strawberrySimpleModule) -and (Test-Path $strawberryExtUtils) -and (Test-Path $strawberryPod)) {
        $perlShimLib = Join-Path $LogDir "perl5lib"
        $perlShimModuleDir = Join-Path $perlShimLib "Locale\Maketext"
        New-Item -ItemType Directory -Force -Path $perlShimModuleDir | Out-Null
        Copy-Item -LiteralPath $strawberrySimpleModule -Destination (Join-Path $perlShimModuleDir "Simple.pm") -Force
        Copy-Item -LiteralPath $strawberryExtUtils -Destination $perlShimLib -Recurse -Force
        Copy-Item -LiteralPath $strawberryPod -Destination $perlShimLib -Recurse -Force

        $perlShimMsysPath = $perlShimLib -replace "\\", "/"
        if ($perlShimMsysPath -match "^([A-Za-z]):/(.*)$") {
            $perlShimMsysPath = "/$($Matches[1].ToLower())/$($Matches[2])"
        }
        $envOverrides["PERL5LIB"] = $perlShimMsysPath
    }

    Write-OringutanStatus -Phase "building" -Message "Building codex-cli for aarch64-linux-android" @{
        target = "aarch64-linux-android"
        codexDir = $CodexDir
        linker = $linker
    }

    Invoke-Oringutan `
        -Label "Cargo Android arm64 release build" `
        -FilePath "cargo" `
        -Arguments @("build", "-p", "codex-cli", "--bin", "codex", "--release", "--target", "aarch64-linux-android") `
        -WorkingDirectory $CodexRsDir `
        -Environment $envOverrides

    $builtBinary = Join-Path $CodexRsDir "target\aarch64-linux-android\release\codex"
    if (-not (Test-Path $builtBinary)) {
        $builtBinary = Join-Path $CodexRsDir "target\aarch64-linux-android\release\codex.exe"
    }
    if (-not (Test-Path $builtBinary)) {
        throw "Build completed but binary was not found in target\aarch64-linux-android\release."
    }

    Write-OringutanStatus -Phase "built" -Message "Built Codex Android binary" @{
        binary = $builtBinary
        sizeBytes = (Get-Item $builtBinary).Length
    }

    $installBinary = Join-Path $LogDir "codex-android-aarch64-stripped"
    $strip = Join-Path $toolchainBin "llvm-strip.exe"
    Copy-Item -LiteralPath $builtBinary -Destination $installBinary -Force
    if (Test-Path $strip) {
        & $strip $installBinary
        if ($LASTEXITCODE -ne 0) {
            throw "llvm-strip failed with exit code $LASTEXITCODE"
        }
    }

    Write-OringutanStatus -Phase "prepared" -Message "Prepared stripped Codex binary for Termux install" @{
        binary = $installBinary
        originalBinary = $builtBinary
        sizeBytes = (Get-Item $installBinary).Length
    }

    Invoke-Oringutan -Label "Check ADB device" -FilePath $adb -Arguments @("-s", $DeviceId, "get-state")

    Invoke-Oringutan -Label "Bring Termux to foreground" -FilePath $adb -Arguments @("-s", $DeviceId, "shell", "am", "start", "-n", "com.ominal/.app.TermuxActivity")
    Start-Sleep -Seconds 2
    Invoke-Oringutan -Label "Request Termux sshd start" -FilePath $adb -Arguments @("-s", $DeviceId, "shell", "input", "text", "sshd")
    Invoke-Oringutan -Label "Submit Termux sshd command" -FilePath $adb -Arguments @("-s", $DeviceId, "shell", "input", "keyevent", "ENTER")
    Start-Sleep -Seconds 2

    Invoke-Oringutan -Label "Ensure Termux SSH forward" -FilePath $adb -Arguments @("-s", $DeviceId, "forward", "tcp:8022", "tcp:8022")

    $remoteTmp = "/data/data/com.ominal/files/home/codex-cross-built"
    $remoteBin = "/data/data/com.ominal/files/usr/bin/codex"

    Invoke-Oringutan `
        -Label "Verify Termux SSH" `
        -FilePath "ssh" `
        -Arguments @("-p", "8022", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10", "-o", "StrictHostKeyChecking=accept-new", "u0_a660@127.0.0.1", "echo ssh-ok")

    Invoke-Oringutan `
        -Label "Copy Codex binary to Termux home" `
        -FilePath "scp" `
        -Arguments @("-P", "8022", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10", "-o", "StrictHostKeyChecking=accept-new", $installBinary, "u0_a660@127.0.0.1:$remoteTmp")

    Invoke-Oringutan `
        -Label "Install Codex binary into Termux prefix" `
        -FilePath "ssh" `
        -Arguments @("-p", "8022", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10", "-o", "StrictHostKeyChecking=accept-new", "u0_a660@127.0.0.1", "install -m 700 $remoteTmp $remoteBin && codex --version && codex exec --help | head")

    Write-OringutanStatus -Phase "complete" -Message "Codex CLI built locally, installed into Termux, and smoke-tested" @{
        remoteBinary = $remoteBin
        localBinary = $installBinary
        originalBinary = $builtBinary
    }
} catch {
    $message = $_.Exception.Message
    $message | Set-Content -Path $ErrorPath -Encoding UTF8
    Write-OringutanStatus -Phase "failed" -Message $message
    throw
} finally {
    try { Stop-Transcript | Out-Null } catch { }
}
