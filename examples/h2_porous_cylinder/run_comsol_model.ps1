param(
    [string]$ComsolBin = "D:\Software\Comsol6.3\COMSOL63\Multiphysics\bin\win64",
    [string]$ModelScript = "h2_porous_cylinder_model.java",
    [string]$ExportScript = "export_results.java",
    [string]$ModelFile = "h2_porous_cylinder_model.mph",
    [switch]$SkipSolve,
    [switch]$SkipExport
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$comsolBatch = Join-Path $ComsolBin "comsolbatch.exe"
$comsolCompile = Join-Path $ComsolBin "comsolcompile.exe"
if (-not (Test-Path -LiteralPath $comsolBatch)) {
    throw "Cannot find comsolbatch.exe at '$comsolBatch'. Pass -ComsolBin with your COMSOL bin\win64 directory."
}
if (-not (Test-Path -LiteralPath $comsolCompile)) {
    throw "Cannot find comsolcompile.exe at '$comsolCompile'. Pass -ComsolBin with your COMSOL bin\win64 directory."
}

if (-not (Test-Path -LiteralPath $ModelScript)) {
    throw "Cannot find model script '$ModelScript'."
}

New-Item -ItemType Directory -Force -Path "results" | Out-Null

if (-not $SkipSolve) {
    Write-Host "Compiling $ModelScript ..."
    & $comsolCompile $ModelScript
    if ($LASTEXITCODE -ne 0) {
        throw "COMSOL Java compilation failed with exit code $LASTEXITCODE."
    }

    $modelClass = [System.IO.Path]::ChangeExtension($ModelScript, ".class")
    Write-Host "Building and solving $modelClass ..."
    & $comsolBatch -inputfile $modelClass -outputfile $ModelFile
    if ($LASTEXITCODE -ne 0) {
        throw "COMSOL model build/solve failed with exit code $LASTEXITCODE."
    }
    if (-not (Test-Path -LiteralPath $ModelFile)) {
        throw "COMSOL finished without creating '$ModelFile'. Check the COMSOL progress output above for Java/API errors."
    }
}

if (-not $SkipExport) {
    if (-not (Test-Path -LiteralPath $ModelFile)) {
        throw "Cannot export results because '$ModelFile' does not exist."
    }
    if (-not (Test-Path -LiteralPath $ExportScript)) {
        throw "Cannot find export script '$ExportScript'."
    }
    Write-Host "Compiling $ExportScript ..."
    & $comsolCompile $ExportScript
    if ($LASTEXITCODE -ne 0) {
        throw "COMSOL export Java compilation failed with exit code $LASTEXITCODE."
    }

    $exportClass = [System.IO.Path]::ChangeExtension($ExportScript, ".class")
    Write-Host "Exporting result images and CSV files ..."
    & $comsolBatch -inputfile $exportClass -outputfile "h2_porous_cylinder_model_with_exports.mph" -pname "modelPath" -plist $ModelFile
    if ($LASTEXITCODE -ne 0) {
        throw "COMSOL result export failed with exit code $LASTEXITCODE."
    }
    if (-not (Test-Path -LiteralPath "h2_porous_cylinder_model_with_exports.mph")) {
        throw "COMSOL finished without creating 'h2_porous_cylinder_model_with_exports.mph'. Check the COMSOL progress output above for export errors."
    }
}

Write-Host "Done. Model: $ModelFile"
Write-Host "Results directory: $(Join-Path $scriptDir 'results')"
