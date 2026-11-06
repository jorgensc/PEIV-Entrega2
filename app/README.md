# JCondo - aplicação

Projeto Spring Boot que gera um único `jcondo.jar` com:

- painel do síndico (`/login`): Thymeleaf + Bootstrap, responsivo;
- app do morador (`/app/`): PWA com service worker e modo offline;
- API REST (`/api/v1`): JWT e Swagger em `/swagger-ui/index.html`.

## Tecnologias

Java 21, Spring Boot 3.5 (Web, Data JPA, Validation, Security), MySQL 8 (H2 no perfil local e nos testes), Thymeleaf, Bootstrap 5, springdoc-openapi, JUnit 5 e Mockito.

## Executar

```
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local    (sem MySQL)
mvnw.cmd spring-boot:run                                     (com MySQL local)
```

Abrir http://localhost:8080. Com o banco vazio, a aplicação carrega dados de demonstração.

## Testes

```
mvnw.cmd test
```

30 testes: regras de negócio (Mockito) e a API de ponta a ponta (H2).

## Estrutura

```
src/main/java/jcondo/
├── api/          controllers REST e DTOs
├── controller/   telas do painel
├── service/      regras de negócio
├── repository/   acesso ao banco
├── entity/       entidades JPA
├── security/     login e JWT
├── config/       configurações
└── exception/    exceções de negócio
src/main/resources/
├── templates/    páginas do painel
└── static/app/   PWA
```

## Autor

Jorge Luiz do Nascimento - UNOESC
