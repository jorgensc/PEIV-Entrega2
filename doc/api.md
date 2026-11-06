# API REST do JCondo (v1)

A API é a mesma que o app PWA usa. Ela fica em `/api/v1`, responde em JSON e está documentada de forma interativa no Swagger (`/swagger-ui/index.html`), onde dá para testar cada rota.

## Autenticação

1. Fazer login em `POST /api/v1/auth/login` com e-mail e senha.
2. A resposta traz um `token` JWT (assinado com HS256, válido por 12 horas) e os dados do usuário.
3. Nas demais chamadas, mandar o cabeçalho `Authorization: Bearer <token>`.

No Swagger: executar o login, copiar o `token` e colar no botão **Authorize**.

Usuários de demonstração (criados automaticamente quando o banco está vazio):

| Perfil | E-mail | Senha |
|---|---|---|
| Síndico | `sindico@jcondo.com` | `sindico123` |
| Morador | `ana.souza@email.com` (e os outros 4 moradores) | `morador123` |

## Endpoints

| Método | Rota | Quem pode | Descrição |
|---|---|---|---|
| POST | `/auth/login` | público | Entrar e receber o token |
| GET | `/auth/me` | logado | Dados do usuário logado |
| PUT | `/auth/senha` | logado | Trocar a própria senha |
| GET | `/moradores` | síndico | Listar moradores |
| GET | `/moradores/{id}` | síndico | Buscar morador |
| POST | `/moradores` | síndico | Cadastrar morador e criar o acesso ao app |
| PUT | `/moradores/{id}` | síndico | Atualizar morador |
| DELETE | `/moradores/{id}` | síndico | Excluir morador (e reservas, ocorrências e acesso) |
| GET | `/avisos` | logado | Listar avisos (importantes primeiro) |
| GET | `/avisos/{id}` | logado | Detalhe do aviso |
| POST | `/avisos` | síndico | Publicar aviso |
| PUT | `/avisos/{id}` | síndico | Editar aviso |
| DELETE | `/avisos/{id}` | síndico | Excluir aviso |
| GET | `/areas?todas=false` | logado | Listar áreas comuns |
| GET | `/areas/{id}` | logado | Detalhe da área |
| POST | `/areas` | síndico | Cadastrar área |
| PUT | `/areas/{id}` | síndico | Atualizar área |
| DELETE | `/areas/{id}` | síndico | Excluir área sem reservas |
| GET | `/reservas?data=AAAA-MM-DD` | logado | Síndico vê todas; morador vê só as dele |
| GET | `/reservas/disponibilidade?areaId=&data=` | logado | Horários ocupados da área no dia |
| POST | `/reservas` | logado | Criar reserva |
| PATCH | `/reservas/{id}/cancelar` | dono ou síndico | Cancelar reserva |
| GET | `/ocorrencias?status=` | logado | Síndico vê todas; morador vê só as dele |
| GET | `/ocorrencias/{id}` | dono ou síndico | Detalhe da ocorrência |
| POST | `/ocorrencias` | morador | Abrir ocorrência |
| PATCH | `/ocorrencias/{id}` | síndico | Mudar status e responder |
| GET | `/resumo` | logado | Números da tela inicial (por perfil) |

Fora da API: `GET /actuator/health` (público) devolve `{"status":"UP"}` e é usado pelo Azure e pelo pipeline.

## Regras da reserva

O `POST /reservas` só confirma quando:

- a área está ativa;
- o horário final é depois do inicial e a reserva tem pelo menos 30 minutos;
- a data não passou (e, se for hoje, o horário ainda não passou);
- a data está a no máximo 90 dias de hoje;
- o horário está dentro do funcionamento da área;
- não existe outra reserva confirmada na mesma área com horário sobreposto.

Para evitar que dois moradores peguem o mesmo horário ao mesmo tempo, a área é travada no banco (`SELECT ... FOR UPDATE`) durante a verificação.

## Códigos de resposta

| Código | Quando |
|---|---|
| 200 / 201 / 204 | Sucesso (o 201 traz o cabeçalho `Location` do recurso criado) |
| 400 | Campos inválidos (com a mensagem de cada campo em `campos`) |
| 401 | Sem token, token inválido ou expirado, ou login errado |
| 403 | O perfil não pode fazer a operação |
| 404 | Registro não encontrado |
| 409 | Conflito: horário já reservado ou e-mail já cadastrado |
| 422 | Regra de negócio violada (data passada, fora do horário...) |

Todos os erros seguem o padrão **Problem Details (RFC 9457)**:

```json
{
  "type": "about:blank",
  "title": "Conflito",
  "status": 409,
  "detail": "Horário indisponível: já existe reserva para Salão de festas nesse período",
  "instance": "/api/v1/reservas"
}
```

```json
{
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Confira os campos destacados.",
  "campos": {
    "titulo": "O título é obrigatório",
    "categoria": "Escolha uma categoria"
  }
}
```

## Exemplo com curl

```bash
# login
TOKEN=$(curl -s -X POST https://<app>.azurewebsites.net/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana.souza@email.com","senha":"morador123"}' | jq -r .token)

# horarios ocupados do salao (area 1) em 12/12/2026
curl -s "https://<app>.azurewebsites.net/api/v1/reservas/disponibilidade?areaId=1&data=2026-12-12" \
  -H "Authorization: Bearer $TOKEN"

# reservar
curl -s -X POST https://<app>.azurewebsites.net/api/v1/reservas \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"areaId":1,"data":"2026-12-12","horaInicio":"14:00","horaFim":"18:00","observacao":"Aniversário"}'
```

## Testes automatizados

`app/src/test/java/jcondo/api/ApiIntegracaoTest.java` sobe a aplicação inteira com banco H2 e testa a API de ponta a ponta: login, 401 sem token, 403 por perfil, morador vendo só os próprios dados, conflito de reserva (409), validação (400) e o fluxo de ocorrência (morador abre, síndico responde). Esses testes rodam no GitHub Actions a cada push.
