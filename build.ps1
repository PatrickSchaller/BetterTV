param(
    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]]$Tasks = @(':app:assembleDebug', ':app:lintDebug')
)

$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (-not $env:JAVA_HOME) {
        $tvLocalJdk = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot '.tools/jdk') -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin/java.exe') } | Select-Object -First 1
        if ($tvLocalJdk) { $env:JAVA_HOME = $tvLocalJdk.FullName }
    }
    if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
        throw 'A JDK 17 or newer is required. Run .\scripts\setup-android.ps1 or set JAVA_HOME.'
    }
    if (-not $env:ANDROID_HOME) {
        if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_HOME = $env:ANDROID_SDK_ROOT }
        elseif (Test-Path -LiteralPath '.tools/android-sdk') { $env:ANDROID_HOME = Join-Path $PSScriptRoot '.tools/android-sdk' }
    }
    if (-not $env:ANDROID_HOME) { throw 'Run .\scripts\setup-android.ps1 or set ANDROID_HOME to an Android SDK.' }
    $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.tools/gradle-user-home'
    $env:ANDROID_USER_HOME = Join-Path $PSScriptRoot '.tools/android-user-home'
    $tvDebugKey = Join-Path $PSScriptRoot '.tools/debug.keystore'
    if (-not (Test-Path -LiteralPath $tvDebugKey)) {
        New-Item -ItemType Directory -Path '.tools' -Force | Out-Null
        & (Join-Path $env:JAVA_HOME 'bin/keytool.exe') -genkeypair -keystore $tvDebugKey -storepass android -alias androiddebugkey -keypass android -dname 'CN=Android Debug,O=Android,C=US' -keyalg RSA -keysize 2048 -validity 10000
        if ($LASTEXITCODE -ne 0) { throw 'Could not create the local development signing key.' }
    }
    & .\scripts\check-core.ps1
    & .\gradlew.bat @Tasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE." }
}
finally { Pop-Location }
