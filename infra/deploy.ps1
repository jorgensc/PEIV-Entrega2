<#
  deploy.ps1 - cria a infraestrutura do JCondo no Azure e publica a aplicacao

  Pre-requisitos: Azure CLI (winget install Microsoft.AzureCLI), JDK 21 e login feito com "az login".

  Uso (na raiz do repositorio):
    .\infra\deploy.ps1                         # plano F1 (gratuito), regiao escolhida pela politica da assinatura
    .\infra\deploy.ps1 -Plano B1               # plano basico com Always On
    .\infra\deploy.ps1 -Regiao eastus          # forca uma regiao
    .\infra\deploy.ps1 -SoAplicacao            # so gera o .jar e publica de novo
#>
param(
    [string]$GrupoRecursos = "rg-jcondo",
    [string]$Regiao = "",
    [ValidateSet("F1", "B1")][string]$Plano = "F1",
    [switch]$SoAplicacao
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path $PSScriptRoot -Parent

function Passo($texto) { Write-Host "`n==> $texto" -ForegroundColor Cyan }

function Aleatorio([int]$tamanho) {
    $letras = [char[]]"ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
    $bytes = New-Object byte[] $tamanho
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return -join ($bytes | ForEach-Object { $letras[$_ % $letras.Length] })
}

Passo "Conferindo o Azure CLI"
if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
    throw "Azure CLI nao encontrado. Instale com: winget install Microsoft.AzureCLI"
}
$conta = az account show -o json 2>$null | ConvertFrom-Json
if (-not $conta) { throw "Faca login antes: az login" }
Write-Host "Assinatura: $($conta.name) | usuario: $($conta.user.name)"

if (-not $SoAplicacao) {
    Passo "Registrando os provedores de recursos (so demora na primeira vez)"
    foreach ($p in "Microsoft.Web", "Microsoft.DBforMySQL", "Microsoft.Insights", "Microsoft.OperationalInsights") {
        az provider register --namespace $p --wait --output none
    }

    Passo "Escolhendo a regiao"
    if (-not $Regiao) {
        # a assinatura Azure for Students so libera algumas regioes (politica "Allowed locations")
        $permitidas = @()
        $listas = az policy assignment list --disable-scope-strict-match `
            --query "[].parameters.listOfAllowedLocations.value" -o json 2>$null | ConvertFrom-Json
        foreach ($l in $listas) { if ($l) { $permitidas += $l } }
        $preferidas = "brazilsouth", "eastus", "eastus2", "centralus", "southcentralus", "westus2", "canadacentral"
        if ($permitidas.Count -gt 0) {
            Write-Host "Regioes liberadas na assinatura: $(($permitidas | Select-Object -Unique) -join ', ')"
            $Regiao = $preferidas | Where-Object { $permitidas -contains $_ } | Select-Object -First 1
            if (-not $Regiao) { $Regiao = $permitidas[0] }
        } else {
            $Regiao = "brazilsouth"
        }
    }
    Write-Host "Regiao escolhida: $Regiao"

    Passo "Gerando os segredos (ficam so neste computador, em infra\.segredos.local.json)"
    $arquivoSegredos = Join-Path $PSScriptRoot ".segredos.local.json"
    if (Test-Path $arquivoSegredos) {
        $segredos = Get-Content $arquivoSegredos -Raw | ConvertFrom-Json
        Write-Host "Usando os segredos ja gerados antes"
    } else {
        $segredos = [pscustomobject]@{
            mysqlSenha = "Jc" + (Aleatorio 20) + "!7"
            jwtSecret  = Aleatorio 48
        }
        $segredos | ConvertTo-Json | Set-Content $arquivoSegredos -Encoding utf8
    }

    # parametros vao por arquivo pra senha com simbolo nao quebrar na linha de comando
    $arquivoParametros = Join-Path $env:TEMP "jcondo-parametros.json"
    @{
        '$schema'      = "https://schema.management.azure.com/schemas/2019-04-01/deploymentParameters.json#"
        contentVersion = "1.0.0.0"
        parameters     = @{
            location        = @{ value = $Regiao }
            planoAppService = @{ value = $Plano }
            mysqlSenha      = @{ value = $segredos.mysqlSenha }
            jwtSecret       = @{ value = $segredos.jwtSecret }
        }
    } | ConvertTo-Json -Depth 5 | ForEach-Object {
        # utf-8 sem BOM (o az nao gosta do BOM do PowerShell 5)
        [IO.File]::WriteAllText($arquivoParametros, $_, (New-Object Text.UTF8Encoding $false))
    }

    Passo "Criando o grupo de recursos $GrupoRecursos"
    az group create --name $GrupoRecursos --location $Regiao --tags projeto=JCondo --output none

    Passo "Criando App Service, MySQL e monitoramento (leva de 5 a 15 minutos)"
    try {
        $saida = az deployment group create `
            --resource-group $GrupoRecursos `
            --name "jcondo-$(Get-Date -Format 'yyyyMMdd-HHmm')" `
            --template-file (Join-Path $PSScriptRoot "main.bicep") `
            --parameters "@$arquivoParametros" `
            --query properties.outputs -o json | ConvertFrom-Json
    } finally {
        Remove-Item $arquivoParametros -ErrorAction SilentlyContinue
    }
    if (-not $saida) { throw "O deploy da infraestrutura falhou. Veja a mensagem acima (se for regiao, rode de novo com -Regiao)." }
    $nomeApp = $saida.nomeApp.value
} else {
    $nomeApp = az webapp list --resource-group $GrupoRecursos --query "[0].name" -o tsv
}

$url = "https://" + (az webapp show -g $GrupoRecursos -n $nomeApp --query defaultHostName -o tsv)

Passo "Gerando o .jar (testes rodam no GitHub Actions)"
Push-Location (Join-Path $raiz "app")
try {
    & .\mvnw.cmd -q -B package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Falha ao gerar o .jar" }
} finally {
    Pop-Location
}

Passo "Publicando o jcondo.jar no App Service $nomeApp"
az webapp deploy --resource-group $GrupoRecursos --name $nomeApp `
    --src-path (Join-Path $raiz "app\target\jcondo.jar") --type jar --output none

Passo "Esperando a aplicacao subir (health check)"
$ok = $false
for ($i = 1; $i -le 40; $i++) {
    try {
        $saude = Invoke-RestMethod "$url/actuator/health" -TimeoutSec 30
        if ($saude.status -eq "UP") { $ok = $true; break }
    } catch { }
    Write-Host "  tentativa $i ainda subindo..."
    Start-Sleep -Seconds 15
}

Passo "Salvando o perfil de publicacao para o GitHub Actions"
$perfil = Join-Path $PSScriptRoot ".perfil-publicacao.local.xml"
az webapp deployment list-publishing-profiles -g $GrupoRecursos -n $nomeApp --xml | Set-Content $perfil -Encoding utf8

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
if ($ok) { Write-Host " JCondo no ar!" -ForegroundColor Green } else { Write-Host " Publicado, mas o health check ainda nao respondeu. Veja o Log stream no portal." -ForegroundColor Yellow }
Write-Host " Painel do sindico : $url/login"
Write-Host " App do morador    : $url/app/"
Write-Host " API (Swagger)     : $url/swagger-ui/index.html"
Write-Host " Health check      : $url/actuator/health"
Write-Host ""
Write-Host " GitHub > Settings > Secrets and variables > Actions:"
Write-Host "   variable AZURE_WEBAPP_NAME            = $nomeApp"
Write-Host "   secret   AZURE_WEBAPP_PUBLISH_PROFILE = conteudo de infra\.perfil-publicacao.local.xml"
Write-Host "============================================================" -ForegroundColor Green
