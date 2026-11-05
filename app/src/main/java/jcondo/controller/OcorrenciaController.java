package jcondo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jcondo.entity.StatusOcorrencia;
import jcondo.exception.NaoEncontradoException;
import jcondo.service.OcorrenciaService;

@Controller
@RequestMapping("/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaService service;

    public OcorrenciaController(OcorrenciaService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) StatusOcorrencia status, Model model) {
        model.addAttribute("ocorrencias", service.listar(status));
        model.addAttribute("statusFiltro", status);
        model.addAttribute("statusLista", StatusOcorrencia.values());
        return "ocorrencias/lista";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("ocorrencia", service.buscar(id));
            model.addAttribute("statusLista", StatusOcorrencia.values());
            return "ocorrencias/detalhe";
        } catch (NaoEncontradoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/ocorrencias";
        }
    }

    @PostMapping("/{id}/atualizar")
    public String atualizar(@PathVariable Long id, @RequestParam StatusOcorrencia status,
                            @RequestParam(required = false) String resposta, RedirectAttributes redirect) {
        try {
            service.atualizar(id, status, resposta);
            redirect.addFlashAttribute("sucesso", "Ocorrência atualizada. O morador vê o novo status no app.");
        } catch (NaoEncontradoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/ocorrencias";
        }
        return "redirect:/ocorrencias/" + id;
    }
}
