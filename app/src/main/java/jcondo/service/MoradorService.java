package jcondo.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jcondo.entity.Morador;
import jcondo.entity.Perfil;
import jcondo.entity.Usuario;
import jcondo.exception.ConflitoException;
import jcondo.exception.NaoEncontradoException;
import jcondo.exception.RegraNegocioException;
import jcondo.repository.MoradorRepository;
import jcondo.repository.OcorrenciaRepository;
import jcondo.repository.ReservaRepository;
import jcondo.repository.UsuarioRepository;

// Camada de servico - o controller nunca fala direto com o repository
@Service
public class MoradorService {

    private final MoradorRepository repository;
    private final UsuarioRepository usuarios;
    private final ReservaRepository reservas;
    private final OcorrenciaRepository ocorrencias;
    private final PasswordEncoder encoder;

    public MoradorService(MoradorRepository repository, UsuarioRepository usuarios,
                          ReservaRepository reservas, OcorrenciaRepository ocorrencias,
                          PasswordEncoder encoder) {
        this.repository = repository;
        this.usuarios = usuarios;
        this.reservas = reservas;
        this.ocorrencias = ocorrencias;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<Morador> listarTodos() {
        return repository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public long contar() {
        return repository.count();
    }

    @Transactional(readOnly = true)
    public Morador buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException(
                        "Morador não encontrado com o ID: " + id));
    }

    // salva o morador e cria/atualiza o acesso dele ao app
    // senhaAcesso: obrigatoria no cadastro, opcional na edicao (so troca se vier preenchida)
    @Transactional
    public Morador salvar(Morador morador, String senhaAcesso) {
        boolean novo = morador.getId() == null;
        Usuario usuario = novo ? null : usuarios.findByMoradorId(morador.getId()).orElse(null);

        usuarios.findByEmailIgnoreCase(morador.getEmail()).ifPresent(outro -> {
            if (usuario == null || !outro.getId().equals(usuario.getId())) {
                throw new ConflitoException("Já existe um usuário com o e-mail " + morador.getEmail());
            }
        });

        boolean temSenha = senhaAcesso != null && !senhaAcesso.isBlank();
        if (temSenha && senhaAcesso.length() < 6) {
            throw new RegraNegocioException("A senha de acesso precisa ter pelo menos 6 caracteres");
        }
        if (novo && !temSenha) {
            throw new RegraNegocioException("Informe uma senha de acesso ao app para o novo morador");
        }

        Morador salvo = repository.save(morador);

        Usuario acesso = usuario;
        if (acesso == null && temSenha) {
            acesso = new Usuario();
            acesso.setPerfil(Perfil.MORADOR);
            acesso.setMorador(salvo);
        }
        if (acesso != null) {
            acesso.setNome(salvo.getNome());
            acesso.setEmail(salvo.getEmail());
            if (temSenha) {
                acesso.setSenha(encoder.encode(senhaAcesso));
            }
            usuarios.save(acesso);
        }
        return salvo;
    }

    // remove junto as reservas, ocorrencias e o acesso dele
    @Transactional
    public void excluir(Long id) {
        Morador morador = buscarPorId(id);
        reservas.excluirDoMorador(id);
        ocorrencias.excluirDoMorador(id);
        usuarios.findByMoradorId(id).ifPresent(usuarios::delete);
        repository.delete(morador);
    }
}
