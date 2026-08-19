package com.fidelitas.gymprogress.controller.rutina;

import com.fidelitas.gymprogress.domain.Ejercicio;
import com.fidelitas.gymprogress.domain.Racha;
import com.fidelitas.gymprogress.domain.rutina.Rutina;
import com.fidelitas.gymprogress.domain.rutina.RutinaEjercicio;
import com.fidelitas.gymprogress.service.EjercicioService;
import com.fidelitas.gymprogress.service.RachaService;
import com.fidelitas.gymprogress.service.rutina.DeloadService;
import com.fidelitas.gymprogress.service.rutina.RutinaService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador del módulo de Rutinas: ver la lista de rutinas,
 * elegir un programa, ver la rutina de hoy con racha y descarga
 * (deload), y agregar o quitar ejercicios de la rutina.
 */
@Controller
@RequestMapping("/rutinas")
public class RutinaController {

    private static final String SESION_USUARIO_ID = "usuarioId";

    // Servicios que traen y guardan los datos que necesita este controlador
    private final RutinaService rutinaService;
    private final EjercicioService ejercicioService;
    private final RachaService rachaService;
    private final DeloadService deloadService;

    public RutinaController(
            RutinaService rutinaService,
            EjercicioService ejercicioService,
            RachaService rachaService,
            DeloadService deloadService
    ) {
        this.rutinaService = rutinaService;
        this.ejercicioService = ejercicioService;
        this.rachaService = rachaService;
        this.deloadService = deloadService;
    }

    //Muestra la lista de rutinas del usuario 
    @GetMapping
    public String listar(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        model.addAttribute("rutinas", rutinaService.listarRutinas(usuarioId));
        return "rutina/index";
    }

    //Muestra la pantalla para poder elegir un programa de entrenamiento
    @GetMapping("/programas")
    public String programas(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        /*
         * La pantalla necesita saber cuál es el programa actual y cuántos
         * ejercicios tiene la rutina, para avisar antes de cambiarlo que
         * se van a reajustar las repeticiones y los descansos.
         */
        Rutina rutina = rutinaService.obtenerRutinaActiva(usuarioId);

        model.addAttribute(
                "programaActual",
                rutina != null ? rutina.getPrograma() : null
        );

        model.addAttribute(
                "totalEjercicios",
                rutina != null && rutina.getEjercicios() != null
                        ? rutina.getEjercicios().size()
                        : 0
        );

        return "rutina/programas";
    }

    //Guarda el programa que el usuario eligio
    @PostMapping("/programas/seleccionar")
    public String seleccionarPrograma(
            @RequestParam String programa,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        RutinaService.ResultadoPrograma resultado =
                rutinaService.seleccionarPrograma(usuarioId, programa);

        /*
         * Si el cambio de programa reajustó ejercicios se le dice al
         * usuario, para que no le extrañe ver otras repeticiones.
         */
        int ajustados = resultado.ejerciciosAjustados();

        if (ajustados > 0) {
            RutinaService.ParametrosPrograma parametros =
                    RutinaService.parametrosDe(programa);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Programa cambiado a " + programa.toLowerCase() + ". Se ajustaron "
                            + ajustados
                            + (ajustados == 1 ? " ejercicio a " : " ejercicios a ")
                            + parametros.repsObjetivo() + " repeticiones y "
                            + parametros.descansoSeg() + " s de descanso."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Programa seleccionado correctamente."
            );
        }

        return "redirect:/rutinas";
    }

    /**
     * Muestra la rutina de hoy, junto con:
     * - la racha de días entrenados
     * - si toca semana de descarga o no
     * - el catálogo de ejercicios para poder agregarlos a la rutina
     */
    @GetMapping("/hoy")
    public String rutinaHoy(HttpSession session, Model model) {
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

        // Catálogo para el formulario de "agregar ejercicio a la rutina"
        List<Ejercicio> catalogo = ejercicioService.listar();
        model.addAttribute("catalogoEjercicios", catalogo);

        /**
         * este mapa de id a nombre resuelve un problema concreto:
         * 'RutinaEjercicio' guarda solo el ejercicioId, no el nombre, así que la
         * plantilla escribe ${nombresEjercicios[detalle.ejercicioId]}
         *
         * es un truco de rendimiento, no solo de comodidad: sin el mapa, la
         * plantilla tendría que buscar cada ejercicio uno por uno, con una
         * consulta por fila. así, con una sola pasada al catálogo, que además ya
         * se traía para el desplegable, se arma un diccionario en memoria
         */
        Map<Long, String> nombres = new HashMap<>();
        for (Ejercicio ejercicio : catalogo) {
            nombres.put(ejercicio.getId(), ejercicio.getNombre());
        }
        model.addAttribute("nombresEjercicios", nombres);

        //Racha destacada, sin tener que navegar a otra pantalla.
        Racha racha = rachaService.obtener(usuarioId);
        model.addAttribute("racha", racha);
        model.addAttribute("entrenoHoy", entrenoHoy(racha));

        /**
         * esta línea dispara toda la detección de fatiga.
         *
         * fijarse en el diseño: la detección no corre en segundo plano ni con
         * una tarea programada, corre cada vez que el usuario entra a la rutina
         * de hoy. es pragmático y está bien pensado, porque no hace falta un
         * @Scheduled ni infraestructura extra, el análisis se hace justo cuando
         * el usuario va a ver el resultado, y evaluar() es idempotente, así que
         * si ya hay una sugerencia pendiente devuelve esa sin crear otra
         */
        //se llama sin usar lo que devuelve porque lo que interesa es el efecto
        //secundario, o sea que la sugerencia quede creada; el estado se lee
        //después con estado()
        //Semana de descarga: evalúa, y si toca deja la sugerencia.
        deloadService.evaluar(usuarioId);

        DeloadService.EstadoDeload deload = deloadService.estado(usuarioId);
        model.addAttribute("deload", deload);

        double factor = deloadService.factorVolumen(usuarioId);
        model.addAttribute("enDescarga", factor < 1.0);

        // Series ajustadas por ejercicio durante la descarga.
        /**
         * otro mapa precalculado, esta vez de rutinaEjercicioId a las series
         * ajustadas. con descarga activa el factor es 0.6, así que un ejercicio
         * de 5 series aparece con 3
         *
         * lo importante: las series originales no se modifican en la base, el
         * ajuste es solo para mostrar. así, cuando la semana de descarga termina,
         * la rutina vuelve sola a sus valores normales y no hay que deshacer nada
         */
        Map<Long, Integer> seriesAjustadas = new HashMap<>();
        if (rutina != null && rutina.getEjercicios() != null) {
            for (RutinaEjercicio detalle : rutina.getEjercicios()) {
                seriesAjustadas.put(
                        detalle.getId(),
                        deloadService.seriesAjustadas(detalle.getSeries(), factor)
                );
            }
        }
        model.addAttribute("seriesAjustadas", seriesAjustadas);

        /*
         * Valores que el formulario de "agregar ejercicio" propone por
         * defecto, según el programa de la rutina.
         */
        RutinaService.ParametrosPrograma parametros = RutinaService.parametrosDe(
                rutina != null ? rutina.getPrograma() : null
        );
        model.addAttribute("repsPrograma", parametros.repsObjetivo());
        model.addAttribute("descansoPrograma", parametros.descansoSeg());

        return "rutina/hoy";
    }

    /**
     * Revisa si el usuario ya entrenó hoy, comparando la
     * fecha del último entrenamiento con la fecha de hoy.
     */
    private boolean entrenoHoy(Racha racha) {
        return racha != null
                && racha.getUltimoEntrenamiento() != null
                && racha.getUltimoEntrenamiento().isEqual(LocalDate.now());
    }

    //El usuario acepta la semana de descarga sugerida
    @PostMapping("/descarga/{id}/aceptar")
    public String aceptarDescarga(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return decidirDescarga(id, session, redirectAttributes, "aceptar");
    }

    @PostMapping("/descarga/{id}/posponer")
    public String posponerDescarga(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return decidirDescarga(id, session, redirectAttributes, "posponer");
    }

    @PostMapping("/descarga/{id}/ignorar")
    public String ignorarDescarga(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return decidirDescarga(id, session, redirectAttributes, "ignorar");
    }

    private String decidirDescarga(
            Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            String decision
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        try {
            //un switch con flechas pero que no devuelve valor: es una sentencia
            //y no una expresión, por eso cada rama abre llaves y el switch no
            //termina en punto y coma
            switch (decision) {
                case "aceptar" -> {
                    deloadService.aceptar(usuarioId, id);
                    redirectAttributes.addFlashAttribute("mensaje",
                            "Semana de descarga activada. Durante 7 días tu rutina "
                            + "baja a un 60% del volumen y un 10% menos de carga.");
                }
                case "posponer" -> {
                    deloadService.posponer(usuarioId, id);
                    redirectAttributes.addFlashAttribute("mensaje",
                            "Descarga pospuesta. Se volverá a revisar en una semana.");
                }
                default -> {
                    deloadService.ignorar(usuarioId, id);
                    redirectAttributes.addFlashAttribute("mensaje",
                            "Sugerencia descartada. No se volverá a proponer en un mes.");
                }
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/rutinas/hoy";
    }

    //Agrega un ejercico del catalogo a la rutina de hoy 
    @PostMapping("/hoy/ejercicios")
    public String agregarEjercicio(
            @RequestParam Long ejercicioId,
            @RequestParam(required = false) Integer series,
            @RequestParam(required = false) Integer repsObjetivo,
            @RequestParam(required = false) Integer descansoSeg,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        try {
            rutinaService.agregarEjercicio(
                    usuarioId, ejercicioId, series, repsObjetivo, descansoSeg
            );
            redirectAttributes.addFlashAttribute(
                    "mensaje", "Ejercicio agregado a la rutina."
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/rutinas/hoy";
    }

    // quita el ejercicio de la rutina de hoy 
    @PostMapping("/hoy/ejercicios/{id}/eliminar")
    public String eliminarEjercicio(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        try {
            rutinaService.eliminarEjercicio(usuarioId, id);
            redirectAttributes.addFlashAttribute(
                    "mensaje", "Ejercicio eliminado de la rutina."
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/rutinas/hoy";
    }
}
