package jcondo.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import jcondo.entity.Usuario;

// gera o JWT que o app usa no header Authorization
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration validade;
    private final Clock relogio;

    public TokenService(JwtEncoder encoder, Clock relogio,
                        @Value("${jcondo.jwt.validade-horas:12}") long horas) {
        this.encoder = encoder;
        this.relogio = relogio;
        this.validade = Duration.ofHours(horas);
    }

    public Token gerar(Usuario usuario) {
        Instant agora = relogio.instant();
        Instant expira = agora.plus(validade);

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("jcondo")
                .issuedAt(agora)
                .expiresAt(expira)
                .subject(usuario.getEmail())
                .claim("uid", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim("perfil", usuario.getPerfil().name());
        if (usuario.getMorador() != null) {
            claims.claim("moradorId", usuario.getMorador().getId());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new Token(valor, expira);
    }

    public record Token(String valor, Instant expiraEm) {
    }
}
