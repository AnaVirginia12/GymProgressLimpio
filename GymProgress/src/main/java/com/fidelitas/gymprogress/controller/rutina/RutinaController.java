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

@Controller
@RequestMapping("/rutinas")
public class RutinaController {

    private static final String SESION_USUARIO_ID = "usuarioId";

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

    @GetMapping
    public String listar(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        model.addAttribute("rutinas", rutinaService.listarRutinas(usuarioId));
        return "rutina/index";
    }

    @GetMapping("/programas")
    public String programas(HttpSession session) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        return "rutina/programas";
    }

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

        rutinaService.seleccionarPrograma(usuarioId, programa);

        redirectAttributes.addFlashAttribute(
                "mensaje",
                "Programa seleccionado correctamente."
        );

        return "redirect:/rutinas";
    }

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

        Map<Long, String> nombres = new HashMap<>();
        for (Ejercicio ejercicio : catalogo) {
            nombres.put(ejercicio.getId(), ejercicio.getNombre());
        }
        model.addAttribute("nombresEjercicios", nombres);

        // HU22 — Racha destacada, sin tener que navegar a otra pantalla.
        Racha racha = rachaService.obtener(usuarioId);
        model.addAttribute("racha", racha);
        model.addAttribute("entrenoHoy", entrenoHoy(racha));

        // HU37 — Semana de descarga: evalúa, y si toca deja la sugerencia.
        deloadService.evaluar(usuarioId);

        DeloadService.EstadoDeload deload = deloadService.estado(usuarioId);
        model.addAttribute("deload", deload);

        double factor = deloadService.factorVolumen(usuarioId);
        model.addAttribute("enDescarga", factor < 1.0);

        // Series ajustadas por ejercicio durante la descarga.
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

        return "rutina/hoy";
    }

    /* HU22 — ¿La racha de hoy ya está completada? */
    private boolean entrenoHoy(Racha racha) {
        return racha != null
                && racha.getUltimoEntrenamiento() != null
                && racha.getUltimoEntrenamiento().isEqual(LocalDate.now());
    }

    /* HU37 — El usuario acepta, pospone o ignora la descarga.               */

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
