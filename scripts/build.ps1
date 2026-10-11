param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GradleArgs = @('build'))
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $localJdk = Get-ChildItem -Path '.tools\jdk' -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
    if ($env:JAVA_HOME) { $env:PATH = "$env:JAVA_HOME\bin;$env:PATH" }
    $env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-user-home'
    # Windows AF_UNIX sockets need a short temp path; Codex's sandbox path can exceed the limit.
    New-Item -ItemType Directory -Force '.tools\tmp' | Out-Null
    $env:TEMP = Join-Path $projectRoot '.tools\tmp'
    $env:TMP = $env:TEMP
    & '.\gradlew.bat' @GradleArgs --no-daemon
    $buildExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $buildExitCode
