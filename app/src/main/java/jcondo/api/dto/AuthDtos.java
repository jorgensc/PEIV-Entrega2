package jcondo.api.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jcondo.entity.Perfil;
import jcondo.entity.Usuario;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @Schema(example = "ana.souza@email.com")
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") String email,
            @Schema(example = "morador123")
            @NotBlank(message = "Informe a senha") String senha) {
    }

    public record LoginResponse(String token, String tipo, Instant expiraEm, UsuarioResponse usuario) {
    }

    public record MoradorDoUsuario(Long id, String apartamento, String bloco, String telefone) {
    }

    public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, MoradorDoUsuario morador) {

        public static UsuarioResponse de(Usuario u) {
            MoradorDoUsuario m = u.getMorador() == null ? null
                    : new MoradorDoUsuario(u.getMorador().getId(), u.getMorador().getApartamento(),
                            u.getMorador().getBloco(), u.getMorador().getTelefone());
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), m);
        }
    }
}
