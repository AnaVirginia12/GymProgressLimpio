package com.fidelitas.gymprogress.controller.rutina;

import com.fidelitas.gymprogress.domain.Ejercicio;
import com.fidelitas.gymprogress.domain.rutina.Rutina;
import com.fidelitas.gymprogress.service.EjercicioService;
import com.fidelitas.gymprogress.service.rutina.RutinaService;
import jakarta.servlet.http.HttpSession;
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

    public RutinaController(
            RutinaService rutinaService,
            EjercicioService ejercicioService
    ) {
        this.rutinaService = rutinaService;
        this.ejercicioService = ejercicioService;
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

        return "rutina/hoy";
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
