package jcondo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jcondo.entity.Morador;
import jcondo.repository.MoradorRepository;

// Testes unitarios do service usando Mockito (repository mockado, nao precisa de banco)
@ExtendWith(MockitoExtension.class)
class MoradorServiceTest {

    @Mock
    private MoradorRepository repository;

    @InjectMocks
    private MoradorService service;

    private Morador morador;

    @BeforeEach
    void setUp() {
        morador = new Morador("Ana Paula Souza", "101", "A",
                "123.456.789-01", "(49) 99911-1111", "ana.souza@email.com");
        morador.setId(1L);
    }

    @Test
    void deveListarTodosOsMoradores() {
        when(repository.findAllByOrderByNomeAsc()).thenReturn(List.of(morador));

        List<Morador> resultado = service.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals("Ana Paula Souza", resultado.get(0).getNome());
    }

    @Test
    void deveBuscarMoradorPorId() {
        when(repository.findById(1L)).thenReturn(Optional.of(morador));

        Morador resultado = service.buscarPorId(1L);

        assertEquals(1L, resultado.getId());
    }

    @Test
    void deveLancarExcecaoQuandoIdNaoExiste() {
        // buscarPorId de um id que nao existe tem que lancar IllegalArgumentException
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(99L));
    }

    @Test
    void deveSalvarMorador() {
        when(repository.save(any(Morador.class))).thenReturn(morador);

        Morador salvo = service.salvar(morador);

        assertEquals("Ana Paula Souza", salvo.getNome());
        verify(repository, times(1)).save(morador);
    }

    @Test
    void deveExcluirMorador() {
        when(repository.findById(1L)).thenReturn(Optional.of(morador));

        service.excluir(1L);

        verify(repository, times(1)).delete(morador);
    }
}
