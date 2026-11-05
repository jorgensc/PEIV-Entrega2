package jcondo.security;

import org.springframework.security.oauth2.jwt.Jwt;

import jcondo.entity.Perfil;
import jcondo.entity.Usuario;

// quem esta fazendo a requisicao, tanto pela tela quanto pela API
public record UsuarioLogado(Long id, String nome, String email, Perfil perfil, Long moradorId) {

    public boolean isSindico() {
        return perfil == Perfil.SINDICO;
    }

    public static UsuarioLogado de(Usuario u) {
        return new UsuarioLogado(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(),
                u.getMorador() == null ? null : u.getMorador().getId());
    }

    public static UsuarioLogado de(Jwt jwt) {
        Number morador = jwt.getClaim("moradorId");
        Number uid = jwt.getClaim("uid");
        return new UsuarioLogado(uid.longValue(), jwt.getClaimAsString("nome"), jwt.getSubject(),
                Perfil.valueOf(jwt.getClaimAsString("perfil")),
                morador == null ? null : morador.longValue());
    }
}
