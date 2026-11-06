<#
  backup.ps1 - copia de seguranca do banco jcondo (mysqldump)
  Guarda em <pasta>\backups e mantem os 7 mais recentes.

  Uso:
    .\infra\backup.cmd
  Para restaurar:
    mysql\bin\mysql.exe -u root -p jcondo < backups\jcondo-AAAAMMDD-HHMM.sql
#>
param(
    [string]$Pasta = (Join-Path $env:USERPROFILE "JCondoServidor"),
    [int]$Manter = 7
)

. "$PSScriptRoot\comum.ps1"
$c = Caminhos $Pasta
$config = LerConfig $c
if (-not (PortaAberta 3306)) { throw "O MySQL esta desligado. Rode .\infra\iniciar.cmd antes." }

$destino = Join-Path $c.Raiz "backups"
New-Item -ItemType Directory -Force $destino | Out-Null
$arquivo = Join-Path $destino ("jcondo-" + (Get-Date -Format "yyyyMMdd-HHmm") + ".sql")

Passo "Gerando $arquivo"
$cnf = Join-Path $env:TEMP "jcondo-backup.cnf"
GravarUtf8 $cnf "[client]`nuser=root`npassword=$($config.mysqlRootSenha)`nhost=127.0.0.1`nport=3306`n"
try {
    $dump = Join-Path $c.MysqlBin "mysqldump.exe"
    cmd /c "`"$dump`" --defaults-extra-file=`"$cnf`" --single-transaction --routines --default-character-set=utf8mb4 --result-file=`"$arquivo`" jcondo 2>&1"
    if ($LASTEXITCODE -ne 0) { throw "Falha no mysqldump" }
} finally {
    Remove-Item $cnf -ErrorAction SilentlyContinue
}

Get-ChildItem $destino -Filter "jcondo-*.sql" | Sort-Object Name -Descending | Select-Object -Skip $Manter | Remove-Item
$tamanho = [math]::Round((Get-Item $arquivo).Length / 1KB, 1)
Write-Host "Backup pronto: $arquivo ($tamanho KB)" -ForegroundColor Green
