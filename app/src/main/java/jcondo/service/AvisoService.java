package jcondo.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jcondo.entity.Aviso;
import jcondo.exception.NaoEncontradoException;
import jcondo.repository.AvisoRepository;
import jcondo.repository.UsuarioRepository;

@Service
public class AvisoService {

    private final AvisoRepository repository;
    private final UsuarioRepository usuarios;
    private final Clock relogio;

    public AvisoService(AvisoRepository repository, UsuarioRepository usuarios, Clock relogio) {
        this.repository = repository;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<Aviso> listar() {
        return repository.findAllByOrderByImportanteDescPublicadoEmDesc();
    }

    @Transactional(readOnly = true)
    public List<Aviso> ultimos() {
        return repository.findTop3ByOrderByPublicadoEmDesc();
    }

    @Transactional(readOnly = true)
    public long contar() {
        return repository.count();
    }

    @Transactional(readOnly = true)
    public Aviso buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Aviso não encontrado com o ID: " + id));
    }

    // novo aviso: grava data e autor. Edicao: troca so o conteudo
    @Transactional
    public Aviso salvar(Aviso dados, Long autorId) {
        Aviso aviso;
        if (dados.getId() == null) {
            aviso = new Aviso();
            aviso.setPublicadoEm(LocalDateTime.now(relogio));
            aviso.setAutor(autorId == null ? null : usuarios.findById(autorId).orElse(null));
        } else {
            aviso = buscar(dados.getId());
        }
        aviso.setTitulo(dados.getTitulo().trim());
        aviso.setConteudo(dados.getConteudo().trim());
        aviso.setCategoria(dados.getCategoria());
        aviso.setImportante(dados.isImportante());
        return repository.save(aviso);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscar(id));
    }
}
