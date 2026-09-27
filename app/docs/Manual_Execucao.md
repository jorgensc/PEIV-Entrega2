# Manual de Execução - JCondo

Antes de começar: o ambiente precisa estar montado conforme o Manual_Instalacao.md
(Java 21, MySQL do XAMPP rodando e banco jcondo importado).

## 1. Ligar o MySQL

Abra o XAMPP Control Panel e clique em Start no MySQL.

## 2. Rodar a aplicação

Abra o terminal na pasta raiz do projeto (onde está o pom.xml) e rode:

```
mvnw.cmd spring-boot:run        (Windows)
./mvnw spring-boot:run          (Linux/Mac)
```

Na primeira vez o Maven baixa as dependências, então demora um pouco e precisa
de internet. Quando aparecer "Started JcondoApplication" no console, está no ar.

## 3. Abrir o sistema

http://localhost:8080

Já redireciona pra tela de moradores, que mostra a tabela ordenada por nome
com os botões de ação:

- Novo: abre o formulário de cadastro
- Lápis (editar): abre o formulário preenchido com os dados do morador
- Lixeira (excluir): pede confirmação e remove o registro

Depois de salvar ou excluir aparece uma mensagem verde de sucesso no topo.
Se algum campo do formulário estiver inválido, o campo fica vermelho com a
mensagem de erro embaixo.

## 4. Abrir o Swagger

Com a aplicação rodando:

http://localhost:8080/swagger-ui/index.html

Lá estão documentados os endpoints da API REST:

- GET /api/moradores - lista todos
- GET /api/moradores/{id} - busca por id
- POST /api/moradores - cadastra
- PUT /api/moradores/{id} - atualiza
- DELETE /api/moradores/{id} - exclui

Dá pra testar direto pela interface usando o botão "Try it out".

## 5. Rodar os testes

```
mvnw.cmd test
```

Roda os testes unitários do service (com Mockito, sem precisar de banco).
Pra rodar também o teste de integração, que sobe o Spring inteiro e precisa
do MySQL ligado:

```
mvnw.cmd test -Dtest.integration=true
```

## 6. Parar a aplicação

Ctrl + C no terminal.

## Problemas comuns

- "Communications link failure": o MySQL está parado, liga no XAMPP
- "Access denied for user 'root'": seu MySQL tem senha, ajusta a propriedade
  spring.datasource.password no application.properties
- Porta 8080 ocupada: fecha o outro programa ou adiciona server.port=8081
  no application.properties
