#Requires -Version 5.1
<#
.SYNOPSIS
  Build debug APK without Android Studio (needs JDK 17 + Android SDK only).

.USAGE
  1. Install JDK 17:  winget install Microsoft.OpenJDK.17
  2. Install Android command-line tools (one-time):
       winget install Google.AndroidSDK.PlatformTools
     Or install "Android SDK Command-line Tools" via Android Studio SDK Manager once,
     then set ANDROID_HOME (e.g. %LOCALAPPDATA%\Android\Sdk).
  3. Edit app\src\main\res\values\strings.xml → app_start_url
  4. Run:  .\build-apk.ps1
  5. Output: app\build\outputs\apk\debug\app-debug.apk
#>

$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot

function Find-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
        return $env:JAVA_HOME
    }
    $candidates = @(
        'C:\Program Files\Microsoft\jdk-17*',
        'C:\Program Files\Eclipse Adoptium\jdk-17*',
        'C:\Program Files\Java\jdk-17*'
    )
    foreach ($pattern in $candidates) {
        $hit = Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
        if ($hit -and (Test-Path "$($hit.FullName)\bin\java.exe")) {
            return $hit.FullName
        }
    }
    return $null
}

function Find-AndroidSdk {
    if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) {
        return $env:ANDROID_HOME
    }
    $local = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path $local) {
        return $local
    }
    return $null
}

$javaHome = Find-JavaHome
if (-not $javaHome) {
    Write-Host ''
    Write-Host 'JDK 17 not found.' -ForegroundColor Red
    Write-Host 'Install:  winget install Microsoft.OpenJDK.17' -ForegroundColor Yellow
    Write-Host 'Then re-open PowerShell and run this script again.' -ForegroundColor Yellow
    Write-Host ''
    Write-Host 'Alternative (no install): push project to GitHub and run workflow' -ForegroundColor Cyan
    Write-Host '  Actions -> Build Android APK -> Run workflow -> download artifact' -ForegroundColor Cyan
    exit 1
}

$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$env:PATH"

$sdk = Find-AndroidSdk
if ($sdk) {
    $env:ANDROID_HOME = $sdk
    $localProps = Join-Path $Root 'local.properties'
    $sdkEsc = ($sdk -replace '\\', '\\')
    "sdk.dir=$sdkEsc" | Set-Content -Path $localProps -Encoding ASCII
    Write-Host "Using Android SDK: $sdk"
} else {
    Write-Host ''
    Write-Host 'Android SDK not found.' -ForegroundColor Red
    Write-Host 'Easiest: use GitHub Actions (no local SDK needed).' -ForegroundColor Cyan
    Write-Host '  GitHub -> Actions -> Build Android APK -> Run workflow' -ForegroundColor Cyan
    Write-Host ''
    Write-Host 'Or install SDK command-line tools and set ANDROID_HOME.' -ForegroundColor Yellow
    exit 1
}

Push-Location $Root
try {
    Write-Host 'Building debug APK...' -ForegroundColor Green
    & .\gradlew.bat assembleDebug --no-daemon
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    $apk = Join-Path $Root 'app\build\outputs\apk\debug\app-debug.apk'
    $destDir = Join-Path (Split-Path $Root -Parent) 'public\downloads'
    if (-not (Test-Path $destDir)) {
        New-Item -ItemType Directory -Path $destDir -Force | Out-Null
    }
    $dest = Join-Path $destDir 'school-manager.apk'
    Copy-Item -Path $apk -Destination $dest -Force

    Write-Host ''
    Write-Host 'Done!' -ForegroundColor Green
    Write-Host "  APK: $apk"
    Write-Host "  Copied to server path: $dest"
} finally {
    Pop-Location
}
