package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.domain.Ejercicio;
import com.fidelitas.gymprogress.service.EjercicioService;
import com.fidelitas.gymprogress.service.SesionService;
import com.fidelitas.gymprogress.service.RachaService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/sesion")
public class SesionController {

    private static final String SESION_USUARIO_ID = "usuarioId";

    private final SesionService sesionService;
    private final EjercicioService ejercicioService;
    private final RachaService rachaService;

    public SesionController(
            SesionService sesionService,
            EjercicioService ejercicioService,
            RachaService rachaService
    ) {
        this.sesionService = sesionService;
        this.ejercicioService = ejercicioService;
        this.rachaService = rachaService;
    }

    /* Iniciar sesión y mostrar sesión activa                               */

    /** HU — Inicia una nueva sesión (o recupera la activa) y redirige a ella. */
    @PostMapping("/iniciar")
    public String iniciar(
            @RequestParam(required = false) Long rutinaId,
            HttpSession session
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        var sesion = sesionService.iniciar(usuarioId, rutinaId);
        return "redirect:/sesion/" + sesion.getId();
    }

    /** HU — Muestra la sesión activa con el formulario para registrar series. */
    @GetMapping("/{sesionId}")
    public String mostrarActiva(
            @PathVariable Long sesionId,
            HttpSession session,
            Model model
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        var sesion = sesionService.obtenerActiva(usuarioId);
        if (sesion.isEmpty() || !sesion.get().getId().equals(sesionId)) {
            return "redirect:/";
        }

        List<Ejercicio> ejercicios = ejercicioService.listar();
        model.addAttribute("sesion", sesion.get());
        model.addAttribute("ejercicios", ejercicios);
        return "sesion/activa";
    }

    /* Guardar serie (auto-save inmediato)                                  */

    /**
     * HU — Almacena ejercicio, peso, repeticiones y fecha.
     * HU — Marcar serie como fallo muscular.
     * HU — Guardado automático (cada serie se persiste de inmediato).
     */
    @PostMapping("/{sesionId}/serie")
    public String guardarSerie(
            @PathVariable Long sesionId,
            @RequestParam Long ejercicioId,
            @RequestParam String ejercicioNombre,
            @RequestParam Integer numeroSerie,
            @RequestParam Double pesoKg,
            @RequestParam Integer repeticiones,
            @RequestParam(defaultValue = "false") Boolean falloMuscular,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        sesionService.guardarSerie(
                sesionId, ejercicioId, ejercicioNombre,
                numeroSerie, pesoKg, repeticiones, falloMuscular
        );

        redirectAttributes.addFlashAttribute("serieGuardada", true);
        return "redirect:/sesion/" + sesionId;
    }

    // Finalizar sesión                                                     

    //HU — Finaliza la sesión, actualiza la racha y redirige al resumen.
     
    @PostMapping("/{sesionId}/finalizar")
    public String finalizar(
            @PathVariable Long sesionId,
            HttpSession session
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        Map<String, Object> resumen = sesionService.finalizar(sesionId, usuarioId);

        // HU — Racha: registra el entrenamiento del día
        rachaService.registrarEntrenamiento(usuarioId);

        session.setAttribute("resumenSesion", resumen);
        return "redirect:/sesion/" + sesionId + "/resumen";
    }

    // Resumen de la sesión finalizada        

    // HU — Muestra el resumen con volumen, tiempo y PRs
    @GetMapping("/{sesionId}/resumen")
    public String mostrarResumen(
            @PathVariable Long sesionId,
            HttpSession session,
            Model model
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> resumen =
                (Map<String, Object>) session.getAttribute("resumenSesion");

        if (resumen == null) {
            return "redirect:/historial";
        }

        model.addAllAttributes(resumen);
        session.removeAttribute("resumenSesion");
        return "sesion/resumen";
    }
}
