<#
  parar.ps1 - desliga o tunel, a aplicacao e o MySQL (nessa ordem)
  Uso: .\infra\parar.ps1            (-ManterBanco deixa o MySQL ligado)
#>
param(
    [string]$Pasta = (Join-Path $env:USERPROFILE "JCondoServidor"),
    [switch]$ManterBanco
)

. "$PSScriptRoot\comum.ps1"
$c = Caminhos $Pasta
$pids = LerPids $c

Passo "Fechando o tunel"
if (Ativo $pids.tunel) { Stop-Process -Id $pids.tunel -Force }
$pids.tunel = $null
Remove-Item $c.Url -ErrorAction SilentlyContinue

Passo "Parando a aplicacao"
if (Ativo $pids.app) { Stop-Process -Id $pids.app -Force }
PararPorta ([int](LerConfig $c).porta)
$pids.app = $null

if (-not $ManterBanco) {
    Passo "Parando o MySQL"
    if (PortaAberta 3306) {
        $config = LerConfig $c
        # desligamento limpo (grava tudo no disco antes de sair)
        ExecutarSql $c "root" $config.mysqlRootSenha "SHUTDOWN;"
        for ($i = 0; $i -lt 30 -and (PortaAberta 3306); $i++) { Start-Sleep -Seconds 1 }
    }
    $pids.mysql = $null
}

GravarPids $c $pids
Write-Host "`nJCondo desligado." -ForegroundColor Green
