[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Aab,

    [ValidateSet("completed", "draft", "halted", "inProgress")]
    [string]$Status = "completed",

    [switch]$ValidateOnly
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$bundlePath = (Resolve-Path -LiteralPath $Aab).Path
$versionFile = Join-Path $repoRoot "version.properties"
$version = Get-Content -LiteralPath $versionFile -Raw | ConvertFrom-StringData
$expectedVersionCode = [int]$version.versionCode
$expectedVersionName = [string]$version.versionName

if ([string]::IsNullOrWhiteSpace($env:GOOGLE_PLAY_JSON_KEY)) {
    throw "Set GOOGLE_PLAY_JSON_KEY to the Play service-account JSON file."
}

$credentialPath = (Resolve-Path -LiteralPath $env:GOOGLE_PLAY_JSON_KEY).Path
if (-not (Test-Path -LiteralPath $bundlePath -PathType Leaf)) {
    throw "AAB not found: $bundlePath"
}
if (-not (Test-Path -LiteralPath $credentialPath -PathType Leaf)) {
    throw "Play service-account JSON not found: $credentialPath"
}

Push-Location $repoRoot
try {
    & .\gradlew.bat :app:verifyVersioning --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "Canonical version validation failed."
    }

    $bundletool = if ($env:BUNDLETOOL_JAR) {
        (Resolve-Path -LiteralPath $env:BUNDLETOOL_JAR).Path
    } else {
        Get-ChildItem -LiteralPath (Join-Path $repoRoot "build-tools") `
            -Filter "bundletool-all-*.jar" -File |
            Sort-Object Name -Descending |
            Select-Object -First 1 -ExpandProperty FullName
    }
    if (-not $bundletool) {
        throw "Set BUNDLETOOL_JAR or place bundletool-all in build-tools."
    }

    $embeddedVersionCode = (& java -jar $bundletool dump manifest `
        --bundle $bundlePath --xpath "/manifest/@android:versionCode").Trim()
    if ($LASTEXITCODE -ne 0 -or $embeddedVersionCode -ne "$expectedVersionCode") {
        throw "AAB version code $embeddedVersionCode does not match canonical code $expectedVersionCode."
    }
    $embeddedVersionName = (& java -jar $bundletool dump manifest `
        --bundle $bundlePath --xpath "/manifest/@android:versionName").Trim()
    if ($LASTEXITCODE -ne 0 -or $embeddedVersionName -ne $expectedVersionName) {
        throw "AAB version name $embeddedVersionName does not match canonical name $expectedVersionName."
    }

    $validate = if ($ValidateOnly) { "true" } else { "false" }
    & bundle exec fastlane android internal `
        "aab:$bundlePath" `
        "status:$Status" `
        "validate:$validate"
    if ($LASTEXITCODE -ne 0) {
        throw "Fastlane internal deployment failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}
