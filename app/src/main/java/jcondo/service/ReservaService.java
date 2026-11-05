package jcondo.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jcondo.entity.AreaComum;
import jcondo.entity.Morador;
import jcondo.entity.Reserva;
import jcondo.entity.StatusReserva;
import jcondo.exception.ConflitoException;
import jcondo.exception.NaoEncontradoException;
import jcondo.exception.RegraNegocioException;
import jcondo.repository.AreaComumRepository;
import jcondo.repository.MoradorRepository;
import jcondo.repository.ReservaRepository;
import jcondo.security.UsuarioLogado;

@Service
public class ReservaService {

    static final int ANTECEDENCIA_MAXIMA_DIAS = 90;
    static final int DURACAO_MINIMA_MINUTOS = 30;

    private final ReservaRepository repository;
    private final AreaComumRepository areas;
    private final MoradorRepository moradores;
    private final Clock relogio;

    public ReservaService(ReservaRepository repository, AreaComumRepository areas,
                          MoradorRepository moradores, Clock relogio) {
        this.repository = repository;
        this.areas = areas;
        this.moradores = moradores;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<Reserva> listar(LocalDate data) {
        return data == null
                ? repository.findAllByOrderByDataReservaDescHoraInicioDesc()
                : repository.findByDataReservaOrderByHoraInicioAsc(data);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarDoMorador(Long moradorId) {
        return repository.findByMoradorIdOrderByDataReservaDescHoraInicioDesc(moradorId);
    }

    @Transactional(readOnly = true)
    public List<Reserva> proximas() {
        return repository.findByDataReservaGreaterThanEqualAndStatusOrderByDataReservaAscHoraInicioAsc(
                hoje(), StatusReserva.CONFIRMADA);
    }

    @Transactional(readOnly = true)
    public List<Reserva> proximasDoMorador(Long moradorId) {
        return repository.findByMoradorIdAndDataReservaGreaterThanEqualAndStatusOrderByDataReservaAscHoraInicioAsc(
                moradorId, hoje(), StatusReserva.CONFIRMADA);
    }

    @Transactional(readOnly = true)
    public long contarFuturas() {
        return repository.countByDataReservaGreaterThanEqualAndStatus(hoje(), StatusReserva.CONFIRMADA);
    }

    @Transactional(readOnly = true)
    public long contarFuturasDoMorador(Long moradorId) {
        return repository.countByMoradorIdAndDataReservaGreaterThanEqualAndStatus(
                moradorId, hoje(), StatusReserva.CONFIRMADA);
    }

    // horarios ja ocupados da area no dia (o app usa pra montar a grade)
    @Transactional(readOnly = true)
    public List<Reserva> ocupadas(Long areaId, LocalDate data) {
        return repository.findByAreaIdAndDataReservaAndStatusOrderByHoraInicioAsc(
                areaId, data, StatusReserva.CONFIRMADA);
    }

    @Transactional
    public Reserva criar(Long areaId, Long moradorId, LocalDate data,
                         LocalTime inicio, LocalTime fim, String observacao) {
        AreaComum area = areas.buscarComBloqueio(areaId)
                .orElseThrow(() -> new NaoEncontradoException("Área comum não encontrada com o ID: " + areaId));
        Morador morador = moradores.findById(moradorId)
                .orElseThrow(() -> new NaoEncontradoException("Morador não encontrado com o ID: " + moradorId));

        validarHorario(area, data, inicio, fim);

        if (repository.existeConflito(areaId, data, inicio, fim)) {
            throw new ConflitoException("Horário indisponível: já existe reserva para "
                    + area.getNome() + " nesse período");
        }

        Reserva reserva = new Reserva();
        reserva.setArea(area);
        reserva.setMorador(morador);
        reserva.setDataReserva(data);
        reserva.setHoraInicio(inicio);
        reserva.setHoraFim(fim);
        reserva.setObservacao(observacao == null || observacao.isBlank() ? null : observacao.trim());
        reserva.setStatus(StatusReserva.CONFIRMADA);
        reserva.setCriadaEm(LocalDateTime.now(relogio));
        return repository.save(reserva);
    }

    void validarHorario(AreaComum area, LocalDate data, LocalTime inicio, LocalTime fim) {
        if (!area.isAtiva()) {
            throw new RegraNegocioException("A área " + area.getNome() + " está indisponível para reservas");
        }
        if (!fim.isAfter(inicio)) {
            throw new RegraNegocioException("O horário final deve ser depois do horário inicial");
        }
        if (Duration.between(inicio, fim).toMinutes() < DURACAO_MINIMA_MINUTOS) {
            throw new RegraNegocioException("A reserva deve ter no mínimo " + DURACAO_MINIMA_MINUTOS + " minutos");
        }
        LocalDate hoje = hoje();
        if (data.isBefore(hoje)) {
            throw new RegraNegocioException("Não é possível reservar uma data que já passou");
        }
        if (data.isEqual(hoje) && inicio.isBefore(LocalTime.now(relogio))) {
            throw new RegraNegocioException("Esse horário de hoje já passou");
        }
        if (data.isAfter(hoje.plusDays(ANTECEDENCIA_MAXIMA_DIAS))) {
            throw new RegraNegocioException("Reservas podem ser feitas com até "
                    + ANTECEDENCIA_MAXIMA_DIAS + " dias de antecedência");
        }
        if (inicio.isBefore(area.getHoraAbertura()) || fim.isAfter(area.getHoraFechamento())) {
            throw new RegraNegocioException("A área " + area.getNome() + " funciona das "
                    + area.getHoraAbertura() + " às " + area.getHoraFechamento());
        }
    }

    // morador so cancela a propria reserva, sindico cancela qualquer uma
    @Transactional
    public Reserva cancelar(Long id, UsuarioLogado quem) {
        Reserva reserva = repository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Reserva não encontrada com o ID: " + id));
        if (!quem.isSindico() && !reserva.getMorador().getId().equals(quem.moradorId())) {
            throw new AccessDeniedException("Você só pode cancelar as suas reservas");
        }
        if (reserva.getStatus() == StatusReserva.CANCELADA) {
            throw new RegraNegocioException("Esta reserva já foi cancelada");
        }
        if (reserva.getDataReserva().isBefore(hoje())) {
            throw new RegraNegocioException("Não é possível cancelar uma reserva que já aconteceu");
        }
        reserva.setStatus(StatusReserva.CANCELADA);
        return repository.save(reserva);
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }
}
