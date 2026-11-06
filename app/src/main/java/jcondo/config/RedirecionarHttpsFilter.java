package jcondo.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// quem entra pelo tunel usando http:// vai pro https:// (o PWA so funciona com HTTPS)
// acesso direto em localhost nao tem o cabecalho e passa normal
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RedirecionarHttpsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String protocolo = req.getHeader("X-Forwarded-Proto");
        String host = req.getHeader("Host");
        if ("http".equalsIgnoreCase(protocolo) && host != null) {
            String destino = "https://" + host + req.getRequestURI()
                    + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
            res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            res.setHeader("Location", destino);
            return;
        }
        chain.doFilter(req, res);
    }
}
