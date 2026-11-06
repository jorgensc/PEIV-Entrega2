package jcondo.config;

import java.util.List;

import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jcondo.security.UsuarioLogadoResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UsuarioLogadoResolver usuarioLogadoResolver;

    public WebConfig(UsuarioLogadoResolver usuarioLogadoResolver) {
        this.usuarioLogadoResolver = usuarioLogadoResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(usuarioLogadoResolver);
    }

    // o Tomcat nao conhece a extensao do manifest do PWA
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> tiposDeArquivo() {
        return factory -> {
            MimeMappings tipos = new MimeMappings(MimeMappings.DEFAULT);
            tipos.add("webmanifest", "application/manifest+json");
            factory.setMimeMappings(tipos);
        };
    }

    // /app sem barra abre o index do PWA
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/app", "/app/");
        registry.addViewController("/app/").setViewName("forward:/app/index.html");
    }
}
