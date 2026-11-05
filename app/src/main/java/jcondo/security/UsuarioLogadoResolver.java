package jcondo.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jcondo.repository.UsuarioRepository;

// deixa o controller receber UsuarioLogado como parametro
@Component
public class UsuarioLogadoResolver implements HandlerMethodArgumentResolver {

    private final UsuarioRepository usuarios;

    public UsuarioLogadoResolver(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(UsuarioLogado.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        // API: os dados ja vem no token
        if (auth.getPrincipal() instanceof Jwt jwt) {
            return UsuarioLogado.de(jwt);
        }
        // telas: busca pelo e-mail da sessao
        return usuarios.findByEmailIgnoreCase(auth.getName())
                .map(UsuarioLogado::de)
                .orElse(null);
    }
}
