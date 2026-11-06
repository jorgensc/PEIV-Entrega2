<#
  atualizar.ps1 - publica uma versao nova do JCondo no servidor

  Por padrao baixa o jcondo.jar da ultima Release do GitHub (gerada pelo pipeline
  depois que os testes passam). Com -Local usa o .jar compilado neste repositorio.
  O banco nao e tocado: os dados continuam.

  Uso:
    .\infra\atualizar.ps1
    .\infra\atualizar.ps1 -Local
#>
param(
    [string]$Pasta = (Join-Path $env:USERPROFILE "JCondoServidor"),
    [switch]$Local
)

. "$PSScriptRoot\comum.ps1"
$c = Caminhos $Pasta
$novo = Join-Path $c.App "jcondo.jar.novo"

if ($Local) {
    Passo "Gerando o .jar local"
    $repo = Split-Path $PSScriptRoot -Parent
    Push-Location (Join-Path $repo "app")
    try {
        & .\mvnw.cmd -q -B package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw "Falha ao gerar o .jar" }
    } finally {
        Pop-Location
    }
    Copy-Item (Join-Path $repo "app\target\jcondo.jar") $novo -Force
} else {
    Passo "Baixando a ultima versao publicada no GitHub"
    Invoke-WebRequest $Versoes.jarRelease -OutFile $novo -UseBasicParsing
}

Passo "Trocando a versao"
$pids = LerPids $c
$config = LerConfig $c
if (Ativo $pids.app) { Stop-Process -Id $pids.app -Force }
PararPorta ([int]$config.porta)
$pids.app = $null
GravarPids $c $pids
if (Test-Path $c.Jar) { Copy-Item $c.Jar (Join-Path $c.App "jcondo.jar.anterior") -Force }
# o Windows pode segurar o .jar por alguns segundos depois que o Java fecha
$trocou = $false
for ($i = 0; $i -lt 15 -and -not $trocou; $i++) {
    try {
        Copy-Item $novo $c.Jar -Force
        $trocou = $true
    } catch {
        Start-Sleep -Seconds 1
    }
}
if (-not $trocou) { throw "Nao consegui substituir $($c.Jar). Rode .\infra\parar.ps1 e tente de novo." }
Remove-Item $novo -ErrorAction SilentlyContinue

# sobe de novo (o tunel continua o mesmo, entao o link publico nao muda)
& "$PSScriptRoot\iniciar.ps1" -Pasta $Pasta -NaoAbrirNavegador
