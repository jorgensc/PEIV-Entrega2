package jcondo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

// Testes unitarios do service usando Mockito (repository mockado, nao precisa de banco)
@ExtendWith(MockitoExtension.class)
class MoradorServiceTest {

    @Mock
    private MoradorRepository repository;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private ReservaRepository reservas;
    @Mock
    private OcorrenciaRepository ocorrencias;
    @Mock
    private PasswordEncoder encoder;

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

        assertEquals(1L, service.buscarPorId(1L).getId());
    }

    @Test
    void deveLancarExcecaoQuandoIdNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> service.buscarPorId(99L));
    }

    @Test
    void novoMoradorGanhaAcessoAoAppComSenhaCriptografada() {
        Morador novo = new Morador("Fernanda Ribeiro", "306", "C",
                "678.901.234-56", "(49) 99966-6666", "fernanda@email.com");
        when(usuarios.findByEmailIgnoreCase("fernanda@email.com")).thenReturn(Optional.empty());
        when(repository.save(novo)).thenReturn(novo);
        when(encoder.encode("segredo1")).thenReturn("HASH");

        service.salvar(novo, "segredo1");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        assertEquals(Perfil.MORADOR, captor.getValue().getPerfil());
        assertEquals("HASH", captor.getValue().getSenha());
        assertEquals("fernanda@email.com", captor.getValue().getEmail());
    }

    @Test
    void novoMoradorSemSenhaNaoPodeSerSalvo() {
        Morador novo = new Morador("Fernanda Ribeiro", "306", "C",
                "678.901.234-56", "(49) 99966-6666", "fernanda@email.com");
        when(usuarios.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());

        assertThrows(RegraNegocioException.class, () -> service.salvar(novo, ""));
        verify(repository, never()).save(any());
    }

    @Test
    void naoPermiteEmailDeOutroUsuario() {
        Morador novo = new Morador("Outra Pessoa", "307", "C",
                "678.901.234-56", "(49) 99966-6666", "ana.souza@email.com");
        Usuario ana = new Usuario("Ana", "ana.souza@email.com", "x", Perfil.MORADOR);
        ana.setId(5L);
        when(usuarios.findByEmailIgnoreCase("ana.souza@email.com")).thenReturn(Optional.of(ana));

        assertThrows(ConflitoException.class, () -> service.salvar(novo, "segredo1"));
    }

    @Test
    void deveExcluirMoradorComReservasOcorrenciasEAcesso() {
        Usuario acesso = new Usuario("Ana", "ana.souza@email.com", "x", Perfil.MORADOR);
        when(repository.findById(1L)).thenReturn(Optional.of(morador));
        when(usuarios.findByMoradorId(1L)).thenReturn(Optional.of(acesso));

        service.excluir(1L);

        verify(reservas).excluirDoMorador(1L);
        verify(ocorrencias).excluirDoMorador(1L);
        verify(usuarios).delete(acesso);
        verify(repository, times(1)).delete(morador);
    }
}
