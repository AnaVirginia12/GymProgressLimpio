package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.service.RachaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

//el controlador más corto del proyecto
@Controller
@RequestMapping("/racha") //prefijo para todos los métodos de la clase
public class RachaController {

    private static final String SESION_USUARIO_ID = "usuarioId";

    private final RachaService rachaService;

    public RachaController(RachaService rachaService) {
        this.rachaService = rachaService;
    }

    // Muestra la racha actual del usuario. */
    //cuando el @GetMapping va vacío se usa solo el prefijo de la clase, o sea
    //que la url termina siendo /racha a secas
    @GetMapping
    public String mostrar(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        model.addAttribute("racha", rachaService.obtener(usuarioId));
        return "progreso/racha";
    }
}
