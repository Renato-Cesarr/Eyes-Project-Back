#Requires -Version 7.4
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true

$targets = @{
    'Eyes-Project-Back' = 'Build + Test + Sonar'
    'Eyes-Project-Front' = 'Build + Test + Sonar Front'
}
foreach ($target in $targets.GetEnumerator()) {
    $repo = "Renato-Cesarr/$($target.Key)"
    $name = 'Eyes production main'
    $base = "repos/$repo/rulesets"
    $existing = @(gh api $base --paginate --jq ".[] | select(.name == `"$name`") | .id")
    if ($existing.Count -gt 1) { throw "Há regras duplicadas em $repo." }
    $rules = @{
        name = $name
        target = 'branch'
        enforcement = 'active'
        bypass_actors = @()
        conditions = @{ ref_name = @{ include = @('refs/heads/main'); exclude = @() } }
        rules = @(
            @{ type = 'deletion' },
            @{ type = 'non_fast_forward' },
            @{ type = 'pull_request'; parameters = @{
                required_approving_review_count = 0
                dismiss_stale_reviews_on_push = $false
                require_code_owner_review = $false
                require_last_push_approval = $false
                required_review_thread_resolution = $true
                allowed_merge_methods = @('merge')
            }},
            @{ type = 'required_status_checks'; parameters = @{
                strict_required_status_checks_policy = $false
                required_status_checks = @(@{ context = $target.Value; integration_id = 15368 })
            }}
        )
    } | ConvertTo-Json -Depth 10
    if ($existing.Count) {
        $rules | gh api --method PUT "$base/$($existing[0])" --input - --jq '{id, name, enforcement}'
    } else {
        $rules | gh api --method POST $base --input - --jq '{id, name, enforcement}'
    }
    gh variable set GCP_DEPLOY_ENABLED --repo $repo --body false
}
