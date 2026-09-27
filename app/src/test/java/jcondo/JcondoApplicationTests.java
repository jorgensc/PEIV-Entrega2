package jcondo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

// Teste de integracao: sobe o contexto inteiro do Spring, entao precisa do MySQL rodando.
// Pra nao quebrar o build quando o banco estiver parado, so roda com:
// mvnw test -Dtest.integration=true
@SpringBootTest
@EnabledIfSystemProperty(named = "test.integration", matches = "true")
class JcondoApplicationTests {

	@Test
	void contextLoads() {
	}

}
