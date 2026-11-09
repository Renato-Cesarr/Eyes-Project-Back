#Requires -Version 7.4
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^eyes-prod-[a-z0-9-]{3,20}$')][string] $ProjectId,
    [Parameter(Mandatory)][ValidatePattern('^[a-z]+-[a-z]+[0-9]+$')][string] $Region,
    [string] $Gcloud = 'gcloud'
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true

function Invoke-Cloud {
    & $Gcloud @args "--project=$ProjectId" --quiet
}

function New-Secret([string] $Id, [string] $Value) {
    $path = [IO.Path]::GetTempFileName()
    try {
        [IO.File]::WriteAllText($path, $Value)
        Invoke-Cloud secrets create $Id --replication-policy=automatic "--data-file=$path" --labels=application=our-eyes | Out-Null
    } finally {
        Remove-Item -LiteralPath $path
    }
}

$project = Invoke-Cloud projects describe $ProjectId --format=json | ConvertFrom-Json
if ($project.labels.application -ne 'our-eyes' -or $project.lifecycleState -ne 'ACTIVE') {
    throw 'Use um projeto Eyes dedicado e ativo com o rótulo application=our-eyes.'
}
$billing = Invoke-Cloud billing projects describe $ProjectId --format=json | ConvertFrom-Json
if (-not $billing.billingEnabled) {
    throw 'O projeto precisa estar associado à conta de faturamento autorizada para a demonstração.'
}

$apis = @('run.googleapis.com', 'sqladmin.googleapis.com', 'artifactregistry.googleapis.com',
    'secretmanager.googleapis.com', 'iam.googleapis.com', 'iamcredentials.googleapis.com',
    'sts.googleapis.com', 'cloudasset.googleapis.com')
Invoke-Cloud services enable @apis

$services = @(Invoke-Cloud run services list --platform=managed "--region=$Region" --format='value(metadata.name)')
$databases = @(Invoke-Cloud sql instances list --format='value(name)')
$registries = @(Invoke-Cloud artifacts repositories list "--location=$Region" --format='value(name)')
if ($services.Count -or $databases.Count -or $registries.Count) {
    throw 'Já há recursos de execução no projeto. Confira o inventário antes de retomar.'
}

$projectNumber = [string] $project.projectNumber
$apiAccount = "eyes-api-runtime@$ProjectId.iam.gserviceaccount.com"
$webAccount = "eyes-web-runtime@$ProjectId.iam.gserviceaccount.com"
$instance = 'eyes-postgres'
$dbName = 'eyes'
$dbUser = 'eyes_app'
$dbPassword = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$jwtSecret = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
$adminPassword = 'A1!' + [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))

$accounts = @(Invoke-Cloud iam service-accounts list --format='value(email)')
foreach ($name in 'eyes-api-runtime', 'eyes-web-runtime', 'eyes-api-deployer', 'eyes-web-deployer') {
    if ("$name@$ProjectId.iam.gserviceaccount.com" -notin $accounts) {
        Invoke-Cloud iam service-accounts create $name | Out-Null
    }
}
Invoke-Cloud projects add-iam-policy-binding $ProjectId "--member=serviceAccount:$apiAccount" --role=roles/cloudsql.client --condition=None | Out-Null

New-Secret eyes-db-password $dbPassword
New-Secret eyes-jwt $jwtSecret
New-Secret eyes-bootstrap-admin $adminPassword
foreach ($id in 'eyes-db-password', 'eyes-jwt', 'eyes-bootstrap-admin') {
    Invoke-Cloud secrets add-iam-policy-binding $id "--member=serviceAccount:$apiAccount" --role=roles/secretmanager.secretAccessor | Out-Null
}

Invoke-Cloud artifacts repositories create eyes-release "--location=$Region" --repository-format=docker --immutable-tags --description='Our Eyes release images'
$sql = @('sql', 'instances', 'create', $instance, "--region=$Region", '--database-version=POSTGRES_15',
    '--edition=ENTERPRISE', '--tier=db-f1-micro', '--storage-type=SSD', '--storage-size=10GB',
    '--availability-type=zonal', '--assign-ip', '--ssl-mode=ENCRYPTED_ONLY',
    '--connector-enforcement=REQUIRED', '--no-storage-auto-increase',
    '--backup-start-time=05:00', '--retained-backups-count=2', '--no-retain-backups-on-delete',
    '--deletion-protection')
Invoke-Cloud @sql
Invoke-Cloud sql databases create $dbName "--instance=$instance"
Invoke-Cloud sql users create $dbUser "--instance=$instance" "--password=$dbPassword"

[pscustomobject]@{
    project = $ProjectId
    region = $Region
    api = "https://eyes-api-$projectNumber.$Region.run.app"
    web = "https://eyes-web-$projectNumber.$Region.run.app"
    cloudSql = "${ProjectId}:${Region}:${instance}"
    runtimeApiIdentity = $apiAccount
    runtimeWebIdentity = $webAccount
}
