package jcondo.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // manda direto pro painel (se nao estiver logado o Spring Security leva pro login)
    @GetMapping("/")
    public String home() {
        return "redirect:/painel";
    }

    @GetMapping("/login")
    public String login(Authentication auth) {
        if (auth != null && !(auth instanceof AnonymousAuthenticationToken) && auth.isAuthenticated()) {
            return "redirect:/painel";
        }
        return "login";
    }
}
