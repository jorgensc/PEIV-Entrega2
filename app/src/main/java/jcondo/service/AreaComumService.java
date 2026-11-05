package jcondo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jcondo.entity.AreaComum;
import jcondo.exception.NaoEncontradoException;
import jcondo.exception.RegraNegocioException;
import jcondo.repository.AreaComumRepository;
import jcondo.repository.ReservaRepository;

@Service
public class AreaComumService {

    private final AreaComumRepository repository;
    private final ReservaRepository reservas;

    public AreaComumService(AreaComumRepository repository, ReservaRepository reservas) {
        this.repository = repository;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public List<AreaComum> listarTodas() {
        return repository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<AreaComum> listarAtivas() {
        return repository.findByAtivaTrueOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public AreaComum buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Área comum não encontrada com o ID: " + id));
    }

    @Transactional
    public AreaComum salvar(AreaComum area) {
        if (!area.getHoraFechamento().isAfter(area.getHoraAbertura())) {
            throw new RegraNegocioException("O horário de fechamento deve ser depois do de abertura");
        }
        return repository.save(area);
    }

    // area com historico de reservas nao some, so e desativada
    @Transactional
    public void excluir(Long id) {
        AreaComum area = buscar(id);
        if (reservas.existsByAreaId(id)) {
            throw new RegraNegocioException(
                    "A área " + area.getNome() + " tem reservas registradas. Desative em vez de excluir.");
        }
        repository.delete(area);
    }
}
