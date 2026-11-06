# Configuração da infraestrutura self-hosted

Este documento descreve como foi configurada a infraestrutura que hospeda o JCondo na Entrega 2 de Práticas Extensionistas IV: quais componentes existem, como cada um foi ajustado, como eles conversam entre si, como a aplicação chega até o servidor e como ele é operado.

Toda a configuração está automatizada em scripts na pasta [`infra/`](../infra). Com eles, o servidor é montado do zero em um computador Windows com um comando (`instalar.cmd`) e colocado no ar com outro (`iniciar.cmd`).

## 1. Escolha: de nuvem para self-host

Na Entrega 1 a opção escolhida foi o Microsoft Azure, e o self-host aparecia na comparação com três desvantagens: precisar de **IP público ou túnel**, cuidar sozinho de **certificado HTTPS** e de **firewall**, e depender da **máquina ligada**.

Na hora de configurar, a decisão foi trocar para self-host. A assinatura de nuvem dependeria de uma conta institucional ativada, de regiões liberadas pela política de estudante e de crédito com prazo. Além disso, o plano gratuito do App Service desliga a aplicação quando ela fica parada. O que viabilizou a troca foi o **Cloudflare Tunnel**, que resolve três das quatro desvantagens apontadas na Entrega 1:

| Problema do self-host (Entrega 1) | Como foi resolvido |
|---|---|
| IP público / abrir porta no roteador | O túnel faz uma conexão de **saída** do servidor até a Cloudflare. Nenhuma porta é aberta e o IP de casa não aparece. |
| Certificado HTTPS | A Cloudflare entrega o endereço público já com certificado válido (TLS). O PWA exige HTTPS e funciona normalmente. |
| Firewall e segurança | O MySQL e a aplicação só escutam em `127.0.0.1`. Não existe nada exposto na rede local nem na internet além do túnel. |
| Máquina ligada | Continua valendo: o sistema fica no ar enquanto o servidor estiver ligado. Para um projeto acadêmico e para a apresentação isso é aceitável, e a seção 9 mostra o caminho para eliminar essa limitação. |

Com isso o custo é **zero** (sem cartão, sem crédito, sem prazo de expiração) e o controle sobre o ambiente é total.

## 2. Visão geral

```
   Celular / computador do usuário
              |
              | HTTPS 443 (certificado da Cloudflare)
              v
   +------------------------------+
   |  Cloudflare (borda, GRU/SP)  |   https://<nome>.trycloudflare.com
   +--------------+---------------+
                  ^  túnel: conexão de SAÍDA criptografada (QUIC/HTTP2)
                  |  nenhuma porta aberta no roteador
   +--------------+------------------------------------------------+
   |  SERVIDOR (Windows 11)                                        |
   |                                                               |
   |   cloudflared  --->  JCondo (Java 21 / Spring Boot)           |
   |                      127.0.0.1:8080                           |
   |                      painel /login · PWA /app · API /api/v1   |
   |                            |                                  |
   |                            | JDBC 127.0.0.1:3306              |
   |                            v                                  |
   |                      MySQL 8.4 LTS (banco jcondo)             |
   +---------------------------------------------------------------+
```

Um único processo Java entrega as três partes do sistema no mesmo endereço:

| Caminho | O que é | Quem usa |
|---|---|---|
| `/login`, `/painel`, `/moradores`... | Painel web responsivo (Thymeleaf + Bootstrap) | Síndico |
| `/app/` | App mobile PWA (instalável, funciona offline) | Moradores (e síndico) |
| `/api/v1/...` | API REST com autenticação JWT | O PWA e integrações |
| `/swagger-ui/index.html` | Documentação interativa da API | Desenvolvedores e professor |
| `/actuator/health` | Health check | Scripts do servidor |

## 3. Servidor

| Item | Configuração |
|---|---|
| Máquina | notebook Intel Core i7-13620H (10 núcleos), 16 GB de RAM, SSD de 476 GB |
| Sistema operacional | Windows 11 Pro |
| Pasta do servidor | `C:\Users\<usuário>\JCondoServidor` (fora do repositório) |
| Java | JDK 21 LTS |
| Banco | MySQL Community 8.4.3 LTS (versão zip, sem instalador e sem serviço do Windows) |
| Túnel | cloudflared (Cloudflare Tunnel, modo *quick tunnel*) |
| Consumo medido | aplicação em torno de 430 MB de RAM e MySQL em torno de 240 MB |

Estrutura da pasta do servidor, criada pelo `instalar.cmd`:

```
JCondoServidor/
├── mysql/            binários do MySQL 8.4.3 + my.ini
├── dados/mysql/      arquivos do banco
├── app/jcondo.jar    versão em produção (e jcondo.jar.anterior, para voltar)
├── cloudflared.exe
├── logs/             app.log, mysql-erros.log, tunel.log
├── backups/          dumps do banco (7 mais recentes)
├── config.json       senhas e segredo do JWT (gerados, nunca vão pro Git)
└── url-publica.txt   endereço HTTPS atual
```

## 4. Configuração de cada componente

### 4.1 MySQL

O `instalar.cmd` gera o `my.ini` abaixo, inicializa a pasta de dados e cria o banco e o usuário da aplicação.

| Configuração | Valor | Por quê |
|---|---|---|
| `bind-address` | `127.0.0.1` | O banco só aceita conexões do próprio servidor. |
| `port` | `3306` | Porta padrão, acessível só localmente. |
| `mysqlx` | `OFF` | Desliga o protocolo X (porta 33060), que não é usado. |
| `character-set-server` / `collation-server` | `utf8mb4` / `utf8mb4_unicode_ci` | Acentos e emojis nos avisos e ocorrências. |
| `default-time-zone` | `-03:00` | Horário de Brasília. |
| `max_connections` | `50` | Suficiente para um condomínio; o pool da aplicação usa 5. |
| `innodb_buffer_pool_size` | `256M` | Cabe o banco inteiro em memória sem pesar no computador. |
| Usuário `root` | senha aleatória de 24 caracteres | Usado só pelos scripts (backup e desligamento). |
| Usuário `jcondo` | senha aleatória, permissão só no banco `jcondo` | Usado pela aplicação (princípio do menor privilégio). |

As tabelas são criadas pela própria aplicação na primeira subida (JPA com `ddl-auto=update`), e com o banco vazio ela carrega os dados de demonstração. O esquema equivalente está em `app/database/jcondo.sql`.

### 4.2 Aplicação (JCondo)

O `iniciar.cmd` sobe o `jcondo.jar` com o Java 21 e passa a configuração por variáveis de ambiente, lidas no `application.properties`. Assim, nenhuma senha fica no código ou no Git.

| Variável / parâmetro | Valor | Uso |
|---|---|---|
| `--server.address` | `127.0.0.1` | A aplicação não fica visível na rede local, só para o túnel. |
| `--server.port` | `8080` | Porta interna. |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/jcondo?serverTimezone=America/Sao_Paulo&...` | Conexão com o MySQL local. |
| `DB_USERNAME` / `DB_PASSWORD` | `jcondo` / senha do `config.json` | Usuário com acesso só ao banco `jcondo`. |
| `JWT_SECRET` | 48 caracteres aleatórios do `config.json` | Assina os tokens da API. |
| `SEED_DEMO` | `true` | Carrega os dados de demonstração quando o banco está vazio. |
| `SHOW_SQL` | `false` | Não grava as consultas no log. |
| `TZ` | `America/Sao_Paulo` | As regras de reserva usam o horário de Brasília. |
| `-Xms256m -Xmx512m` | memória da JVM | Limita o uso de memória do computador. |

Outras configurações importantes no `application.properties`:

- `server.forward-headers-strategy=native`: por trás do túnel a requisição chega em HTTP, e com isso a aplicação entende que o usuário está em HTTPS e gera os redirecionamentos do login corretamente;
- um filtro (`RedirecionarHttpsFilter`) manda para `https://` quem entra pelo túnel usando `http://`;
- `spring.datasource.hikari.maximum-pool-size=5`: pool de conexões pequeno;
- `server.compression.enabled=true`: compressão de HTML, CSS, JS e JSON;
- `spring.web.resources.cache.cachecontrol.no-cache=true`: arquivos estáticos sempre revalidam, e o service worker do PWA cuida do modo offline.

### 4.3 Cloudflare Tunnel

O `cloudflared` roda com `tunnel --url http://127.0.0.1:8080`. Ele abre conexões criptografadas de saída (QUIC, com HTTP/2 de reserva) até a borda da Cloudflare mais próxima, que nos testes foi São Paulo (`gru19`), e recebe um endereço público `https://<nome>.trycloudflare.com`.

- Não precisa de conta, domínio nem cartão.
- O certificado HTTPS é da Cloudflare, válido em qualquer navegador e celular.
- O endereço muda cada vez que o túnel é aberto. O `iniciar.cmd` lê o endereço novo no log, mostra na tela, salva em `url-publica.txt` e confere o health check por ele. O QR code do painel do síndico usa o endereço atual, então sempre aponta para o link certo.
- O `atualizar.cmd` troca a versão da aplicação sem fechar o túnel, então o link continua o mesmo.

## 5. Segurança

1. **Rede:** nada escuta em `0.0.0.0`. MySQL (`127.0.0.1:3306`) e aplicação (`127.0.0.1:8080`) só aceitam conexões locais, conferido com `netstat`. O roteador e o firewall do Windows não foram alterados.
2. **Transporte:** HTTPS da Cloudflare até o usuário, com redirecionamento automático de HTTP para HTTPS. Entre a Cloudflare e o servidor o túnel é criptografado.
3. **Segredos:** senhas do MySQL e segredo do JWT gerados aleatoriamente (gerador criptográfico do .NET) no `config.json` do servidor, fora do repositório. Os scripts passam a senha ao MySQL por arquivo temporário, nunca na linha de comando.
4. **Banco:** usuário da aplicação com permissão apenas no banco `jcondo`.
5. **Aplicação:** login com sessão e proteção CSRF no painel; token JWT (HS256, 12 horas) na API; senhas com hash BCrypt; perfis SÍNDICO e MORADOR conferidos em cada operação (401/403 no padrão Problem Details).
6. **Scripts:** os atalhos `.cmd` rodam os `.ps1` com `-ExecutionPolicy Bypass` só naquela execução, sem mudar a política de segurança do Windows.

## 6. Publicação (CI/CD)

O pipeline `.github/workflows/ci-cd.yml` roda no GitHub Actions:

1. **CI, a cada push ou pull request:** instala o JDK 21, roda `./mvnw verify` (30 testes, entre unitários e de integração da API, com banco H2 em memória) e guarda o `jcondo.jar` e o relatório dos testes.
2. **CD, em push na main:** cria uma *Release* no GitHub (`v2.0.<número>`) com o `jcondo.jar` já testado.
3. **Deploy no servidor:** `infra\atualizar.cmd` baixa o `jcondo.jar` da última Release, guarda a versão atual como `jcondo.jar.anterior`, troca e sobe de novo, conferindo o health check. Como o servidor está atrás do túnel e não aceita conexões de fora, o deploy é do tipo *pull*: o servidor busca a versão, em vez de o GitHub empurrar.

Para testar uma alteração antes do push: `infra\atualizar.cmd -Local` usa o `.jar` compilado no próprio repositório.

## 7. Operação

| Tarefa | Comando | O que faz |
|---|---|---|
| Montar o servidor (uma vez) | `infra\instalar.cmd` | Baixa MySQL e cloudflared, configura, cria banco, usuário e segredos, copia a aplicação. |
| Colocar no ar | `infra\iniciar.cmd` | Sobe MySQL, aplicação e túnel, nessa ordem, e mostra os endereços. |
| Só na máquina, sem túnel | `infra\iniciar.cmd -SemTunel` | Mesmo processo, acessível em `http://localhost:8080`. |
| Publicar versão nova | `infra\atualizar.cmd` | Baixa a última Release e reinicia só a aplicação. |
| Backup do banco | `infra\backup.cmd` | `mysqldump` em `backups\`, mantendo os 7 mais recentes. |
| Desligar | `infra\parar.cmd` | Fecha o túnel, para a aplicação e desliga o MySQL de forma limpa. |

Monitoramento e diagnóstico:

- **Health check:** `/actuator/health` devolve `{"status":"UP"}` e inclui a conexão com o banco. Os scripts só consideram a aplicação no ar quando ele responde.
- **Logs:** `logs\app.log` (Spring Boot), `logs\mysql-erros.log` (MySQL) e `logs\tunel.log` (conexões do túnel e endereço público).
- **Métricas do túnel:** o cloudflared expõe métricas em `127.0.0.1:20241/metrics`.
- **Backup e restauração:** `backup.cmd` gera o dump; para restaurar, `mysql -u root -p jcondo < backups\jcondo-AAAAMMDD-HHMM.sql`.

## 8. Custos

| Item | Custo |
|---|---|
| Servidor | o próprio computador, sem custo extra |
| MySQL Community, Java (OpenJDK/Oracle JDK 21), cloudflared | gratuitos |
| Cloudflare Tunnel (quick tunnel) | gratuito, sem conta |
| Energia e internet | as que já existem |
| **Total** | **R$ 0,00** |

## 9. Limitações e evolução

| Limitação atual | Como evoluir |
|---|---|
| O sistema só fica no ar com o computador ligado | Mover a mesma pasta para um computador sempre ligado (um mini PC ou uma VPS) e registrar MySQL, aplicação e túnel como serviços do Windows ou do Linux. |
| O endereço muda a cada `iniciar.cmd` | Criar uma conta gratuita na Cloudflare e um *named tunnel* com domínio próprio (por exemplo `jcondo.meudominio.com.br`), que mantém o endereço fixo. |
| Um servidor só, sem redundância | Como a API é stateless (JWT), dá para rodar mais de uma instância atrás do túnel; o banco pode ganhar replicação. |
| Backup manual | Agendar o `backup.cmd` no Agendador de Tarefas do Windows e copiar os dumps para outro disco ou para a nuvem. |

## 10. Passo a passo para reproduzir

Pré-requisitos: Windows 10/11 e JDK 21.

```
git clone https://github.com/jorgensc/PEIV.git
cd PEIV
infra\instalar.cmd
infra\iniciar.cmd
```

O `iniciar.cmd` mostra no final:

```
 JCondo no ar
 Painel do sindico : https://<nome>.trycloudflare.com/login
 App do morador    : https://<nome>.trycloudflare.com/app/
 API (Swagger)     : https://<nome>.trycloudflare.com/swagger-ui/index.html
 Health check      : https://<nome>.trycloudflare.com/actuator/health
```

### Problemas comuns

- **"execução de scripts foi desabilitada":** usar os atalhos `.cmd` em vez dos `.ps1`.
- **Porta 3306 ou 8080 ocupada:** outro MySQL (XAMPP) ou outra aplicação está rodando. Desligue o outro programa ou mude `porta` no `config.json`.
- **O túnel não mostra o endereço:** conferir a internet e o `logs\tunel.log`. Redes que bloqueiam UDP fazem o cloudflared usar HTTP/2 automaticamente.
- **A aplicação não sobe:** ver `logs\app.log`. O erro mais comum é o MySQL desligado.
