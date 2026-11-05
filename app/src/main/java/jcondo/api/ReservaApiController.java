package jcondo.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jcondo.api.dto.ReservaDtos.DisponibilidadeResponse;
import jcondo.api.dto.ReservaDtos.ReservaRequest;
import jcondo.api.dto.ReservaDtos.ReservaResponse;
import jcondo.entity.Reserva;
import jcondo.exception.RegraNegocioException;
import jcondo.security.UsuarioLogado;
import jcondo.service.AreaComumService;
import jcondo.service.ReservaService;

@RestController
@RequestMapping("/api/v1/reservas")
@Tag(name = "5. Reservas", description = "Reserva de áreas comuns com bloqueio de conflito de horário")
public class ReservaApiController {

    private final ReservaService service;
    private final AreaComumService areas;

    public ReservaApiController(ReservaService service, AreaComumService areas) {
        this.service = service;
        this.areas = areas;
    }

    @GetMapping
    @Operation(summary = "Listar reservas",
            description = "Síndico vê todas (filtro opcional por data). Morador vê só as dele.")
    public List<ReservaResponse> listar(UsuarioLogado quem,
            @Parameter(example = "2026-12-12")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        List<Reserva> reservas = quem.isSindico()
                ? service.listar(data)
                : service.listarDoMorador(quem.moradorId());
        return reservas.stream().map(ReservaResponse::de).toList();
    }

    @GetMapping("/disponibilidade")
    @Operation(summary = "Horários ocupados de uma área num dia")
    public DisponibilidadeResponse disponibilidade(
            @RequestParam Long areaId,
            @Parameter(example = "2026-12-12")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return DisponibilidadeResponse.de(areas.buscar(areaId), data, service.ocupadas(areaId, data));
    }

    @PostMapping
    @Operation(summary = "Criar reserva",
            description = "Retorna 409 se o horário já estiver ocupado e 422 se violar uma regra (data passada, fora do horário da área...).")
    public ResponseEntity<ReservaResponse> criar(UsuarioLogado quem, @Valid @RequestBody ReservaRequest req) {
        Long moradorId = quem.isSindico() ? req.moradorId() : quem.moradorId();
        if (moradorId == null) {
            throw new RegraNegocioException("Informe o morador da reserva");
        }
        Reserva salva = service.criar(req.areaId(), moradorId, req.data(),
                req.horaInicio(), req.horaFim(), req.observacao());
        return ResponseEntity.created(URI.create("/api/v1/reservas/" + salva.getId()))
                .body(ReservaResponse.de(salva));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar reserva (a própria, ou qualquer uma se for síndico)")
    public ReservaResponse cancelar(UsuarioLogado quem, @PathVariable Long id) {
        return ReservaResponse.de(service.cancelar(id, quem));
    }
}
