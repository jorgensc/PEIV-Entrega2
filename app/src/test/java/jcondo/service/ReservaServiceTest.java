package jcondo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import jcondo.entity.AreaComum;
import jcondo.entity.Morador;
import jcondo.entity.Perfil;
import jcondo.entity.Reserva;
import jcondo.entity.StatusReserva;
import jcondo.exception.ConflitoException;
import jcondo.exception.RegraNegocioException;
import jcondo.repository.AreaComumRepository;
import jcondo.repository.MoradorRepository;
import jcondo.repository.ReservaRepository;
import jcondo.security.UsuarioLogado;

// regras da reserva com o relogio fixo em 10/11/2026 as 10h
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);

    @Mock
    private ReservaRepository repository;
    @Mock
    private AreaComumRepository areas;
    @Mock
    private MoradorRepository moradores;

    private ReservaService service;
    private AreaComum churrasqueira;
    private Morador ana;

    @BeforeEach
    void setUp() {
        Clock relogio = Clock.fixed(LocalDateTime.of(HOJE, LocalTime.of(10, 0)).atZone(FUSO).toInstant(), FUSO);
        service = new ReservaService(repository, areas, moradores, relogio);

        churrasqueira = new AreaComum("Churrasqueira", null, 25, LocalTime.of(10, 0), LocalTime.of(22, 0));
        churrasqueira.setId(1L);
        ana = new Morador("Ana", "101", "A", "123.456.789-01", "(49) 9", "ana@email.com");
        ana.setId(7L);
    }

    private void areaEMoradorExistem() {
        when(areas.buscarComBloqueio(1L)).thenReturn(Optional.of(churrasqueira));
        when(moradores.findById(7L)).thenReturn(Optional.of(ana));
    }

    @Test
    void criaReservaQuandoHorarioEstaLivre() {
        areaEMoradorExistem();
        when(repository.existeConflito(any(), any(), any(), any())).thenReturn(false);
        when(repository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        Reserva r = service.criar(1L, 7L, HOJE.plusDays(2), LocalTime.of(12, 0), LocalTime.of(16, 0), " festa ");

        assertEquals(StatusReserva.CONFIRMADA, r.getStatus());
        assertEquals("festa", r.getObservacao());
    }

    @Test
    void bloqueiaHorarioQueJaTemReserva() {
        areaEMoradorExistem();
        when(repository.existeConflito(1L, HOJE.plusDays(2), LocalTime.of(14, 0), LocalTime.of(18, 0)))
                .thenReturn(true);

        ConflitoException ex = assertThrows(ConflitoException.class, () ->
                service.criar(1L, 7L, HOJE.plusDays(2), LocalTime.of(14, 0), LocalTime.of(18, 0), null));
        assertTrue(ex.getMessage().contains("indisponível"));
    }

    @Test
    void naoReservaDataQueJaPassou() {
        areaEMoradorExistem();
        assertThrows(RegraNegocioException.class, () ->
                service.criar(1L, 7L, HOJE.minusDays(1), LocalTime.of(12, 0), LocalTime.of(14, 0), null));
    }

    @Test
    void naoReservaHorarioDeHojeQueJaPassou() {
        areaEMoradorExistem();
        // relogio marca 10h
        assertThrows(RegraNegocioException.class, () ->
                service.criar(1L, 7L, HOJE, LocalTime.of(9, 0), LocalTime.of(11, 0), null));
    }

    @Test
    void naoReservaForaDoHorarioDaArea() {
        areaEMoradorExistem();
        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () ->
                service.criar(1L, 7L, HOJE.plusDays(1), LocalTime.of(20, 0), LocalTime.of(23, 0), null));
        assertTrue(ex.getMessage().contains("funciona das 10:00 às 22:00"));
    }

    @Test
    void horaFinalPrecisaSerDepoisDaInicial() {
        areaEMoradorExistem();
        assertThrows(RegraNegocioException.class, () ->
                service.criar(1L, 7L, HOJE.plusDays(1), LocalTime.of(15, 0), LocalTime.of(14, 0), null));
    }

    @Test
    void naoReservaComMaisDe90DiasDeAntecedencia() {
        areaEMoradorExistem();
        assertThrows(RegraNegocioException.class, () ->
                service.criar(1L, 7L, HOJE.plusDays(91), LocalTime.of(12, 0), LocalTime.of(14, 0), null));
    }

    @Test
    void naoReservaAreaDesativada() {
        churrasqueira.setAtiva(false);
        areaEMoradorExistem();
        assertThrows(RegraNegocioException.class, () ->
                service.criar(1L, 7L, HOJE.plusDays(1), LocalTime.of(12, 0), LocalTime.of(14, 0), null));
    }

    @Test
    void moradorNaoCancelaReservaDeOutro() {
        Reserva r = new Reserva();
        r.setMorador(ana);
        r.setDataReserva(HOJE.plusDays(1));
        when(repository.findById(3L)).thenReturn(Optional.of(r));
        UsuarioLogado outro = new UsuarioLogado(9L, "Bruno", "bruno@email.com", Perfil.MORADOR, 8L);

        assertThrows(AccessDeniedException.class, () -> service.cancelar(3L, outro));
    }

    @Test
    void sindicoCancelaQualquerReserva() {
        Reserva r = new Reserva();
        r.setMorador(ana);
        r.setDataReserva(HOJE.plusDays(1));
        when(repository.findById(3L)).thenReturn(Optional.of(r));
        when(repository.save(r)).thenReturn(r);
        UsuarioLogado sindico = new UsuarioLogado(1L, "Carlos", "sindico@jcondo.com", Perfil.SINDICO, null);

        assertEquals(StatusReserva.CANCELADA, service.cancelar(3L, sindico).getStatus());
    }
}
