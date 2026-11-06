# Manual de Execução - JCondo

## Local

Na pasta `app`:

```
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

## Servidor com link público

Na raiz do repositório:

```
infra\iniciar.cmd
```

O script mostra o link `https://<nome>.trycloudflare.com`. Para desligar: `infra\parar.cmd`.

## Acesso

| Endereço | Usuário |
|---|---|
| `/login` (painel do síndico) | `sindico@jcondo.com` / `sindico123` |
| `/app/` (app do morador) | `ana.souza@email.com` / `morador123` |
| `/swagger-ui/index.html` (API) | login em `POST /api/v1/auth/login` e token no botão Authorize |

## Testes

```
mvnw.cmd test
```
