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
        //el return no es texto que se le muestre al usuario, es el nombre de la
        //plantilla: spring lo traduce a templates/historial/index.html
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

        /**
         * el mínimo y el máximo se calculan acá por una razón concreta:
         * thymeleaf tiene #aggregates con sum y avg, pero no tiene min ni max,
         * o sea que este cálculo no se podía hacer en la plantilla
         *
         * sirven para escalar el gráfico:
         *   posicion_y = (peso - pesoMinimo) / rangoPeso * alto
         * sin eso, si el peso va de 74 a 76 kg en una escala de 0 a 100, la
         * línea saldría plana
         */
        //se arranca con los valores extremos justamente para que cualquier peso
        //real los reemplace
        double minPeso = Double.MAX_VALUE;
        double maxPeso = -Double.MAX_VALUE;

        for (var registro : registros) {
            double peso = registro.getPesoKg();
            minPeso = Math.min(minPeso, peso);
            maxPeso = Math.max(maxPeso, peso);
        }

        //sin esto, con la lista vacía minPeso quedaría en Double.MAX_VALUE, o
        //sea un número gigante, y el gráfico se volvería loco
        if (registros.isEmpty()) {
            minPeso = 0;
            maxPeso = 0;
        }

        model.addAttribute("pesoMinimo", minPeso);

        model.addAttribute(
                "rangoPeso",
                //evita la división por cero: si solo hay un registro, o todos
                //pesan igual, el rango sería 0 y la fórmula del gráfico
                //dividiría por cero. el mínimo de 1.0 lo impide
                (maxPeso - minPeso) < 1 ? 1.0 : (maxPeso - minPeso)
        );

        return "progreso/index";
    }

}
