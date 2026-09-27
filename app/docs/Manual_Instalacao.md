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
Se quiser instalar mesmo assim:

1. Baixe em https://maven.apache.org/download.cgi (o zip binário)
2. Extraia em uma pasta, ex: C:\maven
3. Adicione C:\maven\bin no PATH
4. Teste com `mvn -version`

## 3. Instalar o XAMPP

1. Baixe em https://www.apachefriends.org e instale
2. Abra o XAMPP Control Panel
3. Clique em Start no MySQL
4. Se for usar o phpMyAdmin, dê Start no Apache também

O MySQL do XAMPP vem com usuário root sem senha, que é exatamente como o
projeto está configurado.

## 4. Criar o banco de dados

Jeito mais fácil, pelo phpMyAdmin:

1. Acesse http://localhost/phpmyadmin
2. Aba "Importar"
3. Escolha o arquivo `database/jcondo.sql` que está na pasta do projeto
4. Clique em Executar

Se preferir o terminal:

```
mysql -u root < database/jcondo.sql
```

Depois de importar, confira se o banco `jcondo` apareceu na lista da esquerda
com a tabela `moradores` e 5 registros de exemplo.

## 5. Conferindo se está tudo pronto

- `java -version` mostra a versão 21
- MySQL aparece verde (Running) no XAMPP
- Banco jcondo criado com a tabela moradores

Se estiver tudo ok, segue pro Manual_Execucao.md.
