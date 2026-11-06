package jcondo.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jcondo.api.dto.AuthDtos.LoginRequest;
import jcondo.api.dto.AuthDtos.LoginResponse;
import jcondo.api.dto.AuthDtos.UsuarioResponse;
import jcondo.entity.Usuario;
import jcondo.security.TokenService;
import jcondo.security.UsuarioLogado;
import jcondo.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "1. Autenticação", description = "Login e dados do usuário logado")
public class AuthApiController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthApiController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Entrar e receber o token JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        Usuario usuario = authService.autenticar(req.email(), req.senha());
        TokenService.Token token = tokenService.gerar(usuario);
        return new LoginResponse(token.valor(), "Bearer", token.expiraEm(), UsuarioResponse.de(usuario));
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário logado")
    public UsuarioResponse me(UsuarioLogado quem) {
        return UsuarioResponse.de(authService.buscarPorId(quem.id()));
    }
}
