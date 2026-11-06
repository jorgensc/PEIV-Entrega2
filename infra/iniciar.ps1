<#
  iniciar.ps1 - coloca o JCondo no ar a partir deste computador

  Sobe, nesta ordem: MySQL (127.0.0.1:3306) -> JCondo (127.0.0.1:8080) -> tunel da Cloudflare.
  O tunel abre uma conexao de SAIDA ate a Cloudflare e devolve um endereco HTTPS publico
  (https://<nome>.trycloudflare.com). Nenhuma porta do roteador ou do firewall e aberta.

  Uso:
    .\infra\iniciar.ps1              # tudo, com link publico
    .\infra\iniciar.ps1 -SemTunel    # so na maquina (http://localhost:8080)
#>
param(
    [string]$Pasta = (Join-Path $env:USERPROFILE "JCondoServidor"),
    [switch]$SemTunel,
    [switch]$NaoAbrirNavegador
)

. "$PSScriptRoot\comum.ps1"
$c = Caminhos $Pasta
$config = LerConfig $c
$pids = LerPids $c
$porta = [int]$config.porta

Passo "MySQL"
if (PortaAberta 3306) {
    Write-Host "  ja estava rodando"
} else {
    $p = Start-Process (Join-Path $c.MysqlBin "mysqld.exe") -ArgumentList "--defaults-file=`"$($c.MyIni)`"" -WindowStyle Hidden -PassThru
    $pids.mysql = $p.Id
    EsperarPorta 3306 60 "MySQL"
    Write-Host "  no ar em 127.0.0.1:3306"
}

Passo "Aplicacao JCondo"
if (PortaAberta $porta) {
    Write-Host "  ja estava rodando na porta $porta"
} else {
    # variaveis de ambiente lidas pelo application.properties (a senha nao vai pro Git)
    $env:DB_URL = "jdbc:mysql://127.0.0.1:3306/jcondo?serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true&useSSL=false"
    $env:DB_USERNAME = $config.dbUsuario
    $env:DB_PASSWORD = $config.dbSenha
    $env:JWT_SECRET = $config.jwtSecret
    $env:SEED_DEMO = "true"
    $env:SHOW_SQL = "false"
    $env:TZ = "America/Sao_Paulo"
    $argumentos = "-Xms256m -Xmx512m -jar `"$($c.Jar)`" --server.address=127.0.0.1 --server.port=$porta"
    $p = Start-Process (JavaReal) -ArgumentList $argumentos -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $c.Logs "app.log") -RedirectStandardError (Join-Path $c.Logs "app-erros.log")
    $pids.app = $p.Id
    GravarPids $c $pids
    Write-Host "  subindo o Spring Boot (log em $($c.Logs)\app.log)..."
    $ok = $false
    for ($i = 0; $i -lt 120; $i++) {
        try {
            if ((Invoke-RestMethod "http://127.0.0.1:$porta/actuator/health" -TimeoutSec 3).status -eq "UP") { $ok = $true; break }
        } catch { }
        if (-not (Ativo $p.Id)) { throw "A aplicacao parou ao subir. Veja $($c.Logs)\app.log" }
        Start-Sleep -Seconds 1
    }
    if (-not $ok) { throw "A aplicacao nao respondeu o health check. Veja $($c.Logs)\app.log" }
    Write-Host "  no ar em http://localhost:$porta (health: UP)"
}

$urlPublica = $null
if (-not $SemTunel) {
    Passo "Tunel da Cloudflare (HTTPS publico)"
    if ((Ativo $pids.tunel) -and (Test-Path $c.Url)) {
        $urlPublica = (Get-Content $c.Url -Raw).Trim()
        Write-Host "  ja estava aberto"
    } else {
        $logTunel = Join-Path $c.Logs "tunel.log"
        Remove-Item $logTunel -ErrorAction SilentlyContinue
        $p = Start-Process $c.Cloudflared -ArgumentList "tunnel --no-autoupdate --url http://127.0.0.1:$porta" `
            -WindowStyle Hidden -PassThru -RedirectStandardError $logTunel -RedirectStandardOutput (Join-Path $c.Logs "tunel-saida.log")
        $pids.tunel = $p.Id
        GravarPids $c $pids
        for ($i = 0; $i -lt 60 -and -not $urlPublica; $i++) {
            Start-Sleep -Seconds 1
            if (Test-Path $logTunel) {
                $achou = Select-String -Path $logTunel -Pattern 'https://[a-z0-9-]+\.trycloudflare\.com' | Select-Object -First 1
                if ($achou) { $urlPublica = $achou.Matches[0].Value }
            }
        }
        if (-not $urlPublica) { throw "O tunel nao informou o endereco. Veja $logTunel" }
        GravarUtf8 $c.Url $urlPublica
        # espera o endereco novo propagar na Cloudflare
        for ($i = 0; $i -lt 30; $i++) {
            try {
                if ((Invoke-RestMethod "$urlPublica/actuator/health" -TimeoutSec 5).status -eq "UP") { break }
            } catch { }
            Start-Sleep -Seconds 2
        }
    }
}
GravarPids $c $pids

$base = if ($urlPublica) { $urlPublica } else { "http://localhost:$porta" }
Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " JCondo no ar" -ForegroundColor Green
Write-Host " Painel do sindico : $base/login"
Write-Host " App do morador    : $base/app/"
Write-Host " API (Swagger)     : $base/swagger-ui/index.html"
Write-Host " Health check      : $base/actuator/health"
Write-Host " Para desligar     : infra\parar.cmd"
Write-Host "============================================================" -ForegroundColor Green

if (-not $NaoAbrirNavegador) { Start-Process "$base/login" }
