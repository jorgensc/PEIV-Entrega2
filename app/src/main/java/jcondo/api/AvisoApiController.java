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
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jcondo.api.dto.AvisoDtos.AvisoRequest;
import jcondo.api.dto.AvisoDtos.AvisoResponse;
import jcondo.entity.Aviso;
import jcondo.security.UsuarioLogado;
import jcondo.service.AvisoService;

@RestController
@RequestMapping("/api/v1/avisos")
@Tag(name = "3. Avisos", description = "Mural de avisos (leitura para todos, publicação pelo síndico)")
public class AvisoApiController {

    private final AvisoService service;

    public AvisoApiController(AvisoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar avisos (importantes primeiro)")
    public List<AvisoResponse> listar() {
        return service.listar().stream().map(AvisoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar aviso por ID")
    public AvisoResponse buscar(@PathVariable Long id) {
        return AvisoResponse.de(service.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SINDICO')")
    @Operation(summary = "Publicar aviso (síndico)")
    public ResponseEntity<AvisoResponse> publicar(UsuarioLogado quem, @Valid @RequestBody AvisoRequest req) {
        Aviso salvo = service.salvar(req.paraEntidade(), quem.id());
        return ResponseEntity.created(URI.create("/api/v1/avisos/" + salvo.getId()))
                .body(AvisoResponse.de(salvo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SINDICO')")
    @Operation(summary = "Editar aviso (síndico)")
    public AvisoResponse atualizar(@PathVariable Long id, @Valid @RequestBody AvisoRequest req) {
        Aviso dados = req.paraEntidade();
        dados.setId(id);
        return AvisoResponse.de(service.salvar(dados, null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SINDICO')")
    @Operation(summary = "Excluir aviso (síndico)")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
