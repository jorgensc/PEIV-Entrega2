# JCondo - Sistema de Gestão de Condomínio

Aplicação do JCondo em um único projeto Spring Boot, que gera um `.jar` com três partes:

- **painel do síndico** (Thymeleaf + Bootstrap, responsivo): moradores, avisos, reservas, ocorrências e áreas comuns;
- **app do morador** (PWA em `/app/`): HTML, CSS e JavaScript puros, com service worker, manifest e modo offline;
- **API REST** (`/api/v1`): JWT, perfis síndico/morador, erros no padrão RFC 9457 e Swagger.

## Tecnologias

- Java 21, Spring Boot 3.5 (Web MVC, Data JPA, Validation, Security, OAuth2 Resource Server, Actuator)
- Hibernate, MySQL 8 (H2 no perfil local e nos testes)
- Thymeleaf, Bootstrap 5, Bootstrap Icons
- PWA: Web App Manifest, Service Worker, Cache API, localStorage
- springdoc-openapi (Swagger)
- JUnit 5, Mockito, MockMvc
- Maven (com wrapper)

## Requisitos

- JDK 21
- MySQL na porta 3306 (o do XAMPP serve) **ou** nada, usando o perfil `local` com H2
- Não precisa instalar o Maven, o projeto tem o wrapper (`mvnw`)

## Como executar

Jeito mais rápido, sem MySQL (o banco fica num arquivo em `app/data/`):

```
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local        (Windows)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local          (Linux/Mac)
```

Com o MySQL do XAMPP ligado (o banco `jcondo` é criado sozinho):

```
mvnw.cmd spring-boot:run
```

Depois abrir:

| Endereço | O que é |
|---|---|
| http://localhost:8080 | painel do síndico (`sindico@jcondo.com` / `sindico123`) |
| http://localhost:8080/app/ | app do morador (`ana.souza@email.com` / `morador123`) |
| http://localhost:8080/swagger-ui/index.html | documentação da API |
| http://localhost:8080/actuator/health | health check |

Na primeira subida, com o banco vazio, a aplicação cria as tabelas e carrega dados de demonstração (síndico, 5 moradores, 4 áreas, avisos, reservas e ocorrências). Para não carregar, usar `SEED_DEMO=false`.

> O PWA só registra o service worker em `localhost` ou em HTTPS. Para testar no celular, usar o endereço do Azure.

## Configuração

Tudo que muda entre local e nuvem vem de variáveis de ambiente, com padrão para o XAMPP:

| Variável | Padrão local | Uso |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/jcondo?createDatabaseIfNotExist=true` | conexão |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / vazio | usuário do banco |
| `JWT_SECRET` | segredo de desenvolvimento | assinatura dos tokens |
| `SEED_DEMO` | `true` | dados de demonstração |
| `SHOW_SQL` | `false` | log das consultas |

## Testes

```
mvnw.cmd test
```

São 30 testes, todos sem precisar de MySQL:

- `MoradorServiceTest` e `ReservaServiceTest`: regras de negócio com Mockito (conflito de horário, data passada, horário da área, permissão de cancelamento, criação do acesso do morador);
- `ApiIntegracaoTest`: sobe a aplicação com H2 e testa a API de ponta a ponta (login, 401, 403, 409, 400, fluxo de ocorrência, health check);
- `JcondoApplicationTests`: carga do contexto.

## Arquitetura

Camadas: `api`/`controller` → `service` → `repository` → `entity`. Os controllers nunca acessam o repository direto, e a API nunca devolve entidade: usa DTOs (records) para não expor dados como o hash da senha.

```
src/main/java/jcondo/
├── api/            REST /api/v1 (Auth, Moradores, Avisos, Areas, Reservas, Ocorrencias, Resumo) + dto/
├── controller/     telas do painel (Painel, Morador, Aviso, Area, Reserva, Ocorrencia, Home)
├── service/        regras de negócio
├── repository/     Spring Data JPA
├── entity/         Usuario, Morador, Aviso, AreaComum, Reserva, Ocorrencia + enums
├── security/       SecurityConfig (duas cadeias: sessão no painel e JWT na API), TokenService
├── config/         Swagger, relógio (fuso de Brasília), dados de demonstração, MIME do manifest
└── exception/      NaoEncontrado (404), Conflito (409), RegraNegocio (422)

src/main/resources/
├── templates/      layout, login, painel e pastas de cada módulo
├── static/css, js  estilo e scripts do painel
└── static/app/     PWA: index.html, manifest.webmanifest, sw.js, js/, css/, icons/
```

Manuais: [`docs/Manual_Instalacao.md`](docs/Manual_Instalacao.md) e [`docs/Manual_Execucao.md`](docs/Manual_Execucao.md).

## Autor

Jorge Luiz do Nascimento - UNOESC
