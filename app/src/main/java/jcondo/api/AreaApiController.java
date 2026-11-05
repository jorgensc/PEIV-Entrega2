package jcondo.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jcondo.api.dto.AreaDtos.AreaRequest;
import jcondo.api.dto.AreaDtos.AreaResponse;
import jcondo.entity.AreaComum;
import jcondo.service.AreaComumService;

@RestController
@RequestMapping("/api/v1/areas")
@Tag(name = "4. Áreas comuns", description = "Espaços que podem ser reservados")
public class AreaApiController {

    private final AreaComumService service;

    public AreaApiController(AreaComumService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar áreas comuns")
    public List<AreaResponse> listar(
            @Parameter(description = "true para incluir as desativadas")
            @RequestParam(defaultValue = "false") boolean todas) {
        List<AreaComum> areas = todas ? service.listarTodas() : service.listarAtivas();
        return areas.stream().map(AreaResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar área por ID")
    public AreaResponse buscar(@PathVariable Long id) {
        return AreaResponse.de(service.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SINDICO')")
    @Operation(summary = "Cadastrar área (síndico)")
    public ResponseEntity<AreaResponse> criar(@Valid @RequestBody AreaRequest req) {
        AreaComum salva = service.salvar(req.paraEntidade());
        return ResponseEntity.created(URI.create("/api/v1/areas/" + salva.getId()))
                .body(AreaResponse.de(salva));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SINDICO')")
    @Operation(summary = "Atualizar área (síndico)")
    public AreaResponse atualizar(@PathVariable Long id, @Valid @RequestBody AreaRequest req) {
        service.buscar(id);
        AreaComum area = req.paraEntidade();
        area.setId(id);
        return AreaResponse.de(service.salvar(area));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SINDICO')")
    @Operation(summary = "Excluir área sem reservas (síndico)")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
