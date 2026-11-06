package jcondo.controller;

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
import jcondo.entity.Aviso;
import jcondo.entity.CategoriaAviso;
import jcondo.exception.NaoEncontradoException;
import jcondo.security.UsuarioLogado;
import jcondo.service.AvisoService;

@Controller
@RequestMapping("/avisos")
public class AvisoController {

    private final AvisoService service;

    public AvisoController(AvisoService service) {
        this.service = service;
    }

    @ModelAttribute("categorias")
    public CategoriaAviso[] categorias() {
        return CategoriaAviso.values();
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("avisos", service.listar());
        return "avisos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("aviso", new Aviso());
        model.addAttribute("titulo", "Novo aviso");
        return "avisos/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("aviso", service.buscar(id));
            model.addAttribute("titulo", "Editar aviso");
            return "avisos/form";
        } catch (NaoEncontradoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/avisos";
        }
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("aviso") Aviso aviso, BindingResult result,
                         UsuarioLogado quem, Model model, RedirectAttributes redirect) {
        boolean novo = aviso.getId() == null;
        if (result.hasErrors()) {
            model.addAttribute("titulo", novo ? "Novo aviso" : "Editar aviso");
            return "avisos/form";
        }
        service.salvar(aviso, quem.id());
        redirect.addFlashAttribute("sucesso", novo
                ? "Aviso publicado."
                : "Aviso atualizado.");
        return "redirect:/avisos";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("sucesso", "Aviso excluído.");
        } catch (NaoEncontradoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/avisos";
    }
}
