package jcondo.api.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jcondo.entity.CategoriaOcorrencia;
import jcondo.entity.Ocorrencia;
import jcondo.entity.StatusOcorrencia;

public final class OcorrenciaDtos {

    private OcorrenciaDtos() {
    }

    public record OcorrenciaRequest(
            @Schema(example = "Lâmpada queimada na garagem")
            @NotBlank(message = "O título é obrigatório") @Size(max = 120) String titulo,
            @Schema(example = "A lâmpada perto da vaga 12 do bloco B está queimada desde ontem.")
            @NotBlank(message = "Descreva o problema") @Size(max = 2000) String descricao,
            @NotNull(message = "Escolha uma categoria") CategoriaOcorrencia categoria) {
    }

    public record AtualizarStatusRequest(
            @NotNull(message = "Informe o status") StatusOcorrencia status,
            @Schema(example = "Técnico agendado para amanhã de manhã.")
            @Size(max = 1000) String resposta) {
    }

    public record OcorrenciaResponse(Long id, String titulo, String descricao,
                                     CategoriaOcorrencia categoria, String categoriaDescricao,
                                     StatusOcorrencia status, String statusDescricao, String resposta,
                                     Long moradorId, String morador, String apartamento, String bloco,
                                     LocalDateTime abertaEm, LocalDateTime atualizadaEm) {

        public static OcorrenciaResponse de(Ocorrencia o) {
            return new OcorrenciaResponse(o.getId(), o.getTitulo(), o.getDescricao(), o.getCategoria(),
                    o.getCategoria().getDescricao(), o.getStatus(), o.getStatus().getDescricao(),
                    o.getResposta(), o.getMorador().getId(), o.getMorador().getNome(),
                    o.getMorador().getApartamento(), o.getMorador().getBloco(),
                    o.getAbertaEm(), o.getAtualizadaEm());
        }
    }
}
