<#
  remover.ps1 - apaga todos os recursos do JCondo no Azure (para de consumir o credito)
  Uso: .\infra\remover.ps1
#>
param([string]$GrupoRecursos = "rg-jcondo")

$resposta = Read-Host "Isso apaga o App Service, o MySQL (com os dados) e os logs do grupo $GrupoRecursos. Digite APAGAR para confirmar"
if ($resposta -ne "APAGAR") {
    Write-Host "Cancelado."
    exit 0
}
az group delete --name $GrupoRecursos --yes --no-wait
Write-Host "Remocao iniciada. Acompanhe no portal (Grupos de recursos)."
