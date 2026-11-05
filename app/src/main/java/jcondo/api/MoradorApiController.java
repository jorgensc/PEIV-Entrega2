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
import jcondo.api.dto.MoradorDtos.MoradorRequest;
import jcondo.api.dto.MoradorDtos.MoradorResponse;
import jcondo.entity.Morador;
import jcondo.service.MoradorService;

@RestController
@RequestMapping("/api/v1/moradores")
@PreAuthorize("hasRole('SINDICO')")
@Tag(name = "2. Moradores", description = "Cadastro de moradores (somente síndico)")
public class MoradorApiController {

    private final MoradorService service;

    public MoradorApiController(MoradorService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar moradores")
    public List<MoradorResponse> listar() {
        return service.listarTodos().stream().map(MoradorResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar morador por ID")
    public MoradorResponse buscar(@PathVariable Long id) {
        return MoradorResponse.de(service.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar morador e criar o acesso dele ao app")
    public ResponseEntity<MoradorResponse> criar(@Valid @RequestBody MoradorRequest req) {
        Morador salvo = service.salvar(req.paraEntidade(), req.senha());
        return ResponseEntity.created(URI.create("/api/v1/moradores/" + salvo.getId()))
                .body(MoradorResponse.de(salvo));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar morador")
    public MoradorResponse atualizar(@PathVariable Long id, @Valid @RequestBody MoradorRequest req) {
        service.buscarPorId(id); // 404 se nao existir
        Morador morador = req.paraEntidade();
        morador.setId(id);
        return MoradorResponse.de(service.salvar(morador, req.senha()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir morador (remove também reservas, ocorrências e acesso)")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
