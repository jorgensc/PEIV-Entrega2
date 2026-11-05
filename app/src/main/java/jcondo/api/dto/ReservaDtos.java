package jcondo.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jcondo.entity.AreaComum;
import jcondo.entity.Reserva;
import jcondo.entity.StatusReserva;

public final class ReservaDtos {

    private ReservaDtos() {
    }

    public record ReservaRequest(
            @Schema(example = "1")
            @NotNull(message = "Escolha a área") Long areaId,
            @Schema(description = "Só o síndico informa. Para o morador é sempre ele mesmo.")
            Long moradorId,
            @Schema(example = "2026-12-12")
            @NotNull(message = "Informe a data") LocalDate data,
            @Schema(type = "string", example = "14:00")
            @NotNull(message = "Informe o horário inicial") LocalTime horaInicio,
            @Schema(type = "string", example = "18:00")
            @NotNull(message = "Informe o horário final") LocalTime horaFim,
            @Schema(example = "Aniversário, cerca de 30 convidados")
            @Size(max = 255) String observacao) {
    }

    public record ReservaResponse(Long id, Long areaId, String area, Long moradorId, String morador,
                                  String apartamento, String bloco, LocalDate data,
                                  @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
                                  @JsonFormat(pattern = "HH:mm") LocalTime horaFim,
                                  String observacao, StatusReserva status, LocalDateTime criadaEm) {

        public static ReservaResponse de(Reserva r) {
            return new ReservaResponse(r.getId(), r.getArea().getId(), r.getArea().getNome(),
                    r.getMorador().getId(), r.getMorador().getNome(), r.getMorador().getApartamento(),
                    r.getMorador().getBloco(), r.getDataReserva(), r.getHoraInicio(), r.getHoraFim(),
                    r.getObservacao(), r.getStatus(), r.getCriadaEm());
        }
    }

    public record Periodo(@JsonFormat(pattern = "HH:mm") LocalTime inicio,
                          @JsonFormat(pattern = "HH:mm") LocalTime fim) {
    }

    // nao mostra quem reservou, so os horarios ocupados
    public record DisponibilidadeResponse(Long areaId, String area, LocalDate data,
                                          @JsonFormat(pattern = "HH:mm") LocalTime horaAbertura,
                                          @JsonFormat(pattern = "HH:mm") LocalTime horaFechamento,
                                          List<Periodo> ocupados) {

        public static DisponibilidadeResponse de(AreaComum a, LocalDate data, List<Reserva> reservas) {
            return new DisponibilidadeResponse(a.getId(), a.getNome(), data, a.getHoraAbertura(),
                    a.getHoraFechamento(),
                    reservas.stream().map(r -> new Periodo(r.getHoraInicio(), r.getHoraFim())).toList());
        }
    }
}
