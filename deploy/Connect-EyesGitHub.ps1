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

$project = Invoke-Cloud projects describe $ProjectId --format=json | ConvertFrom-Json
if ($project.labels.application -ne 'our-eyes') { throw 'Projeto Eyes dedicado obrigatório.' }

$pool = 'eyes-github'
$provider = 'github-main'
$poolResource = "projects/$($project.projectNumber)/locations/global/workloadIdentityPools/$pool"
$mapping = 'google.subject=assertion.sub,attribute.repository_id=assertion.repository_id'
$condition = "assertion.repository_owner_id == '163927189' && assertion.repository_id in ['1191014671', '1190939095'] && assertion.ref == 'refs/heads/main' && assertion.environment == 'production' && assertion.workflow_ref == assertion.repository + '/.github/workflows/ci.yml@refs/heads/main'"
$pools = @(Invoke-Cloud iam workload-identity-pools list --location=global --format='value(name)')
if ($poolResource -notin $pools) {
    Invoke-Cloud iam workload-identity-pools create $pool --location=global --display-name='Eyes GitHub Actions' | Out-Null
}
$providerResource = "$poolResource/providers/$provider"
$providers = @(Invoke-Cloud iam workload-identity-pools providers list --location=global "--workload-identity-pool=$pool" --format='value(name)')
$operation = if ($providerResource -in $providers) { 'update-oidc' } else { 'create-oidc' }
$oidcArgs = @('iam', 'workload-identity-pools', 'providers', $operation, $provider, '--location=global',
    "--workload-identity-pool=$pool", "--attribute-mapping=$mapping", "--attribute-condition=$condition")
if ($operation -eq 'create-oidc') {
    $oidcArgs += '--issuer-uri=https://token.actions.githubusercontent.com'
}
Invoke-Cloud @oidcArgs | Out-Null

$targets = @(
    @{ repo = 'Eyes-Project-Back'; id = '1191014671'; service = 'eyes-api' },
    @{ repo = 'Eyes-Project-Front'; id = '1190939095'; service = 'eyes-web' }
)
foreach ($target in $targets) {
    $service = $target.service
    $repo = "Renato-Cesarr/$($target.repo)"
    $identity = gh api "repos/$repo" --jq '.id'
    if ($identity -ne $target.id) { throw "ID do repositório não corresponde a $repo." }

    $deployer = "$service-deployer@$ProjectId.iam.gserviceaccount.com"
    $runtime = "$service-runtime@$ProjectId.iam.gserviceaccount.com"
    $member = "serviceAccount:$deployer"
    $principal = "principalSet://iam.googleapis.com/$poolResource/attribute.repository_id/$identity"
    Invoke-Cloud iam service-accounts add-iam-policy-binding $deployer "--member=$principal" --role=roles/iam.workloadIdentityUser | Out-Null
    Invoke-Cloud iam service-accounts add-iam-policy-binding $runtime "--member=$member" --role=roles/iam.serviceAccountUser | Out-Null
    Invoke-Cloud artifacts repositories add-iam-policy-binding eyes-release "--location=$Region" "--member=$member" --role=roles/artifactregistry.writer | Out-Null
    Invoke-Cloud run services add-iam-policy-binding $service "--region=$Region" "--member=$member" --role=roles/run.developer | Out-Null

    gh variable set GCP_DEPLOY_ENABLED --repo $repo --body false
    gh api --method PUT "repos/$repo/environments/production" -F 'deployment_branch_policy[protected_branches]=false' -F 'deployment_branch_policy[custom_branch_policies]=true' --silent
    $policy = @(gh api "repos/$repo/environments/production/deployment-branch-policies" --jq '.branch_policies[] | select(.name == "main" and .type == "branch") | .id')
    if (-not $policy) {
        gh api --method POST "repos/$repo/environments/production/deployment-branch-policies" -f name=main -f type=branch --silent
    }

    $variables = @{
        GCP_PROJECT_ID = $ProjectId
        GCP_REGION = $Region
        GCP_WIF_PROVIDER = $providerResource
        GCP_DEPLOYER = $deployer
    }
    if ($service -eq 'eyes-web') {
        $variables.GCP_API_ORIGIN = "https://eyes-api-$($project.projectNumber).$Region.run.app"
    }
    foreach ($entry in $variables.GetEnumerator()) {
        gh variable set $entry.Key --repo $repo --env production --body $entry.Value
    }
}
