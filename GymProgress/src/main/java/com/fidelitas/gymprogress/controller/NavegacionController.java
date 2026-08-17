package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.service.SesionService;
import com.fidelitas.gymprogress.service.RachaService;
import com.fidelitas.gymprogress.service.PesoCorporalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador que maneja las pantallas generales de navegación:
 * historial de entrenamientos, progreso y configuración.
 */
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
    
    /**
     * Muestra el historial completo de entrenamientos anteriores.
     */
    @GetMapping("/historial")
    public String historial(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        model.addAttribute("sesiones", sesionService.historial(usuarioId));
        return "historial/index";
    }

     /**
     * Muestra la pantalla de progreso: la racha de días entrenando
     * y un gráfico con el historial de peso corporal.
     */
    @GetMapping("/progreso")
    public String progreso(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        var historialPeso = pesoCorporalService.historial(usuarioId);

        model.addAttribute("racha", rachaService.obtener(usuarioId));
        model.addAttribute("historialPeso", historialPeso);
        
        var registros = historialPeso.size() > 12
                ? historialPeso.subList(0, 12)
                : historialPeso;

        model.addAttribute("registrosGrafico", registros);

        double minPeso = Double.MAX_VALUE;
        double maxPeso = -Double.MAX_VALUE;

        for (var registro : registros) {
            double peso = registro.getPesoKg();
            minPeso = Math.min(minPeso, peso);
            maxPeso = Math.max(maxPeso, peso);
        }

        if (registros.isEmpty()) {
            minPeso = 0;
            maxPeso = 0;
        }

        model.addAttribute("pesoMinimo", minPeso);

        model.addAttribute(
                "rangoPeso",
                (maxPeso - minPeso) < 1 ? 1.0 : (maxPeso - minPeso)
        );

        return "progreso/index";
    }

     /**
     * Muestra la pantalla de configuración. No necesita datos ni
     * verificar sesión porque no muestra información personal.
     */
    /*
     * Era la única pantalla a la que se entraba sin iniciar sesión.
     * Se le pone el mismo control que al resto.
     */
    @GetMapping("/configuracion")
    public String configuracion(HttpSession session) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        return "configuracion/index";
    }
}
