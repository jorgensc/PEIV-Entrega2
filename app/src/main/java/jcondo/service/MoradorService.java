package jcondo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jcondo.entity.Morador;
import jcondo.repository.MoradorRepository;

// Camada de servico - o controller nunca fala direto com o repository
@Service
public class MoradorService {

    private final MoradorRepository repository;

    // injecao pelo construtor (nao precisa de @Autowired)
    public MoradorService(MoradorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Morador> listarTodos() {
        return repository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public Morador buscarPorId(Long id) {
        // se nao achar lanca excecao, quem chamou trata e mostra msg de erro
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Morador não encontrado com o ID: " + id));
    }

    @Transactional
    public Morador salvar(Morador morador) {
        // o save do JPA ja resolve os dois casos: insert se id null, update se nao
        return repository.save(morador);
    }

    @Transactional
    public void excluir(Long id) {
        Morador morador = buscarPorId(id);
        repository.delete(morador);
    }
}
