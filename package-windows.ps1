param(
    [string]$JdkHome = "C:\Users\Simon-Pier\.jdks\ms-21.0.9"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$maven = Join-Path $root "mvnw.cmd"
$javaHome = (Resolve-Path $JdkHome).Path
$jpackage = Join-Path $javaHome "bin\jpackage.exe"
$output = Join-Path $root "dist"
$inputDir = Join-Path $root "craftboard-desktop\target\jpackage-input"
$mainJar = "craftboard-desktop-1.0-SNAPSHOT.jar"

if (-not (Test-Path $jpackage)) {
    throw "jpackage.exe introuvable dans $javaHome"
}

$env:JAVA_HOME = $javaHome
$env:Path = (Join-Path $javaHome "bin") + [IO.Path]::PathSeparator + $env:Path

& $maven clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (Test-Path $output) {
    Remove-Item -LiteralPath $output -Recurse -Force
}

& $jpackage `
    --type app-image `
    --name CraftBoard `
    --input $inputDir `
    --main-jar $mainJar `
    --dest $output `
    --win-console

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Executable cree: $output\CraftBoard\CraftBoard.exe"
