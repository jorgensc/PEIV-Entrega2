// Infraestrutura do JCondo no Microsoft Azure (Azure for Students)
// cria: plano do App Service (Linux), Web App Java 21, MySQL Flexible Server,
//       banco jcondo, regra de firewall, Log Analytics e Application Insights
//
// uso: infra/deploy.ps1 (ou az deployment group create -g rg-jcondo -f infra/main.bicep -p ...)

@description('Região dos recursos (precisa estar liberada na política da assinatura de estudante)')
param location string = resourceGroup().location

@description('Prefixo dos nomes. O App Service vira <prefixo>-<sufixo>.azurewebsites.net')
@minLength(3)
@maxLength(12)
param prefixo string = 'jcondo'

@description('Plano do App Service: F1 (gratuito, sem Always On) ou B1 (básico, recomendado na apresentação)')
@allowed([
  'F1'
  'B1'
])
param planoAppService string = 'B1'

@description('Usuário administrador do MySQL')
param mysqlAdmin string = 'jcondoadmin'

@description('Senha do administrador do MySQL')
@secure()
param mysqlSenha string

@description('Segredo usado para assinar os tokens JWT da API')
@secure()
param jwtSecret string

@description('IP público do desenvolvedor liberado no firewall do MySQL (opcional, para importar dados)')
param ipDesenvolvedor string = ''

@description('Carrega síndico, moradores e reservas de exemplo quando o banco está vazio')
param dadosDemonstracao bool = true

var sufixo = take(uniqueString(resourceGroup().id), 6)
var nomeApp = '${prefixo}-${sufixo}'
var nomeMysql = '${prefixo}-mysql-${sufixo}'
var gratuito = planoAppService == 'F1'
var tags = {
  projeto: 'JCondo'
  disciplina: 'Praticas Extensionistas IV'
  ambiente: 'producao'
}

// ---------- monitoramento ----------
resource logs 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: '${prefixo}-logs-${sufixo}'
  location: location
  tags: tags
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
  }
}

resource insights 'Microsoft.Insights/components@2020-02-02' = {
  name: '${prefixo}-insights-${sufixo}'
  location: location
  kind: 'web'
  tags: tags
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logs.id
  }
}

// ---------- banco de dados ----------
resource mysql 'Microsoft.DBforMySQL/flexibleServers@2023-06-30' = {
  name: nomeMysql
  location: location
  tags: tags
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  properties: {
    version: '8.0.21'
    administratorLogin: mysqlAdmin
    administratorLoginPassword: mysqlSenha
    storage: {
      storageSizeGB: 20
      autoGrow: 'Enabled'
    }
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: {
      mode: 'Disabled'
    }
    network: {
      publicNetworkAccess: 'Enabled'
    }
  }
}

resource banco 'Microsoft.DBforMySQL/flexibleServers/databases@2023-06-30' = {
  parent: mysql
  name: 'jcondo'
  properties: {
    charset: 'utf8mb4'
    collation: 'utf8mb4_unicode_ci'
  }
}

// 0.0.0.0 = libera só os serviços do próprio Azure (o App Service), não a internet toda
resource firewallAzure 'Microsoft.DBforMySQL/flexibleServers/firewallRules@2023-06-30' = {
  parent: mysql
  name: 'PermitirServicosAzure'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

resource firewallDev 'Microsoft.DBforMySQL/flexibleServers/firewallRules@2023-06-30' = if (!empty(ipDesenvolvedor)) {
  parent: mysql
  name: 'Desenvolvedor'
  properties: {
    startIpAddress: ipDesenvolvedor
    endIpAddress: ipDesenvolvedor
  }
}

// ---------- aplicacao ----------
resource plano 'Microsoft.Web/serverfarms@2023-12-01' = {
  name: '${prefixo}-plano-${sufixo}'
  location: location
  tags: tags
  kind: 'linux'
  sku: {
    name: planoAppService
    tier: gratuito ? 'Free' : 'Basic'
  }
  properties: {
    reserved: true // obrigatorio para Linux
  }
}

resource app 'Microsoft.Web/sites@2023-12-01' = {
  name: nomeApp
  location: location
  tags: tags
  properties: {
    serverFarmId: plano.id
    httpsOnly: true
    clientAffinityEnabled: false
    siteConfig: {
      linuxFxVersion: 'JAVA|21-java21'
      alwaysOn: !gratuito
      http20Enabled: true
      minTlsVersion: '1.2'
      ftpsState: 'Disabled'
      healthCheckPath: '/actuator/health'
      appCommandLine: ''
      appSettings: [
        {
          name: 'DB_URL'
          value: 'jdbc:mysql://${mysql.properties.fullyQualifiedDomainName}:3306/jcondo?sslMode=REQUIRED&serverTimezone=America/Sao_Paulo'
        }
        {
          name: 'DB_USERNAME'
          value: mysqlAdmin
        }
        {
          name: 'DB_PASSWORD'
          value: mysqlSenha
        }
        {
          name: 'JWT_SECRET'
          value: jwtSecret
        }
        {
          name: 'SEED_DEMO'
          value: string(dadosDemonstracao)
        }
        {
          name: 'SHOW_SQL'
          value: 'false'
        }
        {
          name: 'TZ'
          value: 'America/Sao_Paulo'
        }
        {
          name: 'JAVA_OPTS'
          value: '-Xms256m -Xmx512m -XX:+UseSerialGC'
        }
        {
          name: 'WEBSITES_CONTAINER_START_TIME_LIMIT'
          value: '600'
        }
        {
          name: 'APPLICATIONINSIGHTS_CONNECTION_STRING'
          value: insights.properties.ConnectionString
        }
        {
          name: 'ApplicationInsightsAgent_EXTENSION_VERSION'
          value: '~3'
        }
      ]
    }
  }
  dependsOn: [
    banco
    firewallAzure
  ]
}

// o deploy do GitHub Actions usa o perfil de publicacao (precisa da autenticacao basica do SCM)
resource scm 'Microsoft.Web/sites/basicPublishingCredentialsPolicies@2023-12-01' = {
  parent: app
  name: 'scm'
  properties: {
    allow: true
  }
}

// logs da aplicacao no Log stream do portal
resource logsApp 'Microsoft.Web/sites/config@2023-12-01' = {
  parent: app
  name: 'logs'
  properties: {
    applicationLogs: {
      fileSystem: {
        level: 'Information'
      }
    }
    httpLogs: {
      fileSystem: {
        enabled: true
        retentionInDays: 3
        retentionInMb: 35
      }
    }
  }
}

output nomeApp string = app.name
output url string = 'https://${app.properties.defaultHostName}'
output appPwa string = 'https://${app.properties.defaultHostName}/app/'
output swagger string = 'https://${app.properties.defaultHostName}/swagger-ui/index.html'
output mysqlHost string = mysql.properties.fullyQualifiedDomainName
