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
import jcondo.entity.Morador;
import jcondo.service.MoradorService;

// Controller das telas (Thymeleaf). A parte REST fica no MoradorRestController
@Controller
@RequestMapping("/moradores")
public class MoradorController {

    private final MoradorService service;

    public MoradorController(MoradorService service) {
        this.service = service;
    }

    // tela inicial com a listagem
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("moradores", service.listarTodos());
        return "index";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("morador", new Morador());
        model.addAttribute("titulo", "Novo Morador");
        return "form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model,
                         RedirectAttributes redirect) {
        try {
            model.addAttribute("morador", service.buscarPorId(id));
            model.addAttribute("titulo", "Editar Morador");
            return "form";
        } catch (IllegalArgumentException e) {
            // id invalido (ex: usuario alterou a url na mao)
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/moradores";
        }
    }

    // salva tanto cadastro novo quanto edicao - o que muda e se o id vem preenchido
    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("morador") Morador morador,
                         BindingResult result, Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            // volta pro form com os erros de validacao em cada campo
            model.addAttribute("titulo",
                    morador.getId() == null ? "Novo Morador" : "Editar Morador");
            return "form";
        }
        boolean novo = morador.getId() == null;
        service.salvar(morador);
        redirect.addFlashAttribute("sucesso", novo
                ? "Morador cadastrado com sucesso!"
                : "Morador atualizado com sucesso!");
        return "redirect:/moradores";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("sucesso", "Morador excluído com sucesso!");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/moradores";
    }
}
