package jcondo.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jcondo.api.dto.OcorrenciaDtos.AtualizarStatusRequest;
import jcondo.api.dto.OcorrenciaDtos.OcorrenciaRequest;
import jcondo.api.dto.OcorrenciaDtos.OcorrenciaResponse;
import jcondo.entity.Ocorrencia;
import jcondo.entity.StatusOcorrencia;
import jcondo.exception.RegraNegocioException;
import jcondo.security.UsuarioLogado;
import jcondo.service.OcorrenciaService;

@RestController
@RequestMapping("/api/v1/ocorrencias")
@Tag(name = "6. Ocorrências", description = "Problemas registrados pelos moradores")
public class OcorrenciaApiController {

    private final OcorrenciaService service;

    public OcorrenciaApiController(OcorrenciaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar ocorrências",
            description = "Síndico vê todas (filtro opcional por status). Morador vê só as dele.")
    public List<OcorrenciaResponse> listar(UsuarioLogado quem,
                                           @RequestParam(required = false) StatusOcorrencia status) {
        List<Ocorrencia> lista = quem.isSindico()
                ? service.listar(status)
                : service.listarDoMorador(quem.moradorId());
        return lista.stream().map(OcorrenciaResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da ocorrência")
    public OcorrenciaResponse buscar(UsuarioLogado quem, @PathVariable Long id) {
        return OcorrenciaResponse.de(service.buscar(id, quem));
    }

    @PostMapping
    @Operation(summary = "Abrir ocorrência (morador)")
    public ResponseEntity<OcorrenciaResponse> abrir(UsuarioLogado quem, @Valid @RequestBody OcorrenciaRequest req) {
        if (quem.moradorId() == null) {
            throw new RegraNegocioException("Somente moradores abrem ocorrências");
        }
        Ocorrencia salva = service.abrir(quem.moradorId(), req.titulo(), req.descricao(), req.categoria());
        return ResponseEntity.created(URI.create("/api/v1/ocorrencias/" + salva.getId()))
                .body(OcorrenciaResponse.de(salva));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SINDICO')")
    @Operation(summary = "Atualizar status e responder (síndico)")
    public OcorrenciaResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarStatusRequest req) {
        return OcorrenciaResponse.de(service.atualizar(id, req.status(), req.resposta()));
    }
}
