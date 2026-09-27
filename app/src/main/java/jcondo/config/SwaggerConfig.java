package jcondo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

// Configuracao do Swagger - abre em http://localhost:8080/swagger-ui/index.html
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI jcondoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JCondo API")
                        .description("API REST para gerenciamento de moradores de condomínio")
                        .version("1.0.0"));
    }
}
