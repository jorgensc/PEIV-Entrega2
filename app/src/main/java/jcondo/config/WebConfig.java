package jcondo.config;

import java.util.List;

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

    // /app sem barra abre o index do PWA
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/app", "/app/");
        registry.addViewController("/app/").setViewName("forward:/app/index.html");
    }
}
