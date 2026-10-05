#requires -Version 7.0
[CmdletBinding()]
param([switch]$BootstrapAdmin)
. (Join-Path $PSScriptRoot 'local-common.ps1')
& (Join-Path $PSScriptRoot 'local-services.ps1') -Action start
$settings = Read-LocalEnvironment
$previousEnvironment = @{}
$previousLocation = Get-Location
try {
    foreach ($key in $settings.Keys) {
        $previousEnvironment[$key] = [Environment]::GetEnvironmentVariable($key,'Process')
        [Environment]::SetEnvironmentVariable($key,$settings[$key],'Process')
    }
    if ($BootstrapAdmin) { $env:BOOTSTRAP_ADMIN_ENABLED = 'true' }
    Set-Location -LiteralPath $script:LocalRoot
    & (Join-Path $script:LocalRoot 'scripts/check-toolchain.ps1')
    $localArguments = @('--server.address=127.0.0.1',
        "--server.port=$($settings['PORT'])",
        "--spring.datasource.url=jdbc:postgresql://127.0.0.1:$($settings['DB_PORT'])/eyes_local",
        '--spring.datasource.username=eyes_local','--spring.mail.host=127.0.0.1',
        "--spring.mail.port=$($settings['SMTP_PORT'])")
    $runArguments = '-Dspring-boot.run.arguments=' + ($localArguments -join ' ')
    if ($IsWindows) {
        & (Join-Path $script:LocalRoot 'mvnw.cmd') '-Dspring-boot.run.profiles=local' $runArguments spring-boot:run
    } else {
        & (Join-Path $script:LocalRoot 'mvnw') '-Dspring-boot.run.profiles=local' $runArguments spring-boot:run
    }
    if ($LASTEXITCODE -ne 0) { throw 'API local encerrou com erro.' }
} finally {
    Set-Location -LiteralPath $previousLocation.Path
    foreach ($key in $previousEnvironment.Keys) { [Environment]::SetEnvironmentVariable($key,$previousEnvironment[$key],'Process') }
}
