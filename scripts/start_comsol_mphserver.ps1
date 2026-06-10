param(
    [string]$ComsolBin = "D:\Software\Comsol6.3\COMSOL63\Multiphysics\bin\win64",
    [string]$RuntimeDir = ".comsol_runtime",
    [int]$Port = 2036,
    [string]$User = $env:USERNAME,
    [switch]$SyncDefaultLogin
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$runtime = if ([System.IO.Path]::IsPathRooted($RuntimeDir)) {
    $RuntimeDir
} else {
    Join-Path $root $RuntimeDir
}

$configuration = Join-Path $runtime "configuration"
$prefs = Join-Path $runtime "prefs"
$tmp = Join-Path $runtime "tmp"
$recovery = Join-Path $runtime "recovery"
$portfile = Join-Path $root "comsol_mphserver.port"

New-Item -ItemType Directory -Force -Path $configuration, $prefs, $tmp, $recovery | Out-Null

if ($SyncDefaultLogin) {
    $defaultLogin = Join-Path $env:USERPROFILE ".comsol\v63\login.properties"
    if (!(Test-Path -LiteralPath $defaultLogin)) {
        throw "Default COMSOL login file not found: $defaultLogin"
    }
    Copy-Item -LiteralPath $defaultLogin -Destination (Join-Path $prefs "login.properties") -Force
}

Remove-Item -LiteralPath $portfile -ErrorAction SilentlyContinue

$exe = Join-Path $ComsolBin "comsolmphserver.exe"
if (!(Test-Path -LiteralPath $exe)) {
    throw "COMSOL mphserver executable not found: $exe"
}

$args = @(
    "-configuration", $configuration,
    "-prefsdir", $prefs,
    "-tmpdir", $tmp,
    "-recoverydir", $recovery,
    "-port", "$Port",
    "-multi", "on",
    "-user", $User,
    "-login", "auto",
    "-silent",
    "-portfile", $portfile
)

$process = Start-Process -FilePath $exe -ArgumentList $args -WindowStyle Hidden -PassThru
Write-Host "Started COMSOL mphserver PID=$($process.Id)"
Write-Host "Port file: $portfile"
Write-Host "Runtime dir: $runtime"
