<#
  instalar.ps1 - prepara o servidor self-hosted do JCondo (roda uma vez so)

  O que faz:
    1. baixa o MySQL 8.4 LTS (zip, sem instalador) e o cloudflared
    2. configura o MySQL pra aceitar conexao so de 127.0.0.1 (nada fica exposto na rede)
    3. cria o banco jcondo e o usuario jcondo com senha aleatoria
    4. gera o segredo do JWT e guarda tudo em <pasta>\config.json (fora do Git)
    5. copia o jcondo.jar (gera com o Maven se ainda nao existir)

  Uso (na raiz do repositorio):
    .\infra\instalar.ps1
    .\infra\instalar.ps1 -Pasta D:\JCondoServidor
  Depois: .\infra\iniciar.ps1
#>
param(
    [string]$Pasta = (Join-Path $env:USERPROFILE "JCondoServidor")
)

. "$PSScriptRoot\comum.ps1"
$c = Caminhos $Pasta
$repo = Split-Path $PSScriptRoot -Parent

Passo "Conferindo o Java 21"
$versaoJava = (cmd /c "java -version 2>&1" | Select-Object -First 1) -join ""
if ($versaoJava -notmatch '"(2[1-9]|[3-9]\d)') { throw "Precisa do Java 21 ou mais novo. Encontrado: $versaoJava" }
Write-Host $versaoJava

Passo "Criando as pastas em $Pasta"
foreach ($p in $c.Raiz, $c.Downloads, $c.App, $c.Logs, (Split-Path $c.Dados -Parent)) {
    New-Item -ItemType Directory -Force $p | Out-Null
}

Passo "Baixando o cloudflared (tunel da Cloudflare)"
if (-not (Test-Path $c.Cloudflared)) {
    $baixado = Join-Path $c.Downloads "cloudflared.exe"
    if (-not (Test-Path $baixado)) { Invoke-WebRequest $Versoes.cloudflared -OutFile $baixado -UseBasicParsing }
    Copy-Item $baixado $c.Cloudflared
}
& $c.Cloudflared --version

Passo "Baixando e extraindo o MySQL $($Versoes.mysql)"
if (-not (Test-Path (Join-Path $c.MysqlBin "mysqld.exe"))) {
    $zip = Join-Path $c.Downloads "mysql-$($Versoes.mysql)-winx64.zip"
    if (-not (Test-Path $zip)) {
        Write-Host "  baixando cerca de 250 MB, pode demorar..."
        Invoke-WebRequest $Versoes.mysqlUrl -OutFile $zip -UseBasicParsing
    }
    $temp = Join-Path $c.Raiz "mysql-extraindo"
    Remove-Item $temp -Recurse -Force -ErrorAction SilentlyContinue
    Expand-Archive $zip -DestinationPath $temp
    Move-Item (Get-ChildItem $temp -Directory | Select-Object -First 1).FullName $c.Mysql
    Remove-Item $temp -Recurse -Force
}

Passo "Configurando o MySQL (so acesso local)"
$base = $c.Mysql -replace '\\', '/'
$dados = $c.Dados -replace '\\', '/'
GravarUtf8 $c.MyIni @"
[mysqld]
basedir=$base
datadir=$dados
port=3306
# so aceita conexao do proprio computador; quem vem de fora entra pelo tunel, na aplicacao
bind-address=127.0.0.1
mysqlx=OFF
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
default-time-zone='-03:00'
max_connections=50
innodb_buffer_pool_size=256M
log-error=$(($c.Logs -replace '\\', '/'))/mysql-erros.log

[client]
port=3306
default-character-set=utf8mb4
"@

$novo = -not (Test-Path (Join-Path $c.Dados "mysql"))
if ($novo) {
    Write-Host "  inicializando a pasta de dados"
    cmd /c "`"$(Join-Path $c.MysqlBin 'mysqld.exe')`" --defaults-file=`"$($c.MyIni)`" --initialize-insecure 2>&1" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Falha ao inicializar o MySQL. Veja $($c.Logs)\mysql-erros.log" }
}

Passo "Gerando as senhas e o segredo do JWT"
if (Test-Path $c.Config) {
    $config = Get-Content $c.Config -Raw | ConvertFrom-Json
    Write-Host "  usando o config.json que ja existe"
} else {
    $config = [pscustomobject]@{
        mysqlRootSenha = Aleatorio 24
        dbUsuario      = "jcondo"
        dbSenha        = Aleatorio 24
        jwtSecret      = Aleatorio 48
        porta          = 8080
    }
}

Passo "Subindo o MySQL e criando o banco jcondo"
$pids = LerPids $c
if (-not (PortaAberta 3306)) {
    $p = Start-Process (Join-Path $c.MysqlBin "mysqld.exe") -ArgumentList "--defaults-file=`"$($c.MyIni)`"" -WindowStyle Hidden -PassThru
    $pids.mysql = $p.Id
    GravarPids $c $pids
    EsperarPorta 3306 60 "MySQL"
}
$senhaRootAtual = if ($novo) { "" } else { $config.mysqlRootSenha }
ExecutarSql $c "root" $senhaRootAtual @"
ALTER USER 'root'@'localhost' IDENTIFIED BY '$($config.mysqlRootSenha)';
CREATE DATABASE IF NOT EXISTS jcondo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$($config.dbUsuario)'@'localhost' IDENTIFIED BY '$($config.dbSenha)';
CREATE USER IF NOT EXISTS '$($config.dbUsuario)'@'127.0.0.1' IDENTIFIED BY '$($config.dbSenha)';
GRANT ALL PRIVILEGES ON jcondo.* TO '$($config.dbUsuario)'@'localhost';
GRANT ALL PRIVILEGES ON jcondo.* TO '$($config.dbUsuario)'@'127.0.0.1';
FLUSH PRIVILEGES;
"@
GravarUtf8 $c.Config ($config | ConvertTo-Json)
Write-Host "  segredos guardados em $($c.Config)"

Passo "Copiando a aplicacao"
$jarRepo = Join-Path $repo "app\target\jcondo.jar"
if (-not (Test-Path $jarRepo)) {
    Push-Location (Join-Path $repo "app")
    try {
        & .\mvnw.cmd -q -B package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw "Falha ao gerar o .jar" }
    } finally {
        Pop-Location
    }
}
Copy-Item $jarRepo $c.Jar -Force

Write-Host "`nServidor instalado em $Pasta" -ForegroundColor Green
Write-Host "Para colocar no ar: .\infra\iniciar.ps1"
