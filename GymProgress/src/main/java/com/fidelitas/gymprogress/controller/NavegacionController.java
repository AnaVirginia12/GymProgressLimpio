package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.service.SesionService;
import com.fidelitas.gymprogress.service.RachaService;
import com.fidelitas.gymprogress.service.PesoCorporalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NavegacionController {

    private static final String SESION_USUARIO_ID = "usuarioId";

    private final SesionService sesionService;
    private final RachaService rachaService;
    private final PesoCorporalService pesoCorporalService;

    public NavegacionController(
            SesionService sesionService,
            RachaService rachaService,
            PesoCorporalService pesoCorporalService
    ) {
        this.sesionService = sesionService;
        this.rachaService = rachaService;
        this.pesoCorporalService = pesoCorporalService;
    }

    /** HU — Historial completo de entrenamientos anteriores. */
    @GetMapping("/historial")
    public String historial(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        model.addAttribute("sesiones", sesionService.historial(usuarioId));
        return "historial/index";
    }

    /** HU — Progreso: racha + datos de peso corporal para gráficos. */
    @GetMapping("/progreso")
    public String progreso(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        model.addAttribute("racha", rachaService.obtener(usuarioId));
        model.addAttribute("historialPeso", pesoCorporalService.historial(usuarioId));
        return "progreso/index";
    }

    @GetMapping("/configuracion")
    public String configuracion() {
        return "configuracion/index";
    }
}
