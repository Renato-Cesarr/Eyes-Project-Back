[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$expectedJavaMajor = 21
$expectedMaven = '3.9.14'
$repositoryRoot = Split-Path -Parent $PSScriptRoot

function Fail([string]$Message) {
    throw "[toolchain] $Message"
}

$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCommand) {
    Fail 'Java não encontrado no PATH. Instale um JDK 21 e configure JAVA_HOME.'
}

$javaOutput = (& java -version 2>&1 | Out-String)
if ($javaOutput -notmatch 'version "(?<major>\d+)') {
    Fail 'Não foi possível identificar a versão do Java.'
}
if ([int]$Matches.major -ne $expectedJavaMajor) {
    Fail "Java $expectedJavaMajor é obrigatório; encontrado: $($Matches.major)."
}

$declaredJava = (Get-Content (Join-Path $repositoryRoot '.java-version') -Raw).Trim()
if ($declaredJava -ne "$expectedJavaMajor") {
    Fail ".java-version deve declarar $expectedJavaMajor; encontrado: $declaredJava."
}

$wrapper = if ($IsWindows -or $env:OS -eq 'Windows_NT') {
    Join-Path $repositoryRoot 'mvnw.cmd'
} else {
    Join-Path $repositoryRoot 'mvnw'
}
if (-not (Test-Path $wrapper)) {
    Fail 'Maven Wrapper não encontrado.'
}

$mavenOutput = (& $wrapper --version 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0) {
    Fail 'O Maven Wrapper não pôde ser executado.'
}
if ($mavenOutput -notmatch "Apache Maven $([regex]::Escape($expectedMaven))") {
    Fail "Maven Wrapper deve resolver a versão $expectedMaven."
}
if ($mavenOutput -notmatch 'Java version: 21(?:\.|,)') {
    Fail 'O Maven Wrapper não está usando Java 21.'
}

Write-Host "[toolchain] OK - Java $expectedJavaMajor e Maven $expectedMaven."
