package jcondo.controller;

import java.time.LocalTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jcondo.entity.AreaComum;
import jcondo.exception.NaoEncontradoException;
import jcondo.exception.RegraNegocioException;
import jcondo.service.AreaComumService;

@Controller
@RequestMapping("/areas")
public class AreaController {

    private final AreaComumService service;

    public AreaController(AreaComumService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("areas", service.listarTodas());
        return "areas/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        AreaComum area = new AreaComum();
        area.setHoraAbertura(LocalTime.of(8, 0));
        area.setHoraFechamento(LocalTime.of(22, 0));
        model.addAttribute("area", area);
        model.addAttribute("titulo", "Nova área comum");
        return "areas/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("area", service.buscar(id));
            model.addAttribute("titulo", "Editar área comum");
            return "areas/form";
        } catch (NaoEncontradoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/areas";
        }
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("area") AreaComum area, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        boolean nova = area.getId() == null;
        model.addAttribute("titulo", nova ? "Nova área comum" : "Editar área comum");
        if (result.hasErrors()) {
            return "areas/form";
        }
        try {
            service.salvar(area);
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "areas/form";
        }
        redirect.addFlashAttribute("sucesso", nova ? "Área cadastrada." : "Área atualizada.");
        return "redirect:/areas";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("sucesso", "Área excluída.");
        } catch (NaoEncontradoException | RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/areas";
    }
}
