package jcondo.api.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jcondo.entity.Aviso;
import jcondo.entity.CategoriaAviso;

public final class AvisoDtos {

    private AvisoDtos() {
    }

    public record AvisoRequest(
            @Schema(example = "Limpeza da caixa d'água")
            @NotBlank(message = "O título é obrigatório") @Size(max = 120) String titulo,
            @Schema(example = "Na quinta-feira, das 8h às 12h, o abastecimento de água será interrompido.")
            @NotBlank(message = "O conteúdo é obrigatório") @Size(max = 2000) String conteudo,
            @NotNull(message = "Escolha uma categoria") CategoriaAviso categoria,
            boolean importante) {

        public Aviso paraEntidade() {
            Aviso a = new Aviso();
            a.setTitulo(titulo);
            a.setConteudo(conteudo);
            a.setCategoria(categoria);
            a.setImportante(importante);
            return a;
        }
    }

    public record AvisoResponse(Long id, String titulo, String conteudo, CategoriaAviso categoria,
                                String categoriaDescricao, boolean importante,
                                LocalDateTime publicadoEm, String autor) {

        public static AvisoResponse de(Aviso a) {
            return new AvisoResponse(a.getId(), a.getTitulo(), a.getConteudo(), a.getCategoria(),
                    a.getCategoria().getDescricao(), a.isImportante(), a.getPublicadoEm(),
                    a.getAutor() == null ? "Administração" : a.getAutor().getNome());
        }
    }
}
