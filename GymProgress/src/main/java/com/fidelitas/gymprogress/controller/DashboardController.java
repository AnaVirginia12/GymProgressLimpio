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
    
    /**
     * todos los controladores siguen el mismo patrón:
     *   @Controller le dice a spring que esta clase atiende peticiones web
     *   la constante guarda el nombre de la clave en la sesión
     *   las dependencias se piden por el constructor
     *   @GetMapping o @PostMapping dice qué url atiende
     *   la comprobación de sesión es la única barrera de seguridad de la app
     *   model.addAttribute le pasa los datos a la plantilla
     *   el return devuelve el nombre del html, sin el .html
     */
    // Nombre con el que se guarda el id del usuario cuando inicia sesión
    private static final String SESION_USUARIO_ID = "usuarioId";
    //esta constante está declarada en cada controlador por separado, o sea
    //nueve copias. lo ideal sería una sola compartida, pero al menos con la
    //constante nadie escribe "usuarioID" con errata

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
        //el (Long) de adelante es un cast: getAttribute devuelve un Object
        //genérico y hay que decirle a java "confiá en mí, esto es un Long"
        if (usuarioId == null) {
            return "redirect:/login";
            //si no hay id en la sesión es que no inició sesión
        }

        Rutina rutina = rutinaService.obtenerRutinaActiva(usuarioId);
        //este método puede devolver null, por eso las líneas siguientes se
        //protegen antes de usarlo

        model.addAttribute("rutina", rutina);
        
         // Tiempo aproximado que toma completar la rutina
        model.addAttribute(
                "duracionEstimada",
                rutinaService.calcularDuracionEstimadaMinutos(rutina)
        );
        
        // Si no hay rutina o no tiene ejercicios, se muestra 0 en vez de fallar
        //el cálculo se hace en java y no en el html. la regla general del
        //proyecto es que los cálculos van en java y la plantilla solo pinta,
        //cuanta menos lógica tenga el html mejor
        model.addAttribute(
                "totalEjercicios",
                rutina == null || rutina.getEjercicios() == null
                        ? 0
                        : rutina.getEjercicios().size()
        );

        // Racha visible en la pantalla principal, sin navegar.
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
    //un detalle de cariño: la pantalla saluda distinto según la hora
    //no lleva else porque cada rama hace return, eso deja el código más plano
    //que anidar if/else if/else
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
