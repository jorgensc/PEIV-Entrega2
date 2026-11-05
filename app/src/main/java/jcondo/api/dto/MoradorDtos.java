package jcondo.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jcondo.entity.Morador;

public final class MoradorDtos {

    private MoradorDtos() {
    }

    public record MoradorRequest(
            @Schema(example = "Fernanda Ribeiro")
            @NotBlank(message = "O nome é obrigatório") @Size(max = 100) String nome,
            @Schema(example = "306")
            @NotBlank(message = "O apartamento é obrigatório") @Size(max = 10) String apartamento,
            @Schema(example = "C")
            @NotBlank(message = "O bloco é obrigatório") @Size(max = 10) String bloco,
            @Schema(example = "678.901.234-56")
            @NotBlank(message = "O CPF é obrigatório")
            @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", message = "CPF inválido. Use o formato 000.000.000-00")
            String cpf,
            @Schema(example = "(49) 99966-6666")
            @NotBlank(message = "O telefone é obrigatório") @Size(max = 20) String telefone,
            @Schema(example = "fernanda.ribeiro@email.com")
            @NotBlank(message = "O e-mail é obrigatório") @Email(message = "Informe um e-mail válido") @Size(max = 100)
            String email,
            @Schema(description = "Senha de acesso ao app. Obrigatória no cadastro; na edição só troca se for enviada.",
                    example = "morador123")
            String senha) {

        public Morador paraEntidade() {
            return new Morador(nome.trim(), apartamento.trim(), bloco.trim(), cpf.trim(), telefone.trim(),
                    email.trim().toLowerCase());
        }
    }

    public record MoradorResponse(Long id, String nome, String apartamento, String bloco,
                                  String cpf, String telefone, String email) {

        public static MoradorResponse de(Morador m) {
            return new MoradorResponse(m.getId(), m.getNome(), m.getApartamento(), m.getBloco(),
                    m.getCpf(), m.getTelefone(), m.getEmail());
        }
    }
}
