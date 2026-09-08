param([switch]$DeviceTests, [switch]$Clean)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $taskRoot
try {
    # This Windows workspace marks generated directories read-only; Gradle cannot delete them.
    $taskBuild = Join-Path $taskRoot 'build'
    if (Test-Path -LiteralPath $taskBuild) {
        $taskBuild = (Resolve-Path -LiteralPath $taskBuild).Path
        Get-ChildItem -LiteralPath $taskBuild -Recurse -Force | ForEach-Object {
            if (-not $_.FullName.StartsWith($taskBuild + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
                throw 'Generated path escaped the build directory.'
            }
            if ($_.Attributes -band [IO.FileAttributes]::ReadOnly) {
                $_.Attributes = $_.Attributes -band (-bnot [IO.FileAttributes]::ReadOnly)
            }
        }
    }
    if (-not $env:JAVA_HOME) {
        $taskJdk = Join-Path $env:USERPROFILE '.gradle/jdks/eclipse_adoptium-21-amd64-windows.2'
        if (Test-Path -LiteralPath (Join-Path $taskJdk 'bin/java.exe')) { $env:JAVA_HOME = $taskJdk }
    }
    if ($Clean) {
        $taskGeneratedApp = Join-Path $taskRoot 'build/prototype-app'
        if (Test-Path -LiteralPath $taskGeneratedApp) {
            $taskGeneratedApp = (Resolve-Path -LiteralPath $taskGeneratedApp).Path
            $taskExpectedBuild = [IO.Path]::GetFullPath((Join-Path $taskRoot 'build'))
            if (-not $taskGeneratedApp.StartsWith($taskExpectedBuild + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
                throw 'Refusing to clean outside this project build directory.'
            }
            Remove-Item -LiteralPath $taskGeneratedApp -Recurse -Force
        }
    }
    $taskArgs = @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug', '--console=plain')
    if ($DeviceTests) { $taskArgs += ':app:connectedDebugAndroidTest' }
    & .\gradlew.bat @taskArgs
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed ($LASTEXITCODE)." }
    New-Item -ItemType Directory -Path artifacts -Force | Out-Null
    Copy-Item -LiteralPath build/prototype-app/outputs/apk/debug/app-debug.apk -Destination artifacts/CzyToTy-prototype.apk -Force
    Get-FileHash -LiteralPath artifacts/CzyToTy-prototype.apk -Algorithm SHA256
} finally { Pop-Location }
