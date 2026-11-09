#Requires -Version 7.4
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^eyes-prod-[a-z0-9-]{3,20}$')][string] $ProjectId,
    [Parameter(Mandatory)][ValidatePattern('^[a-z]+-[a-z]+[0-9]+$')][string] $Region,
    [Parameter(Mandatory)][ValidatePattern('^[a-f0-9]{40}$')][string] $ApiCommit,
    [Parameter(Mandatory)][ValidatePattern('^[a-f0-9]{40}$')][string] $WebCommit,
    [Parameter(Mandatory)][string] $EvidenceDirectory,
    [switch] $VerifySyntheticAdmin,
    [string] $Gcloud = 'gcloud'
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true

function Invoke-Cloud {
    & $Gcloud @args "--project=$ProjectId" --quiet
}

function Read-JsonResponse($Response) {
    [Text.Encoding]::UTF8.GetString($Response.RawContentStream.ToArray()) | ConvertFrom-Json
}

function Get-DeployedService([string] $Name, [string] $Commit) {
    $service = Invoke-Cloud run services describe $Name "--region=$Region" --format=json | ConvertFrom-Json
    if ($service.metadata.labels.application -ne 'our-eyes') { throw "Serviço inesperado: $Name." }
    $ready = $service.status.latestReadyRevisionName
    $revision = Invoke-Cloud run revisions describe $ready "--region=$Region" --format=json | ConvertFrom-Json
    if ($revision.metadata.labels.'source-sha' -cne $Commit -or
        $revision.spec.containers[0].image -cnotmatch '@sha256:[a-f0-9]{64}$') {
        throw "A revisão $ready não corresponde ao commit e digest esperados."
    }
    [ordered]@{
        name = $Name
        url = $service.status.url
        revision = $ready
        trafficPercent = ($service.status.traffic | Where-Object revisionName -eq $ready | Measure-Object percent -Sum).Sum
        image = $revision.spec.containers[0].image
        commit = $Commit
    }
}

$project = Invoke-Cloud projects describe $ProjectId --format=json | ConvertFrom-Json
if ($project.labels.application -ne 'our-eyes') { throw 'Projeto Eyes dedicado obrigatório.' }
$api = Get-DeployedService 'eyes-api' $ApiCommit
$web = Get-DeployedService 'eyes-web' $WebCommit
$webOrigin = "https://eyes-web-$($project.projectNumber).$Region.run.app"
$apiOrigin = "https://eyes-api-$($project.projectNumber).$Region.run.app"
$instance = Invoke-Cloud sql instances describe eyes-postgres --format=json | ConvertFrom-Json
$proof = [ordered]@{
    project = $ProjectId
    region = $Region
    collectedAt = (Get-Date).ToUniversalTime().ToString('o')
    source = 'live-gcp'
    api = $api
    web = $web
    database = [ordered]@{
        instance = $instance.name
        region = $instance.region
        state = $instance.state
        version = $instance.databaseVersion
        tier = $instance.settings.tier
        connectorEnforcement = $instance.settings.connectorEnforcement
    }
    http = [ordered]@{}
}

New-Item -ItemType Directory -Path $EvidenceDirectory -Force | Out-Null
$destination = Join-Path $EvidenceDirectory 'deployment.json'
try {
    $health = Invoke-WebRequest "$apiOrigin/actuator/health" -SkipHttpErrorCheck -TimeoutSec 90
    $proof.http.health = $health.StatusCode
    $proof.http.healthStatus = (Read-JsonResponse $health).status
    $login = Invoke-WebRequest "$webOrigin/login" -SkipHttpErrorCheck -TimeoutSec 90
    $proof.http.webDeepLink = $login.StatusCode
    $proof.http.angularIndex = $login.Content.Contains('<app-root')
    $protected = Invoke-WebRequest "$apiOrigin/api/v1/auth/me" -SkipHttpErrorCheck -TimeoutSec 30
    $proof.http.unauthenticated = $protected.StatusCode
    $preflight = Invoke-WebRequest "$apiOrigin/api/v1/auth/login" -Method Options -SkipHttpErrorCheck -TimeoutSec 30 -Headers @{
        Origin = $webOrigin
        'Access-Control-Request-Method' = 'POST'
        'Access-Control-Request-Headers' = 'content-type'
    }
    $proof.http.corsAllowed = $preflight.Headers.'Access-Control-Allow-Origin' -contains $webOrigin
    if ($VerifySyntheticAdmin) {
        $password = Invoke-Cloud secrets versions access 1 --secret=eyes-bootstrap-admin
        $body = @{ email = 'admin-qa@eyes.example.invalid'; password = $password } | ConvertTo-Json -Compress
        $auth = Invoke-WebRequest "$apiOrigin/api/v1/auth/login" -Method Post -Body $body -ContentType 'application/json' -SkipHttpErrorCheck -TimeoutSec 30
        $proof.http.syntheticLogin = $auth.StatusCode
        if ($auth.StatusCode -eq 200) {
            $token = (Read-JsonResponse $auth).token
            $current = Invoke-WebRequest "$apiOrigin/api/v1/auth/me" -Headers @{ Authorization = "Bearer $token" } -SkipHttpErrorCheck -TimeoutSec 30
            $proof.http.authenticated = $current.StatusCode
            $proof.http.role = (Read-JsonResponse $current).role
        }
    }
    $proof.passed = $proof.http.health -eq 200 -and $proof.http.healthStatus -eq 'UP' -and
        $proof.http.webDeepLink -eq 200 -and $proof.http.angularIndex -and
        $proof.http.unauthenticated -eq 401 -and $proof.http.corsAllowed -and
        $api.trafficPercent -eq 100 -and $web.trafficPercent -eq 100 -and
        (-not $VerifySyntheticAdmin -or ($proof.http.authenticated -eq 200 -and $proof.http.role -eq 'ADMIN'))
} finally {
    $proof | ConvertTo-Json -Depth 7 | Set-Content -LiteralPath $destination -Encoding utf8
}
if (-not $proof.passed) { throw "Há verificações reprovadas em $destination." }
