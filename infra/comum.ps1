# funcoes e caminhos usados pelos scripts do servidor self-hosted do JCondo
# (carregado pelos outros scripts com: . "$PSScriptRoot\comum.ps1")

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$Versoes = @{
    mysql       = "8.4.3"
    mysqlUrl    = "https://cdn.mysql.com/archives/mysql-8.4/mysql-8.4.3-winx64.zip"
    cloudflared = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe"
    jarRelease  = "https://github.com/jorgensc/PEIV-Entrega2/releases/latest/download/jcondo.jar"
}

function Caminhos([string]$pasta) {
    return [pscustomobject]@{
        Raiz        = $pasta
        Downloads   = Join-Path $pasta "downloads"
        Mysql       = Join-Path $pasta "mysql"
        MysqlBin    = Join-Path $pasta "mysql\bin"
        Dados       = Join-Path $pasta "dados\mysql"
        MyIni       = Join-Path $pasta "mysql\my.ini"
        App         = Join-Path $pasta "app"
        Jar         = Join-Path $pasta "app\jcondo.jar"
        Logs        = Join-Path $pasta "logs"
        Cloudflared = Join-Path $pasta "cloudflared.exe"
        Config      = Join-Path $pasta "config.json"
        Url         = Join-Path $pasta "url-publica.txt"
        Pids        = Join-Path $pasta "pids.json"
    }
}

function Passo($texto) { Write-Host "`n==> $texto" -ForegroundColor Cyan }

function Aleatorio([int]$tamanho) {
    $letras = [char[]]"ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
    $bytes = New-Object byte[] $tamanho
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return -join ($bytes | ForEach-Object { $letras[$_ % $letras.Length] })
}

function PortaAberta([int]$porta) {
    $c = New-Object Net.Sockets.TcpClient
    try {
        $c.Connect("127.0.0.1", $porta)
        return $true
    } catch {
        return $false
    } finally {
        $c.Dispose()
    }
}

function EsperarPorta([int]$porta, [int]$segundos, [string]$nome) {
    for ($i = 0; $i -lt $segundos; $i++) {
        if (PortaAberta $porta) { return }
        Start-Sleep -Seconds 1
    }
    throw "$nome nao respondeu na porta $porta em $segundos segundos"
}

function GravarUtf8([string]$arquivo, [string]$texto) {
    # utf-8 sem BOM (o MySQL e o Java leem melhor)
    [IO.File]::WriteAllText($arquivo, $texto, (New-Object Text.UTF8Encoding $false))
}

function LerConfig($c) {
    if (-not (Test-Path $c.Config)) { throw "Servidor nao instalado. Rode antes: .\infra\instalar.ps1" }
    return Get-Content $c.Config -Raw | ConvertFrom-Json
}

# roda SQL no MySQL local sem deixar a senha na linha de comando
function ExecutarSql($c, [string]$usuario, [string]$senha, [string]$sql) {
    $cnf = Join-Path $env:TEMP "jcondo-cliente.cnf"
    $linhas = "[client]`nuser=$usuario`nhost=127.0.0.1`nport=3306`n"
    if ($senha) { $linhas += "password=$senha`n" }
    GravarUtf8 $cnf $linhas
    $antes = $ErrorActionPreference
    $ErrorActionPreference = "Continue" # avisos do mysql vao pro stderr
    try {
        $sql | & (Join-Path $c.MysqlBin "mysql.exe") "--defaults-extra-file=$cnf" --batch 2>&1 | ForEach-Object { "  $_" }
        if ($LASTEXITCODE -ne 0) { throw "Falha ao executar SQL no MySQL" }
    } finally {
        $ErrorActionPreference = $antes
        Remove-Item $cnf -ErrorAction SilentlyContinue
    }
}

function LerPids($c) {
    if (Test-Path $c.Pids) { return Get-Content $c.Pids -Raw | ConvertFrom-Json }
    return [pscustomobject]@{ mysql = $null; app = $null; tunel = $null }
}

function GravarPids($c, $pids) {
    GravarUtf8 $c.Pids ($pids | ConvertTo-Json)
}

# caminho do java.exe de verdade (o do PATH costuma ser um lancador que abre outro processo)
function JavaReal {
    $props = cmd /c "java -XshowSettings:properties -version 2>&1"
    $linha = $props | Where-Object { $_ -match '^\s*java\.home\s*=' } | Select-Object -First 1
    if (-not $linha) { throw "Java nao encontrado no PATH" }
    $javaHome = ($linha -split '=', 2)[1].Trim()
    return Join-Path $javaHome "bin\java.exe"
}

# processo que esta ouvindo numa porta local
function PidDaPorta([int]$porta) {
    $con = Get-NetTCPConnection -LocalPort $porta -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($con) { return $con.OwningProcess }
    return $null
}

function PararPorta([int]$porta) {
    $processoId = PidDaPorta $porta
    if ($processoId) {
        Stop-Process -Id $processoId -Force
        for ($i = 0; $i -lt 20 -and (PortaAberta $porta); $i++) { Start-Sleep -Milliseconds 500 }
    }
}

function Ativo($processoId) {
    if (-not $processoId) { return $false }
    return [bool](Get-Process -Id $processoId -ErrorAction SilentlyContinue)
}
