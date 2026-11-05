package jcondo.security;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 401 e 403 da API respondem em JSON (RFC 9457), igual aos outros erros
@Component
public class ApiErroSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    public ApiErroSeguranca(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res,
                         AuthenticationException ex) throws IOException {
        res.setHeader("WWW-Authenticate", "Bearer");
        escrever(req, res, HttpStatus.UNAUTHORIZED,
                "Faça login para acessar este recurso (token ausente, inválido ou expirado).");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res,
                       AccessDeniedException ex) throws IOException {
        escrever(req, res, HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para esta ação.");
    }

    private void escrever(HttpServletRequest req, HttpServletResponse res,
                          HttpStatus status, String detalhe) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setInstance(URI.create(req.getRequestURI()));
        res.setStatus(status.value());
        res.setContentType("application/problem+json;charset=UTF-8");
        mapper.writeValue(res.getOutputStream(), problema);
    }
}
