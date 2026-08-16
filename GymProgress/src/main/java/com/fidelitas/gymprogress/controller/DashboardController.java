package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.domain.rutina.Rutina;
import com.fidelitas.gymprogress.service.rutina.RutinaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private static final String SESION_USUARIO_ID = "usuarioId";

    private final RutinaService rutinaService;

    public DashboardController(RutinaService rutinaService) {
        this.rutinaService = rutinaService;
    }
    
    @GetMapping("/")
    public String mostrarDashboard(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        Rutina rutina = rutinaService.obtenerRutinaActiva(usuarioId);

        model.addAttribute("rutina", rutina);

        model.addAttribute(
                "duracionEstimada",
                rutinaService.calcularDuracionEstimadaMinutos(rutina)
        );

        model.addAttribute(
                "totalEjercicios",
                rutina == null || rutina.getEjercicios() == null
                        ? 0
                        : rutina.getEjercicios().size()
        );

        model.addAttribute("saludo", saludoSegunHora());

        return "dashboard/index";
    }

    private String saludoSegunHora() {
        int hora = java.time.LocalTime.now().getHour();

        if (hora < 12) {
            return "Buenos días";
        }
        if (hora < 19) {
            return "Buenas tardes";
        }
        return "Buenas noches";
    }
}
