# Configuração da infraestrutura

O JCondo roda em servidor próprio (self-hosted) e fica acessível na internet em HTTPS pelo Cloudflare Tunnel.

## Escolha

Na Entrega 1 a opção era o Microsoft Azure. Na configuração, a hospedagem passou para servidor próprio, que não depende de conta institucional nem de crédito com prazo. O Cloudflare Tunnel resolve os pontos fracos do self-host apontados na Entrega 1: não precisa de IP público nem abrir porta no roteador, já entrega HTTPS com certificado válido e mantém o banco e a aplicação fora da rede. O custo é zero.

## Arquitetura

```
Usuário --HTTPS--> Cloudflare --túnel de saída--> cloudflared --> JCondo (127.0.0.1:8080) --> MySQL (127.0.0.1:3306)
```

## Componentes

| Componente | Configuração |
|---|---|
| Servidor | notebook com Windows 11 |
| Aplicação | Java 21, `jcondo.jar` (Spring Boot), escuta só em `127.0.0.1:8080` |
| Banco | MySQL 8.4 LTS, escuta só em `127.0.0.1:3306`, `utf8mb4`, fuso `-03:00` |
| Túnel | `cloudflared tunnel --url http://127.0.0.1:8080`, gera `https://<nome>.trycloudflare.com` |

A aplicação recebe a configuração por variáveis de ambiente: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` e `TZ`. As senhas são geradas na instalação e ficam em `JCondoServidor\config.json`, fora do Git. O usuário do banco só tem acesso ao banco `jcondo`.

## Segurança

- Nada fica exposto na rede: banco e aplicação só aceitam conexão local.
- HTTPS até o usuário, com redirecionamento de HTTP para HTTPS.
- Senhas com BCrypt, JWT na API e perfil conferido em cada operação.

## Publicação

O GitHub Actions roda os testes a cada push e, na `main`, gera uma Release com o `jcondo.jar`. No servidor, `infra\atualizar.cmd` baixa essa versão e reinicia a aplicação.

## Scripts

| Script | Função |
|---|---|
| `infra\instalar.cmd` | baixa MySQL e cloudflared, cria o banco, o usuário e as senhas |
| `infra\iniciar.cmd` | sobe MySQL, aplicação e túnel e mostra o link |
| `infra\atualizar.cmd` | publica a última versão |
| `infra\parar.cmd` | desliga tudo |

## Limitação

O sistema fica no ar enquanto o servidor estiver ligado, e o link muda a cada `iniciar.cmd`.
