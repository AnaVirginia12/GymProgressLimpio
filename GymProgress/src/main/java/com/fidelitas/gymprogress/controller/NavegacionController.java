package com.fidelitas.gymprogress.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NavegacionController {

    @GetMapping("/historial")
    public String historial() {
        return "historial/index";
    }

    @GetMapping("/progreso")
    public String progreso() {
        return "progreso/index";
    }

    @GetMapping("/perfil")
    public String perfil() {
        return "usuario/perfil";
    }

    @GetMapping("/configuracion")
    public String configuracion() {
        return "configuracion/index";
    }
}