# Manual de Instalação - JCondo

Passo a passo pra preparar o ambiente antes de rodar o sistema.

## 1. Instalar o Java 21

1. Baixe o JDK 21 em https://adoptium.net (ou no site da Oracle)
2. Instale normalmente (next, next, finish)
3. Confira no terminal:

```
java -version
```

Tem que aparecer a versão 21. Se o comando não for reconhecido no Windows,
adicione a pasta bin do JDK na variável PATH e crie a variável JAVA_HOME
apontando pra pasta de instalação.

## 2. Maven (opcional)

O projeto já vem com o Maven Wrapper (mvnw), então não é obrigatório instalar.

## 3. Banco de dados

Existem dois jeitos de rodar:

**Sem MySQL (mais rápido):** o perfil `local` usa o banco H2 gravado na pasta
`app/data`. Não precisa instalar nada, é só seguir pro Manual_Execucao.md.

**Com o MySQL do XAMPP:**

1. Baixe em https://www.apachefriends.org e instale
2. Abra o XAMPP Control Panel e clique em Start no MySQL

O MySQL do XAMPP vem com usuário root sem senha, que é como o projeto está
configurado. O banco `jcondo` e as tabelas são criados pela aplicação na
primeira vez que ela sobe. Se quiser criar na mão, importe o arquivo
`database/jcondo.sql` pelo phpMyAdmin (aba Importar) ou pelo terminal:

```
mysql -u root < database/jcondo.sql
```

## 4. Servidor self-hosted (pra deixar online)

Na raiz do repositório:

```
infra\instalar.cmd
```

Ele baixa o MySQL 8.4 e o cloudflared, configura tudo só para acesso local e
cria o banco, o usuário e as senhas. Depois é só rodar `infra\iniciar.cmd`.
O passo a passo e a configuração de cada parte estão em `doc/infraestrutura.md`.

## 5. Conferindo se está tudo pronto

- `java -version` mostra a versão 21
- (se for usar MySQL) MySQL aparece verde (Running) no XAMPP

Se estiver tudo ok, segue pro Manual_Execucao.md.
