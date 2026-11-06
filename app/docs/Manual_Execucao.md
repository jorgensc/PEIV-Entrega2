# Manual de Execução - JCondo

Antes de começar: o ambiente precisa estar montado conforme o Manual_Instalacao.md.

## 1. Rodar a aplicação

Abra o terminal na pasta `app` (onde está o pom.xml) e rode um dos comandos:

```
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local     (sem MySQL, Windows)
mvnw.cmd spring-boot:run                                      (com MySQL do XAMPP)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local       (Linux/Mac)
```

Na primeira vez o Maven baixa as dependências, então demora um pouco e precisa
de internet. Quando aparecer "Started JcondoApplication" no console, está no ar.
Com o banco vazio, a aplicação já carrega dados de exemplo.

## 2. Painel do síndico

http://localhost:8080 (cai na tela de login)

Usuário: `sindico@jcondo.com` / senha `sindico123` (tem um botão que preenche).

- **Painel:** números do condomínio, próximas reservas, ocorrências aguardando e QR code do app
- **Moradores:** cadastro com a senha de acesso do morador ao app
- **Avisos:** publicar, editar e excluir (o que for publicado aparece no app)
- **Reservas:** agenda com filtro por data, reservar em nome de um morador, cancelar
- **Ocorrências:** filtrar por status, abrir, mudar status e responder ao morador
- **Áreas comuns:** horário de funcionamento, capacidade e desativar

Para ver a responsividade, diminua a janela do navegador ou use o modo
dispositivo do DevTools (F12, Ctrl+Shift+M).

## 3. App do morador (PWA)

http://localhost:8080/app/

Usuário: `ana.souza@email.com` / senha `morador123` (os outros moradores usam
a mesma senha). No Chrome/Edge aparece o botão de instalar o app.

Para testar o modo offline: abra as telas uma vez, depois no DevTools vá em
Network e marque "Offline". O app continua abrindo, mostra o selo "Offline" e
uma ocorrência registrada fica na fila até a internet voltar.

## 4. API e Swagger

http://localhost:8080/swagger-ui/index.html

1. Abra `POST /api/v1/auth/login`, clique em "Try it out" e envie
   `{"email":"sindico@jcondo.com","senha":"sindico123"}`
2. Copie o campo `token` da resposta
3. Clique em **Authorize**, cole o token e confirme
4. Agora dá pra testar todas as rotas

A lista completa está em `doc/api.md`.

## 5. Rodar os testes

```
mvnw.cmd test
```

Roda os 30 testes (unitários e de integração da API). Nenhum precisa de MySQL.

## 6. Parar a aplicação

Ctrl + C no terminal.

## Problemas comuns

- "Communications link failure": o MySQL está parado, liga no XAMPP ou use o perfil local
- "Access denied for user 'root'": seu MySQL tem senha, defina a variável DB_PASSWORD
- Porta 8080 ocupada: rode com `-Dspring-boot.run.arguments=--server.port=8081`
- Para começar do zero no perfil local: apague a pasta `app/data`
