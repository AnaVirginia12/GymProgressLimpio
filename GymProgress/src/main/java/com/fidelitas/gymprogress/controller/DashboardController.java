package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.domain.Racha;
import com.fidelitas.gymprogress.domain.rutina.Rutina;
import com.fidelitas.gymprogress.service.RachaService;
import com.fidelitas.gymprogress.service.rutina.RutinaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de la pantalla principal (dashboard).
 * Muestra la rutina activa del usuario y un resumen de esta.
 */
@Controller
public class DashboardController {
    
    // Nombre con el que se guarda el id del usuario cuando inicia sesión
    private static final String SESION_USUARIO_ID = "usuarioId";

    // Servicio que trae los datos de la rutina
    private final RutinaService rutinaService;
    private final RachaService rachaService;

    public DashboardController(
            RutinaService rutinaService,
            RachaService rachaService
    ) {
        this.rutinaService = rutinaService;
        this.rachaService = rachaService;
    }
    
     /**
     * Muestra el dashboard con la rutina activa del usuario,
     * cuánto dura aproximadamente y cuántos ejercicios tiene.
     */
    @GetMapping("/")
    public String mostrarDashboard(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        Rutina rutina = rutinaService.obtenerRutinaActiva(usuarioId);

        model.addAttribute("rutina", rutina);
        
         // Tiempo aproximado que toma completar la rutina
        model.addAttribute(
                "duracionEstimada",
                rutinaService.calcularDuracionEstimadaMinutos(rutina)
        );
        
        // Si no hay rutina o no tiene ejercicios, se muestra 0 en vez de fallar
        model.addAttribute(
                "totalEjercicios",
                rutina == null || rutina.getEjercicios() == null
                        ? 0
                        : rutina.getEjercicios().size()
        );

        // HU22 — Racha visible en la pantalla principal, sin navegar.
        Racha racha = rachaService.obtener(usuarioId);
        model.addAttribute("racha", racha);
        model.addAttribute("entrenoHoy",
                racha != null
                && racha.getUltimoEntrenamiento() != null
                && racha.getUltimoEntrenamiento().isEqual(java.time.LocalDate.now()));

        model.addAttribute("saludo", saludoSegunHora());

        return "dashboard/index";
    }

     /**
     * Devuelve un saludo distinto según la hora del día
     * (buenos días, buenas tardes o buenas noches).
     */
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
