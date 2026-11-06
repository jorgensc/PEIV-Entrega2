# Configuração da infraestrutura em nuvem (Microsoft Azure)

Este documento descreve como a infraestrutura do JCondo foi configurada no Microsoft Azure para a Entrega 2 de Práticas Extensionistas IV. A escolha do Azure e a comparação com as outras opções estão na Entrega 1 (`doc/PE-IV-Jcondo.pdf`, seção 6). Aqui o foco é a configuração: quais recursos existem, como cada um foi ajustado, como eles conversam entre si, como a aplicação chega até eles e como tudo é monitorado.

Toda a infraestrutura está escrita como código no arquivo [`infra/main.bicep`](../infra/main.bicep). O script [`infra/deploy.ps1`](../infra/deploy.ps1) cria os recursos e publica a aplicação com um único comando, então o ambiente pode ser recriado do zero sempre que for preciso.

## 1. Visão geral

A solução roda inteira em serviços gerenciados (PaaS), dentro de um grupo de recursos chamado `rg-jcondo`. Um único App Service entrega as três partes do sistema, todas no mesmo endereço HTTPS:

| Caminho | O que é | Quem usa |
|---|---|---|
| `/login`, `/painel`, `/moradores`... | Painel web responsivo (Thymeleaf + Bootstrap) | Síndico |
| `/app/` | App mobile PWA (instalável, funciona offline) | Moradores (e síndico) |
| `/api/v1/...` | API REST com autenticação JWT | O PWA e integrações |
| `/swagger-ui/index.html` | Documentação interativa da API | Desenvolvedores e professor |
| `/actuator/health` | Health check | Azure e pipeline |

Manter web, PWA e API no mesmo App Service foi uma decisão consciente: o PWA e a API ficam na mesma origem, o que dispensa configuração de CORS para o app, o certificado HTTPS é um só e o custo continua sendo de um único plano.

```
                    Internet (HTTPS 443, TLS 1.2+)
                               |
              +----------------v-----------------+
              |  Azure App Service (Linux, B1)   |
              |  Java SE 21 + Spring Boot 3.5    |
              |  jcondo.jar                      |
              |   - painel web  - PWA  - API     |
              +---+--------------------------+---+
                  | JDBC 3306 (TLS)          | telemetria
   +--------------v--------------+   +-------v-----------------------+
   | Azure Database for MySQL    |   | Application Insights          |
   | Flexible Server (B1ms)      |   | + Log Analytics (30 dias)     |
   | MySQL 8.0 - banco jcondo    |   +-------------------------------+
   +-----------------------------+
```

## 2. Recursos criados

| Recurso | Nome (exemplo) | Configuração |
|---|---|---|
| Grupo de recursos | `rg-jcondo` | Agrupa tudo, com a tag `projeto=JCondo`. Apagar o grupo remove o ambiente inteiro. |
| Plano do App Service | `jcondo-plano-xxxxxx` | Linux, SKU **F1** (gratuito: 1 GB de memória, 60 minutos de CPU por dia). Pode subir para B1 no parâmetro `planoAppService`, sem mudar mais nada. |
| App Service (Web App) | `jcondo-xxxxxx` | Pilha `JAVA|21-java21` (Java SE, servidor embutido do Spring Boot). |
| MySQL Flexible Server | `jcondo-mysql-xxxxxx` | Burstable **Standard_B1ms**, MySQL 8.0.21, 20 GB com crescimento automático, backup de 7 dias. |
| Banco de dados | `jcondo` | `utf8mb4` / `utf8mb4_unicode_ci` (acentos e emojis). |
| Regra de firewall | `PermitirServicosAzure` | Faixa `0.0.0.0` a `0.0.0.0`, que no Azure significa "somente serviços do próprio Azure". |
| Log Analytics | `jcondo-logs-xxxxxx` | Retenção de 30 dias. |
| Application Insights | `jcondo-insights-xxxxxx` | Ligado ao Log Analytics, recebe requisições, erros e tempo de resposta. |

O sufixo `xxxxxx` é gerado a partir do id do grupo de recursos (`uniqueString`), porque o nome do App Service precisa ser único no mundo todo (ele vira o domínio `jcondo-xxxxxx.azurewebsites.net`).

## 3. Configuração do App Service

| Configuração | Valor | Por quê |
|---|---|---|
| Somente HTTPS | ligado | Todo acesso HTTP é redirecionado para HTTPS. O PWA exige HTTPS para registrar o service worker. |
| Versão mínima do TLS | 1.2 | Bloqueia protocolos antigos e inseguros. |
| HTTP/2 | ligado | Carrega os arquivos do painel e do app em paralelo na mesma conexão. |
| FTP/FTPS | desabilitado | O deploy é feito só pelo pipeline e pelo Azure CLI. |
| Always On | desligado no F1, ligado no B1 | No F1 a aplicação "dorme" depois de uns 20 minutos sem acesso e o primeiro acesso seguinte demora mais. No B1 ela fica sempre ligada. |
| Health check | `/actuator/health` (B1) | No B1 o Azure testa esse endereço e troca a instância se ela parar de responder. No F1, com uma instância só, o endereço é usado pelo pipeline e pelo script de deploy para confirmar que a aplicação subiu. |
| Afinidade de sessão (ARR) | desligada | A API é stateless (JWT), não precisa prender o usuário numa instância. |
| Credenciais SCM básicas | liberadas | Necessárias para o perfil de publicação usado pelo GitHub Actions. |
| Logs | aplicação (Information) e HTTP (3 dias) | Ficam disponíveis no Log stream do portal. |

### Variáveis de ambiente (Application settings)

O código não guarda nenhuma senha. Tudo que muda entre o computador do desenvolvedor e a nuvem vem de variáveis de ambiente, lidas no `application.properties` com um valor padrão para rodar localmente.

| Variável | Valor na nuvem | Uso |
|---|---|---|
| `DB_URL` | `jdbc:mysql://<servidor>.mysql.database.azure.com:3306/jcondo?sslMode=REQUIRED&serverTimezone=America/Sao_Paulo` | Conexão com o banco, com TLS obrigatório. |
| `DB_USERNAME` | `jcondoadmin` | Usuário do MySQL. |
| `DB_PASSWORD` | gerada pelo script (segredo) | Senha do MySQL. |
| `JWT_SECRET` | gerado pelo script (48 caracteres aleatórios) | Assina os tokens da API. |
| `SEED_DEMO` | `true` | Carrega os dados de demonstração quando o banco está vazio. |
| `SHOW_SQL` | `false` | Não grava as consultas SQL no log de produção. |
| `TZ` | `America/Sao_Paulo` | O servidor do Azure roda em UTC; as reservas usam o horário de Brasília. |
| `JAVA_OPTS` | `-Xms256m -Xmx512m -XX:+UseSerialGC` | Limita a memória da JVM ao tamanho do plano. |
| `WEBSITES_CONTAINER_START_TIME_LIMIT` | `600` | Dá até 10 minutos para o Spring Boot subir (importante no plano F1). |
| `APPLICATIONINSIGHTS_CONNECTION_STRING` | gerada pelo Bicep | Liga o agente Java do Application Insights. |
| `ApplicationInsightsAgent_EXTENSION_VERSION` | `~3` | Ativa a instrumentação automática, sem mudar o código. |

A porta não precisa ser configurada: o App Service define `SERVER_PORT=80` e o Spring Boot já lê essa variável. A propriedade `server.forward-headers-strategy=native` faz a aplicação entender que, por trás do proxy do Azure, a requisição original era HTTPS, e assim os redirecionamentos do login não voltam para HTTP.

## 4. Configuração do banco de dados

O Azure Database for MySQL Flexible Server foi escolhido por ser o mesmo MySQL usado no XAMPP durante o desenvolvimento, então o código não muda, só a variável `DB_URL`.

| Configuração | Valor |
|---|---|
| Camada / tamanho | Burstable, Standard_B1ms (1 vCore, 2 GB) |
| Versão | MySQL 8.0.21 |
| Armazenamento | 20 GB, crescimento automático ligado |
| Backup | automático, retenção de 7 dias, restauração para qualquer ponto nesse período |
| Alta disponibilidade | desligada (não cabe no orçamento de estudante) |
| Rede | acesso público com firewall: só serviços do Azure, mais o IP do desenvolvedor se for informado |
| Transporte seguro | `require_secure_transport=ON` (padrão), toda conexão usa TLS |

As tabelas são criadas pela própria aplicação na primeira subida (JPA com `ddl-auto=update`). Para quem preferir criar à mão, o script `app/database/jcondo.sql` tem o mesmo esquema. O pool de conexões foi reduzido para 5 (`spring.datasource.hikari.maximum-pool-size`), porque a camada B1ms aceita poucas conexões simultâneas.

## 5. Segurança

A segurança foi pensada em camadas:

1. **Transporte:** HTTPS obrigatório com TLS 1.2 ou superior até o App Service, e TLS também entre a aplicação e o MySQL.
2. **Rede:** o MySQL não aceita conexões da internet, só de serviços do Azure (e, opcionalmente, do IP do desenvolvedor).
3. **Segredos:** senhas e a chave do JWT ficam nas Application settings do Azure e nos Secrets do GitHub, nunca no repositório. O `.gitignore` bloqueia os arquivos locais gerados pelo script (`infra/*.local.*`).
4. **Autenticação:** o painel web usa login com sessão e proteção CSRF; a API usa token JWT (HS256) com validade de 12 horas. As senhas dos usuários são guardadas com hash BCrypt.
5. **Autorização:** dois perfis, SÍNDICO e MORADOR. A API confere o perfil em cada operação (por exemplo, morador não publica aviso nem vê reserva de outro morador) e responde 401/403 no padrão Problem Details.

## 6. Publicação (CI/CD)

O pipeline `.github/workflows/ci-cd.yml` roda no GitHub Actions:

1. **A cada push ou pull request:** instala o JDK 21, roda `./mvnw verify` (30 testes, entre unitários e de integração da API, usando banco H2 em memória) e guarda o `jcondo.jar` e o relatório dos testes.
2. **Em push na main:** baixa o `.jar`, publica no App Service com a action `azure/webapps-deploy` usando o perfil de publicação e, no fim, faz um *smoke test* chamando `/actuator/health` até receber `UP`. Se a aplicação não subir, o pipeline falha.

O job de deploy só roda quando a variable `AZURE_WEBAPP_NAME` existe no repositório, então o CI funciona mesmo antes de a nuvem estar configurada.

## 7. Monitoramento e operação

| Ferramenta | Onde ver | O que mostra |
|---|---|---|
| Health check | App Service > Monitoramento > Verificação de integridade | Se a instância está respondendo; o Azure reinicia quando falha. |
| Log stream | App Service > Monitoramento > Fluxo de log | Log do Spring Boot em tempo real. |
| Application Insights | Recurso `jcondo-insights-*` | Requisições por endpoint, tempo de resposta, falhas, dependência do MySQL e *Live Metrics*. |
| Métricas do MySQL | MySQL > Monitoramento | CPU, conexões, armazenamento. |
| Backup | MySQL > Backup e restauração | Pontos de restauração dos últimos 7 dias. |

## 8. Custos e escalabilidade

Valores aproximados de referência, que devem ser conferidos na calculadora de preços do Azure para a região escolhida:

| Recurso | Custo estimado |
|---|---|
| App Service F1 Linux (escolhido) | gratuito |
| MySQL Flexible B1ms + 20 GB | coberto pelas 750 horas mensais gratuitas por 12 meses; fora disso, cerca de US$ 15 por mês |
| Application Insights / Log Analytics | dentro da franquia gratuita de 5 GB por mês |

O plano F1 foi escolhido para não consumir o crédito de US$ 100 do Azure for Students. A contrapartida é que ele não tem Always On: depois de uns 20 minutos sem acesso a aplicação é descarregada e o próximo acesso leva de 1 a 2 minutos para religar o Spring Boot. Para a apresentação, basta abrir o endereço alguns minutos antes. Se o sistema passasse a ser usado de verdade por um condomínio, o primeiro ajuste seria subir para o B1 (cerca de US$ 13 a 18 por mês) com `.\infra\deploy.ps1 -Plano B1`, sem mudar código. O ambiente inteiro pode ser apagado com `.\infra\remover.ps1`.

Para crescer, não é preciso mudar a arquitetura: basta subir o plano (F1 para B1, S1 ou P1v3, que permitem várias instâncias e escala automática) e a camada do banco (B1ms para B2s ou General Purpose). Como a API é stateless, várias instâncias funcionam sem ajuste no código.

## 9. Passo a passo para criar o ambiente

Pré-requisitos: conta Azure for Students ativa, [Azure CLI](https://learn.microsoft.com/cli/azure/install-azure-cli) e JDK 21.

```powershell
az login
.\infra\deploy.ps1
```

O script faz, em ordem:

1. confere se o Azure CLI está instalado e logado;
2. registra os provedores `Microsoft.Web`, `Microsoft.DBforMySQL`, `Microsoft.Insights` e `Microsoft.OperationalInsights`;
3. lê a política *Allowed locations* da assinatura de estudante (que libera só algumas regiões) e escolhe Brazil South se estiver liberada, senão a primeira região permitida da lista de preferência;
4. gera a senha do MySQL e o segredo do JWT e guarda em `infra/.segredos.local.json` (fora do Git);
5. cria o grupo `rg-jcondo` e aplica o `main.bicep` (de 5 a 15 minutos);
6. gera o `jcondo.jar` e publica com `az webapp deploy`;
7. espera o health check responder `UP`;
8. salva o perfil de publicação e mostra o que cadastrar no GitHub.

No GitHub, em *Settings > Secrets and variables > Actions*, cadastrar a variable `AZURE_WEBAPP_NAME` e o secret `AZURE_WEBAPP_PUBLISH_PROFILE`. A partir daí, todo push na main publica sozinho.

### Problemas comuns

- **`RequestDisallowedByAzure` ou região não permitida:** a assinatura de estudante bloqueia a região. Rodar de novo informando outra: `.\infra\deploy.ps1 -Regiao eastus`.
- **MySQL indisponível na região:** algumas regiões não aceitam novos servidores para contas de estudante. Mesma solução, trocar a região.
- **Aplicação demora a responder no F1:** o plano gratuito não tem Always On e tem cota de CPU diária. Abrir o endereço uns minutos antes de usar, ou subir para o B1.
- **Erro de conexão com o banco:** conferir no portal se a regra `PermitirServicosAzure` existe no firewall do MySQL e se a `DB_URL` tem `sslMode=REQUIRED`.
