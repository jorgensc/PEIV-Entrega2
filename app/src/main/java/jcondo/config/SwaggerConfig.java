package jcondo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jcondo.security.UsuarioLogado;
import org.springdoc.core.utils.SpringDocUtils;

// Configuracao do Swagger - abre em /swagger-ui/index.html
// botao Authorize: cola o token devolvido pelo POST /api/v1/auth/login
@Configuration
public class SwaggerConfig {

    static {
        // parametro preenchido pelo resolver, nao aparece na documentacao
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(UsuarioLogado.class);
    }

    @Bean
    public OpenAPI jcondoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JCondo API")
                        .description("API REST do JCondo. Faça o login, copie o token e use o botão Authorize.")
                        .version("2.0.0"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
