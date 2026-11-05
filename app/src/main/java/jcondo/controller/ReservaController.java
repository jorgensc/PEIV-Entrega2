package jcondo.controller;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jcondo.exception.ConflitoException;
import jcondo.exception.NaoEncontradoException;
import jcondo.exception.RegraNegocioException;
import jcondo.security.UsuarioLogado;
import jcondo.service.AreaComumService;
import jcondo.service.MoradorService;
import jcondo.service.ReservaService;

@Controller
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService service;
    private final AreaComumService areas;
    private final MoradorService moradores;

    public ReservaController(ReservaService service, AreaComumService areas, MoradorService moradores) {
        this.service = service;
        this.areas = areas;
        this.moradores = moradores;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                         Model model) {
        model.addAttribute("reservas", service.listar(data));
        model.addAttribute("data", data);
        model.addAttribute("hoje", service.hoje());
        return "reservas/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("areas", areas.listarAtivas());
        model.addAttribute("moradores", moradores.listarTodos());
        model.addAttribute("form", new FormReserva(null, null, service.hoje().plusDays(1),
                LocalTime.of(14, 0), LocalTime.of(18, 0), null));
        model.addAttribute("hoje", service.hoje());
        return "reservas/form";
    }

    // sindico reservando em nome de um morador (ex: pedido feito na portaria)
    @PostMapping("/salvar")
    public String salvar(@RequestParam Long areaId, @RequestParam Long moradorId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                         @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaInicio,
                         @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaFim,
                         @RequestParam(required = false) String observacao,
                         Model model, RedirectAttributes redirect) {
        try {
            service.criar(areaId, moradorId, data, horaInicio, horaFim, observacao);
        } catch (ConflitoException | RegraNegocioException | NaoEncontradoException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("areas", areas.listarAtivas());
            model.addAttribute("moradores", moradores.listarTodos());
            model.addAttribute("form", new FormReserva(areaId, moradorId, data, horaInicio, horaFim, observacao));
            model.addAttribute("hoje", service.hoje());
            return "reservas/form";
        }
        redirect.addFlashAttribute("sucesso", "Reserva confirmada!");
        return "redirect:/reservas?data=" + data;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, UsuarioLogado quem, RedirectAttributes redirect) {
        try {
            service.cancelar(id, quem);
            redirect.addFlashAttribute("sucesso", "Reserva cancelada. O horário ficou livre novamente.");
        } catch (NaoEncontradoException | RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/reservas";
    }

    // devolve o que foi digitado quando da erro
    public record FormReserva(Long areaId, Long moradorId, LocalDate data,
                              LocalTime horaInicio, LocalTime horaFim, String observacao) {
    }
}
