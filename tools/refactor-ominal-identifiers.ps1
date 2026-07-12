param(
    [string] $RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$root = [System.IO.Path]::GetFullPath($RepositoryRoot)

function Assert-InRepository([string] $Path) {
    $resolved = [System.IO.Path]::GetFullPath($Path)
    if (-not $resolved.StartsWith($root + [System.IO.Path]::DirectorySeparatorChar,
            [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify a path outside the repository: $resolved"
    }
}

function Rewrite-TextFile([string] $Path, [hashtable] $Replacements) {
    Assert-InRepository $Path
    $content = [System.IO.File]::ReadAllText($Path)
    $updated = $content
    foreach ($entry in $Replacements.GetEnumerator()) {
        $updated = $updated.Replace($entry.Key, $entry.Value)
    }
    if ($updated -ne $content) {
        [System.IO.File]::WriteAllText($Path, $updated, [System.Text.UTF8Encoding]::new($false))
    }
}

$sourceRoots = @(
    'app/src/main/java/com/ominal',
    'app/src/test/java/com/ominal',
    'terminal-emulator/src/main/java/com/ominal',
    'terminal-emulator/src/test/java/com/ominal',
    'terminal-view/src/main/java/com/ominal',
    'ominal-shared/src/main/java/com/ominal',
    'ominal-shared/src/androidTest/java/com/ominal'
)

$javaReplacements = [ordered]@{
    'com.ominal.shared.termux' = 'com.ominal.shared.runtime'
    'com.ominal.app.fragments.settings.termux_api' = 'com.ominal.app.fragments.settings.api'
    'com.ominal.app.fragments.settings.termux_float' = 'com.ominal.app.fragments.settings.floatapp'
    'com.ominal.app.fragments.settings.termux_tasker' = 'com.ominal.app.fragments.settings.tasker'
    'com.ominal.app.fragments.settings.termux_widget' = 'com.ominal.app.fragments.settings.widget'
    'com.ominal.app.fragments.settings.termux' = 'com.ominal.app.fragments.settings.runtime'
    'Termux' = 'Ominal'
}

foreach ($relativeRoot in $sourceRoots) {
    $sourceRoot = Join-Path $root $relativeRoot
    if (-not (Test-Path -LiteralPath $sourceRoot)) { continue }
    Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter '*.java' |
        ForEach-Object {
            Rewrite-TextFile $_.FullName $javaReplacements
            $content = [System.IO.File]::ReadAllText($_.FullName)
            $updated = $content.Replace('TERMUX_', 'OMINAL_').Replace('termux_', 'ominal_')
            $updated = [regex]::Replace($updated, '\btermux(?=[A-Z])', 'ominal')
            if ($updated -ne $content) {
                [System.IO.File]::WriteAllText($_.FullName, $updated, [System.Text.UTF8Encoding]::new($false))
            }
        }
}

$textFiles = @(
    'settings.gradle',
    'app/build.gradle',
    'terminal-emulator/build.gradle',
    'terminal-view/build.gradle',
    'ominal-shared/build.gradle',
    'app/src/main/AndroidManifest.xml'
)
foreach ($relativePath in $textFiles) {
    $path = Join-Path $root $relativePath
    if (Test-Path -LiteralPath $path) {
        Rewrite-TextFile $path ([ordered]@{
            'termux-shared' = 'ominal-shared'
            'Termux' = 'Ominal'
        })
        $content = [System.IO.File]::ReadAllText($path)
        $updated = $content.Replace('TERMUX_', 'OMINAL_').Replace('termux_', 'ominal_')
        if ($updated -ne $content) {
            [System.IO.File]::WriteAllText($path, $updated, [System.Text.UTF8Encoding]::new($false))
        }
    }
}

$resourceRoots = @('app/src/main/res', 'ominal-shared/src/main/res')
foreach ($relativeRoot in $resourceRoots) {
    $resourceRoot = Join-Path $root $relativeRoot
    if (-not (Test-Path -LiteralPath $resourceRoot)) { continue }
    Get-ChildItem -LiteralPath $resourceRoot -Recurse -File -Filter '*.xml' | ForEach-Object {
        $path = $_.FullName
        Assert-InRepository $path
        $content = [System.IO.File]::ReadAllText($path)
        $updated = $content.Replace('Termux', 'Ominal')
        $updated = $updated.Replace('TERMUX_', 'OMINAL_').Replace('termux_', 'ominal_')
        $updated = [regex]::Replace($updated, '(name="|@[a-z]+/|\?attr/)termux', '$1ominal')
        if ($updated -ne $content) {
            [System.IO.File]::WriteAllText($path, $updated, [System.Text.UTF8Encoding]::new($false))
        }
    }
}

$javaResourceRoots = $sourceRoots | ForEach-Object { Join-Path $root $_ }
foreach ($sourceRoot in $javaResourceRoots) {
    if (-not (Test-Path -LiteralPath $sourceRoot)) { continue }
    Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter '*.java' | ForEach-Object {
        $path = $_.FullName
        Assert-InRepository $path
        $content = [System.IO.File]::ReadAllText($path)
        $updated = [regex]::Replace($content, '(R\.(?:string|id|layout|xml|style|drawable|color|dimen)\.)termux', '$1ominal')
        if ($updated -ne $content) {
            [System.IO.File]::WriteAllText($path, $updated, [System.Text.UTF8Encoding]::new($false))
        }
    }
}

$directoryMoves = [ordered]@{
    'termux-shared/src/main/java/com/ominal/shared/termux' = 'termux-shared/src/main/java/com/ominal/shared/runtime'
    'app/src/main/java/com/ominal/app/fragments/settings/termux_api' = 'app/src/main/java/com/ominal/app/fragments/settings/api'
    'app/src/main/java/com/ominal/app/fragments/settings/termux_float' = 'app/src/main/java/com/ominal/app/fragments/settings/floatapp'
    'app/src/main/java/com/ominal/app/fragments/settings/termux_tasker' = 'app/src/main/java/com/ominal/app/fragments/settings/tasker'
    'app/src/main/java/com/ominal/app/fragments/settings/termux_widget' = 'app/src/main/java/com/ominal/app/fragments/settings/widget'
    'app/src/main/java/com/ominal/app/fragments/settings/termux' = 'app/src/main/java/com/ominal/app/fragments/settings/runtime'
}
foreach ($entry in $directoryMoves.GetEnumerator()) {
    $source = Join-Path $root $entry.Key
    $destination = Join-Path $root $entry.Value
    if (-not (Test-Path -LiteralPath $source)) { continue }
    Assert-InRepository $source
    Assert-InRepository $destination
    if (Test-Path -LiteralPath $destination) { throw "Destination already exists: $destination" }
    Move-Item -LiteralPath $source -Destination $destination
}

$renameRoots = @(
    'app/src/main/java/com/ominal',
    'app/src/test/java/com/ominal',
    'terminal-emulator/src/main/java/com/ominal',
    'terminal-emulator/src/test/java/com/ominal',
    'terminal-view/src/main/java/com/ominal',
    'ominal-shared/src/main/java/com/ominal',
    'ominal-shared/src/androidTest/java/com/ominal'
)
foreach ($relativeRoot in $renameRoots) {
    $renameRoot = Join-Path $root $relativeRoot
    if (-not (Test-Path -LiteralPath $renameRoot)) { continue }
    Get-ChildItem -LiteralPath $renameRoot -Recurse -File |
        Where-Object { $_.Name -like '*Termux*' } |
        Sort-Object { $_.FullName.Length } -Descending |
        ForEach-Object {
            $destination = Join-Path $_.DirectoryName ($_.Name.Replace('Termux', 'Ominal'))
            Assert-InRepository $destination
            Move-Item -LiteralPath $_.FullName -Destination $destination
        }
}

foreach ($relativeRoot in $resourceRoots) {
    $resourceRoot = Join-Path $root $relativeRoot
    if (-not (Test-Path -LiteralPath $resourceRoot)) { continue }
    Get-ChildItem -LiteralPath $resourceRoot -Recurse -File |
        Where-Object { $_.Name -like '*termux*' } |
        ForEach-Object {
            $destination = Join-Path $_.DirectoryName ($_.Name.Replace('termux', 'ominal'))
            Assert-InRepository $destination
            Move-Item -LiteralPath $_.FullName -Destination $destination
        }
}

$nativeMoves = [ordered]@{
    'app/src/main/cpp/termux-bootstrap.c' = 'app/src/main/cpp/ominal-bootstrap.c'
    'terminal-emulator/src/main/jni/termux.c' = 'terminal-emulator/src/main/jni/ominal.c'
}
foreach ($entry in $nativeMoves.GetEnumerator()) {
    $source = Join-Path $root $entry.Key
    $destination = Join-Path $root $entry.Value
    if (Test-Path -LiteralPath $source) {
        Assert-InRepository $source
        Assert-InRepository $destination
        Move-Item -LiteralPath $source -Destination $destination
    }
}

foreach ($gradleFile in @('app/build.gradle', 'terminal-emulator/build.gradle')) {
    $path = Join-Path $root $gradleFile
    Rewrite-TextFile $path ([ordered]@{
        'termux-bootstrap.c' = 'ominal-bootstrap.c'
        'termux.c' = 'ominal.c'
    })
}

$oldShared = Join-Path $root 'termux-shared'
$newShared = Join-Path $root 'ominal-shared'
if (Test-Path -LiteralPath $oldShared) {
    Assert-InRepository $oldShared
    Assert-InRepository $newShared
    if (Test-Path -LiteralPath $newShared) { throw "Destination already exists: $newShared" }
    Move-Item -LiteralPath $oldShared -Destination $newShared
}

Write-Host 'Ominal identifier refactor complete.'
