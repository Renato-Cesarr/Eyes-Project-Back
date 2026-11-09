#Requires -Version 7.4
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^eyes-prod-[a-z0-9-]{3,20}$')][string] $ProjectId,
    [Parameter(Mandatory)][ValidatePattern('^[a-z]+-[a-z]+[0-9]+$')][string] $Region,
    [Parameter(Mandatory)][string] $ConfirmProjectId,
    [Parameter(Mandatory)][string] $EvidenceDirectory,
    [switch] $FailedProvisioning,
    [switch] $UnlinkBilling,
    [string] $Gcloud = 'gcloud'
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true
if ($ProjectId -cne $ConfirmProjectId) { throw 'Confirme o ID exato do projeto.' }
if (-not $FailedProvisioning -and -not (Test-Path -LiteralPath (Join-Path $EvidenceDirectory 'deployment.json') -PathType Leaf)) {
    throw 'Preserve primeiro o inventário da implantação em deployment.json.'
}

function Invoke-Cloud {
    & $Gcloud @args "--project=$ProjectId" --quiet
}

$project = Invoke-Cloud projects describe $ProjectId --format=json | ConvertFrom-Json
if ($project.labels.application -ne 'our-eyes') {
    throw 'A limpeza só é permitida no projeto dedicado com o rótulo application=our-eyes.'
}
$cloudRun = @(Invoke-Cloud run services list --platform=managed "--region=$Region" --format=json | ConvertFrom-Json -NoEnumerate)[0]
$cloudSql = @(Invoke-Cloud sql instances list --format=json | ConvertFrom-Json -NoEnumerate)[0]
$artifactRegistry = @(Invoke-Cloud artifacts repositories list "--location=$Region" --format=json | ConvertFrom-Json -NoEnumerate)[0]
$secrets = @(Invoke-Cloud secrets list --format=json | ConvertFrom-Json -NoEnumerate)[0]
if ($FailedProvisioning) {
    New-Item -ItemType Directory -Path $EvidenceDirectory -Force | Out-Null
    [ordered]@{
        project = $ProjectId
        region = $Region
        services = @($cloudRun | ForEach-Object { $_.metadata.name })
        instances = @($cloudSql | ForEach-Object { $_.name })
        repositories = @($artifactRegistry | ForEach-Object { $_.name })
        secrets = @($secrets | ForEach-Object { $_.name })
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $EvidenceDirectory 'partial-runtime.json') -Encoding utf8
} elseif ((Get-Content -LiteralPath (Join-Path $EvidenceDirectory 'deployment.json') -Raw | ConvertFrom-Json).project -cne $ProjectId) {
    throw 'O inventário salvo deve corresponder ao projeto que será desligado.'
}

foreach ($repo in 'Eyes-Project-Back', 'Eyes-Project-Front') {
    gh variable set GCP_DEPLOY_ENABLED --body false --repo "Renato-Cesarr/$repo"
    $deployments = @(gh run list --branch main --repo "Renato-Cesarr/$repo" --limit 30 --json status |
        ConvertFrom-Json | Where-Object status -ne 'completed')
    if ($deployments.Count) { throw "Aguarde as execuções ativas de main em $repo antes da limpeza." }
}

foreach ($service in $cloudRun | Where-Object {
        $_.metadata.labels.application -eq 'our-eyes' -and $_.metadata.name -in 'eyes-web', 'eyes-api'
    }) {
    Invoke-Cloud run services delete $service.metadata.name "--region=$Region"
}
foreach ($db in $cloudSql | Where-Object {
        $_.region -eq $Region -and $_.name -eq 'eyes-postgres'
    }) {
    Invoke-Cloud sql instances patch $db.name --no-deletion-protection --no-retain-backups-on-delete
    Invoke-Cloud sql instances delete $db.name
}
foreach ($repository in $artifactRegistry | Where-Object {
        $_.name -like '*/repositories/eyes-release'
    }) {
    Invoke-Cloud artifacts repositories delete eyes-release "--location=$Region"
}
foreach ($secret in $secrets | Where-Object {
        $_.labels.application -eq 'our-eyes' -and ($_.name -split '/')[-1] -in 'eyes-db-password', 'eyes-jwt', 'eyes-bootstrap-admin'
    }) {
    Invoke-Cloud secrets delete (($secret.name -split '/')[-1])
}

$remaining = [ordered]@{
    project = $ProjectId
    checkedAt = (Get-Date).ToUniversalTime().ToString('o')
    services = @(Invoke-Cloud run services list --platform=managed "--region=$Region" --format='value(metadata.name)')
    instances = @(Invoke-Cloud sql instances list --format='value(name)')
    backups = @(Invoke-Cloud sql backups list --instance=- --format='value(id)')
    registries = @(Invoke-Cloud artifacts repositories list "--location=$Region" --format='value(name)')
    secrets = @(Invoke-Cloud secrets list --format='value(name)')
}
$destination = Join-Path $EvidenceDirectory 'teardown.json'
$remaining | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $destination -Encoding utf8
if ($remaining.services.Count -or $remaining.instances.Count -or $remaining.backups.Count -or
    $remaining.registries.Count -or $remaining.secrets.Count) {
    throw "Ainda existem recursos. Confira $destination antes de encerrar."
}
if ($UnlinkBilling) {
    Invoke-Cloud billing projects unlink $ProjectId
}
$remaining.billingEnabled = (Invoke-Cloud billing projects describe $ProjectId --format=json | ConvertFrom-Json).billingEnabled
$remaining | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $destination -Encoding utf8
