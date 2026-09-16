#Requires -Version 5.1
<#
.SYNOPSIS
  Build School Manager Android APK with branding from the live server API.

.DESCRIPTION
  Applies name, logo, start URL, and version from api/pwa/apk-branding, then builds APK.
  Output is copied to ../public/downloads/school-manager.apk when possible.

.PARAMETER BrandingUrl
  Full HTTPS branding API URL, e.g. https://my.school.ir/public/api/pwa/apk-branding
  Copy this from: Admin → Settings → Mobile app → Branding API

.PARAMETER Release
  Build signed release APK (requires release.keystore in android-app folder).
  Default is debug APK (faster, no keystore needed).

.EXAMPLE
  .\build-apk.ps1 -BrandingUrl "https://my.emamhadischool.ir/public/api/pwa/apk-branding"

.EXAMPLE
  .\build-apk.ps1 -BrandingUrl "https://my.emamhadischool.ir/public/api/pwa/apk-branding" -Release
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$BrandingUrl,

    [switch]$Release
)

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

function Ensure-PythonTool {
    param([string]$ModuleName)
    python -c "import $ModuleName" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Installing Python module: $ModuleName" -ForegroundColor Yellow
        python -m pip install --user $ModuleName
    }
}

if ($BrandingUrl -notmatch '^https://') {
    Write-Host 'BrandingUrl must start with https://' -ForegroundColor Red
    Write-Host 'Example: https://my.emamhadischool.ir/public/api/pwa/apk-branding' -ForegroundColor Yellow
    exit 1
}

$javaHome = Find-JavaHome
if (-not $javaHome) {
    Write-Host 'JDK 17 not found. Install: winget install Microsoft.OpenJDK.17' -ForegroundColor Red
    exit 1
}

$sdk = Find-AndroidSdk
if (-not $sdk) {
    Write-Host 'Android SDK not found. Install Android Studio or use GitHub Actions instead.' -ForegroundColor Red
    exit 1
}

$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$env:PATH"
$env:ANDROID_HOME = $sdk
$sdkEsc = ($sdk -replace '\\', '\\')
"sdk.dir=$sdkEsc" | Set-Content -Path (Join-Path $Root 'local.properties') -Encoding ASCII

Ensure-PythonTool -ModuleName 'Pillow'

Write-Host "Verifying branding API..." -ForegroundColor Cyan
python -c @"
import json, sys, urllib.request
url = sys.argv[1]
if not url.startswith('https://'):
    raise SystemExit('branding_url must start with https://')
req = urllib.request.Request(url, headers={'Accept': 'application/json', 'User-Agent': 'SchoolManagerApkBuild/1.0'})
with urllib.request.urlopen(req, timeout=30) as resp:
    data = json.loads(resp.read().decode('utf-8'))
if data.get('ok') is False:
    raise SystemExit('API returned ok=false')
if 'example.com' in (data.get('start_url') or ''):
    raise SystemExit('start_url still points to example.com')
logo = (data.get('logo_url') or data.get('icon_url') or '').strip()
if logo and not logo.startswith('https://'):
    raise SystemExit(f'logo_url must be absolute HTTPS (got: {logo!r})')
print('Branding API OK:', data.get('app_name'), data.get('start_url'))
"@ $BrandingUrl
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Applying branding..." -ForegroundColor Cyan
python "$Root/scripts/apply_apk_branding.py" $Root $BrandingUrl
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Push-Location $Root
try {
    if ($Release) {
        if (-not (Test-Path "$Root/release.keystore")) {
            Write-Host 'Release build needs release.keystore in android-app/ folder.' -ForegroundColor Red
            Write-Host 'Use GitHub Actions for signed release, or build debug without -Release.' -ForegroundColor Yellow
            exit 1
        }
        $env:ANDROID_KEYSTORE_FILE = 'release.keystore'
        if (-not $env:ANDROID_KEYSTORE_PASSWORD) { $env:ANDROID_KEYSTORE_PASSWORD = 'SchoolManager2026!' }
        $env:ANDROID_KEY_ALIAS = 'schoolmanager'
        if (-not $env:ANDROID_KEY_PASSWORD) { $env:ANDROID_KEY_PASSWORD = $env:ANDROID_KEYSTORE_PASSWORD }
        Write-Host 'Building signed release APK...' -ForegroundColor Green
        & .\gradlew.bat assembleRelease --no-daemon
        $apk = Join-Path $Root 'app\build\outputs\apk\release\app-release.apk'
    } else {
        Write-Host 'Building debug APK...' -ForegroundColor Green
        & .\gradlew.bat assembleDebug --no-daemon
        $apk = Join-Path $Root 'app\build\outputs\apk\debug\app-debug.apk'
    }

    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    if (-not (Test-Path $apk)) {
        Write-Host "APK not found: $apk" -ForegroundColor Red
        exit 1
    }

    $destDir = Join-Path (Split-Path $Root -Parent) 'public\downloads'
    if (-not (Test-Path $destDir)) {
        New-Item -ItemType Directory -Path $destDir -Force | Out-Null
    }
    $dest = Join-Path $destDir 'school-manager.apk'
    Copy-Item -Path $apk -Destination $dest -Force

    Write-Host ''
    Write-Host 'Done!' -ForegroundColor Green
    Write-Host "  Built:  $apk"
    Write-Host "  Server: $dest"
    Write-Host ''
    Write-Host 'Next: uninstall old app on phone, then install the new APK.' -ForegroundColor Cyan
} finally {
    Pop-Location
}
