#requires -Version 7.0
[CmdletBinding()]
param([string]$OutputPath = '')
. (Join-Path $PSScriptRoot 'local-common.ps1')
$settings = Read-LocalEnvironment
Assert-LocalOwnership
Assert-LocalHostReadiness -Configuration $settings
if (!$OutputPath) { $OutputPath = Join-Path $script:LocalRoot 'logs/local-verification.json' }
$api = "http://127.0.0.1:$($settings['PORT'])/api/v1"
$mail = "http://127.0.0.1:$($settings['MAILPIT_HTTP_PORT'])"
function Invoke-LocalApi {
    param([string]$Route,[hashtable]$Body,[hashtable]$Headers = @{})
    Invoke-RestMethod -Method Post -Uri "$api/$Route" -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress) -Headers $Headers -TimeoutSec 15
}
function Wait-LocalEmail {
    param([string]$Recipient,[string]$Path)
    $query = [Uri]::EscapeDataString("to:$Recipient")
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        $search = Invoke-RestMethod -Uri "$mail/api/v1/search?query=$query" -TimeoutSec 5
        foreach ($entry in $search.messages) {
            $message = Invoke-RestMethod -Uri "$mail/api/v1/message/$($entry.ID)" -TimeoutSec 5
            if ($message.HTML.Contains($Path)) { return $message }
        }
        Start-Sleep -Milliseconds 500
    }
    throw 'E-mail esperado não foi capturado no Mailpit local.'
}
function Get-EmailToken {
    param($Message,[string]$Path)
    $link = [regex]::Match($Message.HTML, 'href="([^"<>]+)"')
    if (!$link.Success) { throw 'Link de e-mail ausente.' }
    $uri = [Uri][Net.WebUtility]::HtmlDecode($link.Groups[1].Value)
    $frontend = [Uri]$settings['FRONTEND_URL']
    if ($uri.Authority -ne $frontend.Authority -or $uri.Scheme -ne $frontend.Scheme -or $uri.AbsolutePath -ne $Path) { throw 'Link de e-mail diverge do frontend local configurado.' }
    $tokenMatch = [regex]::Match($uri.Query, '(?:[?&])token=([^&]+)')
    if (!$tokenMatch.Success) { throw 'Token de e-mail ausente.' }
    return [Uri]::UnescapeDataString($tokenMatch.Groups[1].Value)
}
# Somente o projeto/contas locais: nenhum mock, envio externo ou dados produtivos.
$login = Invoke-LocalApi -Route 'auth/login' -Body @{ email=$settings['BOOTSTRAP_ADMIN_EMAIL'];password=$settings['BOOTSTRAP_ADMIN_PASSWORD'] }
if ($login.user.role -ne 'ADMIN') { throw 'Bootstrap local não está disponível. Inicie start-local-api.ps1 -BootstrapAdmin no banco local vazio.' }
$adminHeaders = @{ Authorization="Bearer $($login.token)" }
$studentEmail = "ren39-$([guid]::NewGuid().ToString('N'))@eyes.test"
$initialPassword = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(16))
$resetPassword = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(16))
$null = Invoke-LocalApi -Route 'users' -Body @{ name='Validação local REN-39';email=$studentEmail } -Headers $adminHeaders
$invitation = Wait-LocalEmail -Recipient $studentEmail -Path '/setup-password'
$setupToken = Get-EmailToken -Message $invitation -Path '/setup-password'
$null = Invoke-LocalApi -Route 'users/setup-password' -Body @{ token=$setupToken;password=$initialPassword }
$student = Invoke-LocalApi -Route 'auth/login' -Body @{ email=$studentEmail;password=$initialPassword }
if ($student.user.role -ne 'STUDENT') { throw 'Conta convidada não ativou como STUDENT.' }
$null = Invoke-LocalApi -Route 'auth/forgot-password' -Body @{ email=$studentEmail }
$recovery = Wait-LocalEmail -Recipient $studentEmail -Path '/reset-password'
$resetToken = Get-EmailToken -Message $recovery -Path '/reset-password'
$null = Invoke-LocalApi -Route 'auth/reset-password' -Body @{ token=$resetToken;password=$resetPassword }
$recovered = Invoke-LocalApi -Route 'auth/login' -Body @{ email=$studentEmail;password=$resetPassword }
if ($recovered.user.id -ne $student.user.id) { throw 'Recuperação alterou a identidade da conta.' }
$migrations = @(& docker exec eyes-local-postgres-1 psql -U eyes_local -d eyes_local -At -c 'SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank;')
if ($LASTEXITCODE -ne 0 -or ($migrations -join ',') -ne '1,2,3,4,5') { throw 'Migrations Flyway locais não correspondem à versão atual.' }
$receipt = [ordered]@{
    verifiedAt = [DateTime]::UtcNow.ToString('o')
    sourceCommit = (& git -C $script:LocalRoot rev-parse HEAD)
    sourceDirty = (@(& git -C $script:LocalRoot status --porcelain).Count -gt 0)
    project = $script:LocalProject
    scope = 'backend + PostgreSQL + SMTP/Mailpit; frontend/mobile not executed'
    migrations = $migrations
    mailpitReady = $true
    administratorLogin = 'passed'
    invitationCaptured = 'passed'
    accountActivation = 'passed'
    studentLogin = 'passed'
    recoveryCaptured = 'passed'
    passwordReset = 'passed'
    recoveredIdentityPreserved = $true
    messageIds = @($invitation.ID,$recovery.ID)
    secretsIncluded = $false
}
$resolvedOutput = [IO.Path]::GetFullPath($OutputPath)
$null = [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($resolvedOutput))
[IO.File]::WriteAllText($resolvedOutput, ($receipt | ConvertTo-Json -Depth 5), [Text.UTF8Encoding]::new($false))
Write-Host "Prova local concluída; recibo sem credenciais/tokens: $resolvedOutput"
