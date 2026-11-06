package jcondo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jcondo.entity.Morador;
import jcondo.exception.ConflitoException;
import jcondo.exception.NaoEncontradoException;
import jcondo.exception.RegraNegocioException;
import jcondo.service.MoradorService;

// Controller das telas (Thymeleaf). A API fica em jcondo.api
@Controller
@RequestMapping("/moradores")
public class MoradorController {

    private final MoradorService service;

    public MoradorController(MoradorService service) {
        this.service = service;
    }

    // tela com a listagem
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("moradores", service.listarTodos());
        return "moradores/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("morador", new Morador());
        model.addAttribute("titulo", "Novo morador");
        return "moradores/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("morador", service.buscarPorId(id));
            model.addAttribute("titulo", "Editar morador");
            return "moradores/form";
        } catch (NaoEncontradoException e) {
            // id invalido (ex: usuario alterou a url na mao)
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/moradores";
        }
    }

    // salva tanto cadastro novo quanto edicao - o que muda e se o id vem preenchido
    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("morador") Morador morador, BindingResult result,
                         @RequestParam(required = false) String senhaAcesso,
                         Model model, RedirectAttributes redirect) {
        boolean novo = morador.getId() == null;
        model.addAttribute("titulo", novo ? "Novo morador" : "Editar morador");
        if (result.hasErrors()) {
            // volta pro form com os erros de validacao em cada campo
            return "moradores/form";
        }
        try {
            service.salvar(morador, senhaAcesso);
        } catch (ConflitoException | RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "moradores/form";
        }
        redirect.addFlashAttribute("sucesso", novo
                ? "Morador cadastrado."
                : "Morador atualizado.");
        return "redirect:/moradores";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addFlashAttribute("sucesso", "Morador excluído.");
        } catch (NaoEncontradoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/moradores";
    }
}
