package jcondo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import jcondo.entity.Morador;
import jcondo.service.MoradorService;

// API REST - essa e a parte que aparece documentada no Swagger
@RestController
@RequestMapping("/api/moradores")
@Tag(name = "Moradores", description = "API para gerenciamento de moradores do condomínio")
public class MoradorRestController {

    private final MoradorService service;

    public MoradorRestController(MoradorService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar moradores")
    public List<Morador> listar() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar morador por ID")
    public ResponseEntity<Morador> buscar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Cadastrar morador")
    public ResponseEntity<Morador> criar(@Valid @RequestBody Morador morador) {
        // forca id null pra nao deixar o POST sobrescrever um registro existente
        morador.setId(null);
        Morador salvo = service.salvar(morador);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar morador")
    public ResponseEntity<Morador> atualizar(@PathVariable Long id,
                                             @Valid @RequestBody Morador morador) {
        try {
            service.buscarPorId(id); // confere se existe antes de atualizar
            morador.setId(id);
            return ResponseEntity.ok(service.salvar(morador));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir morador")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        try {
            service.excluir(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
