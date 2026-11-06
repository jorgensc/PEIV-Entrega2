# API REST do JCondo

Base: `/api/v1`. Documentação interativa: `/swagger-ui/index.html`.

**Autenticação:** `POST /auth/login` devolve um token JWT. Nas outras chamadas, enviar `Authorization: Bearer <token>`.

| Método | Rota | Perfil |
|---|---|---|
| POST | `/auth/login` | público |
| GET | `/auth/me` | logado |
| GET, POST | `/moradores` | síndico |
| GET, PUT, DELETE | `/moradores/{id}` | síndico |
| GET | `/avisos`, `/avisos/{id}` | logado |
| POST, PUT, DELETE | `/avisos`, `/avisos/{id}` | síndico |
| GET | `/areas`, `/areas/{id}` | logado |
| POST, PUT, DELETE | `/areas`, `/areas/{id}` | síndico |
| GET, POST | `/reservas` | logado (morador vê só as dele) |
| GET | `/reservas/disponibilidade?areaId=&data=` | logado |
| PATCH | `/reservas/{id}/cancelar` | dono ou síndico |
| GET, POST | `/ocorrencias` | logado (morador vê só as dele) |
| GET | `/ocorrencias/{id}` | dono ou síndico |
| PATCH | `/ocorrencias/{id}` | síndico |
| GET | `/resumo` | logado |

**Reserva:** só é confirmada dentro do horário da área, em data futura (até 90 dias) e sem sobrepor outra reserva.

**Erros** no padrão Problem Details (RFC 9457): 400 dados inválidos, 401 sem login, 403 sem permissão, 404 não encontrado, 409 horário ocupado, 422 regra violada.
