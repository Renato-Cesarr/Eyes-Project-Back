Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$script:LocalRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$script:LocalProject = 'eyes-local'
$script:LocalEnvFile = Join-Path $script:LocalRoot '.env'
function Initialize-LocalEnvironment {
    if (Test-Path -LiteralPath $script:LocalEnvFile) { return }
    $template = [IO.File]::ReadAllText((Join-Path $script:LocalRoot '.env.example'))
    while ($template.Contains('GENERATE_LOCAL_SECRET')) {
        $secret = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLowerInvariant()
        $position = $template.IndexOf('GENERATE_LOCAL_SECRET', [StringComparison]::Ordinal)
        $template = $template.Remove($position, 21).Insert($position, $secret)
    }
    [IO.File]::WriteAllText($script:LocalEnvFile, $template, [Text.UTF8Encoding]::new($false))
    Write-Host 'Arquivo .env local criado, com segredos aleatórios e bootstrap desativado.'
}
function Read-LocalEnvironment {
    if (!(Test-Path -LiteralPath $script:LocalEnvFile)) { throw 'Execute local-services.ps1 -Action init primeiro.' }
    $settings = @{}
    foreach ($line in [IO.File]::ReadAllLines($script:LocalEnvFile)) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) { continue }
        if ($line -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$') { throw 'Formato .env inválido: use CHAVE=valor sem aspas ou comandos.' }
        $key = $Matches[1]
        if ($settings.ContainsKey($key)) { throw "Chave duplicada no .env: $key" }
        $value = $Matches[2]
        if ($value.StartsWith('"') -or $value.StartsWith("'")) { throw "Valor com aspas não suportado: $key" }
        $settings[$key] = $value
    }
    foreach ($key in @('DB_HOST','SMTP_HOST')) {
        if ($settings[$key] -ne '127.0.0.1') { throw "$key deve ser 127.0.0.1 no ambiente local." }
    }
    foreach ($key in @('DB_NAME','DB_USER')) {
        if ($settings[$key] -ne 'eyes_local') { throw "$key deve ser eyes_local." }
    }
    $ports = @()
    foreach ($key in @('DB_PORT','SMTP_PORT','MAILPIT_HTTP_PORT','PORT')) {
        $port = 0
        if (![int]::TryParse($settings[$key], [ref]$port) -or $port -lt 1024 -or $port -gt 65535) { throw "Porta local inválida: $key" }
        $ports += $port
    }
    if (($ports | Select-Object -Unique).Count -ne 4) { throw 'As quatro portas locais devem ser diferentes.' }
    foreach ($key in @('DB_PASSWORD','JWT_SECRET','BOOTSTRAP_ADMIN_PASSWORD')) {
        if ([string]::IsNullOrWhiteSpace($settings[$key]) -or $settings[$key] -eq 'GENERATE_LOCAL_SECRET') { throw "Gere/preencha o segredo local: $key" }
    }
    return $settings
}
function Invoke-LocalCompose {
    param([Parameter(Mandatory)][string[]]$ComposeArguments)
    $configuration = Read-LocalEnvironment
    $previous = @{}
    try {
        foreach ($key in $configuration.Keys) {
            $previous[$key] = [Environment]::GetEnvironmentVariable($key,'Process')
            [Environment]::SetEnvironmentVariable($key,$configuration[$key],'Process')
        }
        & docker compose --project-name $script:LocalProject --project-directory $script:LocalRoot --env-file $script:LocalEnvFile --file (Join-Path $script:LocalRoot 'docker-compose.yml') @ComposeArguments
        if ($LASTEXITCODE -ne 0) { throw 'Docker Compose falhou; consulte o diagnóstico acima.' }
    } finally {
        foreach ($key in $previous.Keys) { [Environment]::SetEnvironmentVariable($key,$previous[$key],'Process') }
    }
}
function Assert-LocalHostReadiness {
    param([Parameter(Mandatory)][hashtable]$Configuration)
    $client = [Net.Sockets.TcpClient]::new()
    try {
        $connection = $client.ConnectAsync('127.0.0.1',[int]$Configuration['DB_PORT'])
        if (!$connection.Wait(5000) -or !$client.Connected) { throw 'PostgreSQL não está acessível pelo host.' }
    } finally { $client.Dispose() }
    $response = Invoke-WebRequest -Uri "http://127.0.0.1:$($Configuration['MAILPIT_HTTP_PORT'])/readyz" -TimeoutSec 5
    if ($response.StatusCode -ne 200) { throw 'Mailpit não está pronto pelo host.' }
}
function Assert-LocalOwnership {
    $ids = @(& docker ps -aq --filter "label=com.docker.compose.project=$script:LocalProject")
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível consultar o Docker.' }
    foreach ($id in $ids) {
        $container = (& docker inspect $id | ConvertFrom-Json)[0]
        $owner = $container.Config.Labels.'com.docker.compose.project.working_dir'
        if (![string]::Equals($owner, $script:LocalRoot.Replace('\','/'), [StringComparison]::OrdinalIgnoreCase) -and
            ![string]::Equals($owner, $script:LocalRoot, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'eyes-local já pertence a outro checkout. Pare esse ambiente no checkout proprietário antes de continuar.'
        }
    }
}
