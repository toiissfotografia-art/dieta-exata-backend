package com.dietaexata.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() { return "forward:/login.html"; }

    @GetMapping("/login")
    public String login() { return "forward:/login.html"; }

    @GetMapping("/cadastro")
    public String cadastro() { return "forward:/cadastro.html"; }

    @GetMapping("/dashboard")
    public String dashboard() { return "forward:/dashboard.html"; }

    @GetMapping("/upgrade")
    public String upgrade() { return "forward:/upgrade.html"; }

    @GetMapping("/configurar-dieta")
    public String configurar() { return "forward:/configurar-dieta.html"; }
}