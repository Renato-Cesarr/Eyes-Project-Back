#Requires -Version 7.4
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^eyes-prod-[a-z0-9-]{3,20}$')][string] $ProjectId,
    [Parameter(Mandatory)][ValidatePattern('^[a-z]+-[a-z]+[0-9]+$')][string] $Region,
    [Parameter(Mandatory)][string] $ApiImage,
    [Parameter(Mandatory)][string] $WebImage,
    [Parameter(Mandatory)][ValidatePattern('^[a-f0-9]{40}$')][string] $ApiCommit,
    [Parameter(Mandatory)][ValidatePattern('^[a-f0-9]{40}$')][string] $WebCommit,
    [switch] $BootstrapSyntheticAdmin,
    [string] $Gcloud = 'gcloud'
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true

function Invoke-Cloud {
    & $Gcloud @args "--project=$ProjectId" --quiet
}

$project = Invoke-Cloud projects describe $ProjectId --format=json | ConvertFrom-Json
if ($project.labels.application -ne 'our-eyes') {
    throw 'Use um projeto dedicado com o rótulo application=our-eyes.'
}
$registry = "$Region-docker.pkg.dev/$ProjectId/eyes-release"
foreach ($component in @{ 'eyes-api' = $ApiImage; 'eyes-web' = $WebImage }.GetEnumerator()) {
    $expected = [regex]::Escape("$registry/$($component.Key)@sha256:")
    if ($component.Value -cnotmatch "^${expected}[a-f0-9]{64}$") {
        throw "Informe um digest imutável do projeto para $($component.Key)."
    }
}
$instance = Invoke-Cloud sql instances describe eyes-postgres --format=json | ConvertFrom-Json
if ($instance.region -ne $Region -or $instance.state -ne 'RUNNABLE' -or
    $instance.settings.connectorEnforcement -ne 'REQUIRED') {
    throw 'O PostgreSQL da demonstração precisa estar disponível na mesma região.'
}

$webOrigin = "https://eyes-web-$($project.projectNumber).$Region.run.app"
$apiEnvironment = "SPRING_PROFILES_ACTIVE=prod,FRONTEND_URL=$webOrigin,CLOUD_SQL_CONNECTION_NAME=$($instance.connectionName),DB_NAME=eyes,DB_USER=eyes_app"
$apiSecrets = 'DB_PASSWORD=eyes-db-password:1,JWT_SECRET=eyes-jwt:1'
if ($BootstrapSyntheticAdmin) {
    $apiEnvironment += ',BOOTSTRAP_ADMIN_ENABLED=true,BOOTSTRAP_ADMIN_NAME=Administrador de QA,BOOTSTRAP_ADMIN_EMAIL=admin-qa@eyes.example.invalid'
    $apiSecrets += ',BOOTSTRAP_ADMIN_PASSWORD=eyes-bootstrap-admin:1'
}

$common = @("--region=$Region", '--port=8080', '--execution-environment=gen2',
    '--no-invoker-iam-check', '--min=0', '--max=2', '--timeout=60s')
$api = @("run", "deploy", "eyes-api", "--image=$ApiImage",
    "--service-account=eyes-api-runtime@$ProjectId.iam.gserviceaccount.com",
    '--cpu=1', '--memory=768Mi', '--concurrency=20',
    "--labels=application=our-eyes,source-sha=$ApiCommit",
    "--set-env-vars=$apiEnvironment", "--set-secrets=$apiSecrets",
    '--startup-probe=httpGet.path=/actuator/health,httpGet.port=8080,timeoutSeconds=3,periodSeconds=5,failureThreshold=24')
Invoke-Cloud @api @common
$web = @("run", "deploy", "eyes-web", "--image=$WebImage",
    "--service-account=eyes-web-runtime@$ProjectId.iam.gserviceaccount.com",
    '--cpu=1', '--memory=256Mi', '--concurrency=80',
    "--labels=application=our-eyes,source-sha=$WebCommit")
Invoke-Cloud @web @common
