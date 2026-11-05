package jcondo.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jcondo.entity.CategoriaOcorrencia;
import jcondo.entity.Morador;
import jcondo.entity.Ocorrencia;
import jcondo.entity.StatusOcorrencia;
import jcondo.exception.NaoEncontradoException;
import jcondo.repository.MoradorRepository;
import jcondo.repository.OcorrenciaRepository;
import jcondo.security.UsuarioLogado;

@Service
public class OcorrenciaService {

    private final OcorrenciaRepository repository;
    private final MoradorRepository moradores;
    private final Clock relogio;

    public OcorrenciaService(OcorrenciaRepository repository, MoradorRepository moradores, Clock relogio) {
        this.repository = repository;
        this.moradores = moradores;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<Ocorrencia> listar(StatusOcorrencia status) {
        return status == null
                ? repository.findAllByOrderByAbertaEmDesc()
                : repository.findByStatusOrderByAbertaEmDesc(status);
    }

    @Transactional(readOnly = true)
    public List<Ocorrencia> listarDoMorador(Long moradorId) {
        return repository.findByMoradorIdOrderByAbertaEmDesc(moradorId);
    }

    @Transactional(readOnly = true)
    public long contar(StatusOcorrencia status) {
        return repository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long contarEmAbertoDoMorador(Long moradorId) {
        return repository.countByMoradorIdAndStatusNot(moradorId, StatusOcorrencia.RESOLVIDA);
    }

    @Transactional(readOnly = true)
    public Ocorrencia buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Ocorrência não encontrada com o ID: " + id));
    }

    // morador so enxerga as dele
    @Transactional(readOnly = true)
    public Ocorrencia buscar(Long id, UsuarioLogado quem) {
        Ocorrencia ocorrencia = buscar(id);
        if (!quem.isSindico() && !ocorrencia.getMorador().getId().equals(quem.moradorId())) {
            throw new AccessDeniedException("Ocorrência de outro morador");
        }
        return ocorrencia;
    }

    @Transactional
    public Ocorrencia abrir(Long moradorId, String titulo, String descricao, CategoriaOcorrencia categoria) {
        Morador morador = moradores.findById(moradorId)
                .orElseThrow(() -> new NaoEncontradoException("Morador não encontrado com o ID: " + moradorId));
        LocalDateTime agora = LocalDateTime.now(relogio);
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setMorador(morador);
        ocorrencia.setTitulo(titulo.trim());
        ocorrencia.setDescricao(descricao.trim());
        ocorrencia.setCategoria(categoria);
        ocorrencia.setStatus(StatusOcorrencia.ABERTA);
        ocorrencia.setAbertaEm(agora);
        ocorrencia.setAtualizadaEm(agora);
        return repository.save(ocorrencia);
    }

    @Transactional
    public Ocorrencia atualizar(Long id, StatusOcorrencia status, String resposta) {
        Ocorrencia ocorrencia = buscar(id);
        ocorrencia.setStatus(status);
        if (resposta != null && !resposta.isBlank()) {
            ocorrencia.setResposta(resposta.trim());
        }
        ocorrencia.setAtualizadaEm(LocalDateTime.now(relogio));
        return repository.save(ocorrencia);
    }
}
