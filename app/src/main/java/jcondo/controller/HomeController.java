package jcondo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // manda direto pra listagem
    @GetMapping("/")
    public String home() {
        return "redirect:/moradores";
    }
}
