package jcondo.api.dto;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jcondo.entity.AreaComum;

public final class AreaDtos {

    private AreaDtos() {
    }

    public record AreaRequest(
            @Schema(example = "Piscina")
            @NotBlank(message = "O nome é obrigatório") @Size(max = 60) String nome,
            @Schema(example = "Piscina adulto e infantil")
            @Size(max = 255) String descricao,
            @Schema(example = "30")
            @NotNull(message = "Informe a capacidade") @Min(1) @Max(500) Integer capacidade,
            @Schema(type = "string", example = "08:00")
            @NotNull(message = "Informe o horário de abertura") LocalTime horaAbertura,
            @Schema(type = "string", example = "20:00")
            @NotNull(message = "Informe o horário de fechamento") LocalTime horaFechamento,
            @Schema(example = "true") Boolean ativa) {

        public AreaComum paraEntidade() {
            AreaComum a = new AreaComum(nome.trim(), descricao, capacidade, horaAbertura, horaFechamento);
            a.setAtiva(ativa == null || ativa);
            return a;
        }
    }

    public record AreaResponse(Long id, String nome, String descricao, Integer capacidade,
                               @JsonFormat(pattern = "HH:mm") LocalTime horaAbertura,
                               @JsonFormat(pattern = "HH:mm") LocalTime horaFechamento,
                               boolean ativa) {

        public static AreaResponse de(AreaComum a) {
            return new AreaResponse(a.getId(), a.getNome(), a.getDescricao(), a.getCapacidade(),
                    a.getHoraAbertura(), a.getHoraFechamento(), a.isAtiva());
        }
    }
}
