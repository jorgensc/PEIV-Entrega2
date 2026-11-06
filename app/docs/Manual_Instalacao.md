# Manual de Instalação - JCondo

1. Instale o JDK 21 (https://adoptium.net) e confira com `java -version`.
2. O Maven não precisa ser instalado: o projeto tem o wrapper (`mvnw`).
3. Banco de dados:
   - sem MySQL: use o perfil `local` (banco H2 na pasta `app/data`);
   - com MySQL (por exemplo o do XAMPP): ligue o MySQL; o banco `jcondo` é criado pela aplicação.
4. Para o servidor com link público, na raiz do repositório: `infra\instalar.cmd`.

Depois siga o Manual_Execucao.md.
