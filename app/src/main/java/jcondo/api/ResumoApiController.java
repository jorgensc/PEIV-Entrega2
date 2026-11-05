package jcondo.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jcondo.api.dto.AvisoDtos.AvisoResponse;
import jcondo.api.dto.ReservaDtos.ReservaResponse;
import jcondo.api.dto.ResumoResponse;
import jcondo.entity.StatusOcorrencia;
import jcondo.security.UsuarioLogado;
import jcondo.service.AvisoService;
import jcondo.service.MoradorService;
import jcondo.service.OcorrenciaService;
import jcondo.service.ReservaService;

@RestController
@RequestMapping("/api/v1/resumo")
@Tag(name = "7. Resumo", description = "Números da tela inicial do app")
public class ResumoApiController {

    private final AvisoService avisos;
    private final ReservaService reservas;
    private final OcorrenciaService ocorrencias;
    private final MoradorService moradores;

    public ResumoApiController(AvisoService avisos, ReservaService reservas,
                               OcorrenciaService ocorrencias, MoradorService moradores) {
        this.avisos = avisos;
        this.reservas = reservas;
        this.ocorrencias = ocorrencias;
        this.moradores = moradores;
    }

    @GetMapping
    @Operation(summary = "Resumo do condomínio para o usuário logado")
    public ResumoResponse resumo(UsuarioLogado quem) {
        List<AvisoResponse> ultimos = avisos.ultimos().stream().map(AvisoResponse::de).toList();

        if (quem.isSindico()) {
            List<ReservaResponse> proximas = reservas.proximas().stream()
                    .limit(5).map(ReservaResponse::de).toList();
            long emAberto = ocorrencias.contar(StatusOcorrencia.ABERTA)
                    + ocorrencias.contar(StatusOcorrencia.EM_ANDAMENTO);
            return new ResumoResponse(quem.perfil(), avisos.contar(), reservas.contarFuturas(),
                    emAberto, moradores.contar(), ultimos, proximas);
        }

        List<ReservaResponse> proximas = reservas.proximasDoMorador(quem.moradorId()).stream()
                .limit(3).map(ReservaResponse::de).toList();
        return new ResumoResponse(quem.perfil(), avisos.contar(),
                reservas.contarFuturasDoMorador(quem.moradorId()),
                ocorrencias.contarEmAbertoDoMorador(quem.moradorId()), null, ultimos, proximas);
    }
}
