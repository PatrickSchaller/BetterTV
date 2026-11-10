$ErrorActionPreference = 'Stop'
$tvProjectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $tvProjectRoot
try {
    New-Item -ItemType Directory -Path '.tools' -Force | Out-Null
    function Save-VerifiedArchive([string]$Url, [string]$Path, [string]$Sha256) {
        if (-not (Test-Path -LiteralPath $Path)) {
            if (Get-Command node -ErrorAction SilentlyContinue) {
                $tvDownloadCode = @'
const fs = require('node:fs');
const { Readable } = require('node:stream');
const { pipeline } = require('node:stream/promises');
(async () => {
    const response = await fetch(process.argv[1]);
    if (!response.ok) throw new Error(`Download failed: HTTP ${response.status}`);
    await pipeline(Readable.fromWeb(response.body), fs.createWriteStream(process.argv[2]));
})().catch(error => { console.error(error.message); process.exitCode = 1; });
'@
                & node -e $tvDownloadCode $Url $Path
                if ($LASTEXITCODE -ne 0) { throw "Download failed: $Url" }
            }
            else { Invoke-WebRequest -Uri $Url -OutFile $Path }
        }
        if ((Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant() -ne $Sha256) {
            throw "Checksum mismatch: $Path. Remove that archive and retry."
        }
    }

    if (-not $env:JAVA_HOME) {
        if (-not (Test-Path -LiteralPath '.tools/jdk/jdk-17.0.21+9/bin/java.exe')) {
            Save-VerifiedArchive 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.21_9.zip' '.tools/jdk.zip' '98518ecf554465057ee5509462631deeee374bc368c12a411d225cd29381f84b'
            Expand-Archive -LiteralPath '.tools/jdk.zip' -DestinationPath '.tools/jdk' -Force
        }
        $env:JAVA_HOME = Join-Path $tvProjectRoot '.tools/jdk/jdk-17.0.21+9'
    }
    if (-not $env:ANDROID_HOME) {
        if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_HOME = $env:ANDROID_SDK_ROOT }
        else { $env:ANDROID_HOME = Join-Path $tvProjectRoot '.tools/android-sdk' }
    }
    $env:ANDROID_USER_HOME = Join-Path $tvProjectRoot '.tools/android-user-home'
    $tvSdkManager = Join-Path $env:ANDROID_HOME 'cmdline-tools/latest/bin/sdkmanager.bat'
    if (-not (Test-Path -LiteralPath $tvSdkManager)) {
        Save-VerifiedArchive 'https://dl.google.com/android/repository/commandlinetools-win-15859902_latest.zip' '.tools/cmdline.zip' '90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a'
        $tvCmdlineDirectory = Join-Path $env:ANDROID_HOME 'cmdline-tools'
        New-Item -ItemType Directory -Path $tvCmdlineDirectory -Force | Out-Null
        Expand-Archive -LiteralPath '.tools/cmdline.zip' -DestinationPath $tvCmdlineDirectory -Force
        $tvExtractedDirectory = Join-Path $tvCmdlineDirectory 'cmdline-tools'
        $tvFinalDirectory = Join-Path $tvCmdlineDirectory 'latest'
        $tvResolvedSdk = [IO.Path]::GetFullPath($env:ANDROID_HOME).TrimEnd('\') + '\'
        foreach ($tvDirectory in @($tvExtractedDirectory, $tvFinalDirectory)) {
            if (-not [IO.Path]::GetFullPath($tvDirectory).StartsWith($tvResolvedSdk, [StringComparison]::OrdinalIgnoreCase)) {
                throw 'The SDK tool directories must stay inside ANDROID_HOME.'
            }
        }
        Move-Item -LiteralPath $tvExtractedDirectory -Destination $tvFinalDirectory
    }
    1..10 | ForEach-Object { 'y' } | & $tvSdkManager "--sdk_root=$env:ANDROID_HOME" --licenses *> '.tools/sdk-license-output.log'
    if ($LASTEXITCODE -ne 0) { throw 'Android SDK license acceptance failed.' }
    & $tvSdkManager "--sdk_root=$env:ANDROID_HOME" 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
    if ($LASTEXITCODE -ne 0) { throw 'Android SDK installation failed.' }
    Write-Output 'Android build tools are ready. Run .\build.ps1.'
}
finally { Pop-Location }
