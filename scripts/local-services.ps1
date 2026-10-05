#requires -Version 7.0
[CmdletBinding()]
param(
    [ValidateSet('init','start','status','stop','reset')][string]$Action = 'start',
    [string]$ConfirmReset = ''
)
. (Join-Path $PSScriptRoot 'local-common.ps1')
if ($Action -in @('init','start')) { Initialize-LocalEnvironment }
$settings = Read-LocalEnvironment
Assert-LocalOwnership
if ($Action -eq 'init') { Write-Host 'Configuração local pronta; .env existente foi preservado.'; return }
if ($Action -eq 'start') {
    Invoke-LocalCompose -ComposeArguments @('config','--quiet')
    Invoke-LocalCompose -ComposeArguments @('up','--wait','--wait-timeout','120')
    Assert-LocalHostReadiness -Configuration $settings
    Write-Host "Serviços saudáveis. Mailpit: http://127.0.0.1:$($settings['MAILPIT_HTTP_PORT'])"
} elseif ($Action -eq 'status') {
    Invoke-LocalCompose -ComposeArguments @('ps')
} elseif ($Action -eq 'stop') {
    Invoke-LocalCompose -ComposeArguments @('down')
    Write-Host 'Serviços parados. Dados de banco/e-mail e .env preservados.'
} elseif ($Action -eq 'reset') {
    if ($ConfirmReset -cne $script:LocalProject) { throw 'Reset exige -ConfirmReset eyes-local; apaga somente os dois volumes deste ambiente.' }
    $ownedVolumes = @()
    foreach ($name in @('eyes-local_postgres-data','eyes-local_mailpit-data')) {
        $exists = @(& docker volume ls --format '{{.Name}}' --filter "name=^$name$")
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao consultar volumes.' }
        if ($exists -contains $name) {
            $volume = (& docker volume inspect $name | ConvertFrom-Json)[0]
            if ($volume.Labels.'com.docker.compose.project' -ne $script:LocalProject) { throw 'Volume sem propriedade eyes-local; reset interrompido.' }
            $ownedVolumes += $name
        }
    }
    Invoke-LocalCompose -ComposeArguments @('down')
    foreach ($name in $ownedVolumes) {
        & docker volume rm $name
        if ($LASTEXITCODE -ne 0) { throw 'Volume em uso ou falha ao remover volume local.' }
    }
    Write-Host 'Somente volumes eyes-local removidos. .env preservado; execute start para recriar.'
}
