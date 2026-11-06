package jcondo.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jcondo.entity.Usuario;
import jcondo.exception.NaoEncontradoException;
import jcondo.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    // mesma mensagem pra e-mail e senha errados, pra nao entregar quais e-mails existem
    @Transactional(readOnly = true)
    public Usuario autenticar(String email, String senha) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email.trim())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos"));
        if (!encoder.matches(senha, usuario.getSenha())) {
            throw new BadCredentialsException("E-mail ou senha inválidos");
        }
        return usuario;
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
    }
}
