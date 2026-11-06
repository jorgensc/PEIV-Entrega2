package jcondo;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JcondoApplication {

	public static void main(String[] args) {
		// o servidor pode estar em outro fuso; o condominio fica no horario de Brasilia
		TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
		SpringApplication.run(JcondoApplication.class, args);
	}

}
