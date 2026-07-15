package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.domain.Ejercicio;
import com.fidelitas.gymprogress.service.EjercicioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ejercicios")
public class EjercicioController {

    private static final String SESION_USUARIO_ID = "usuarioId";

    private final EjercicioService ejercicioService;

    public EjercicioController(EjercicioService ejercicioService) {
        this.ejercicioService = ejercicioService;
    }

    @GetMapping
    public String listar(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        model.addAttribute("ejercicios", ejercicioService.listarTodos());
        return "ejercicio/index";
    }

    @GetMapping("/nuevo")
    public String mostrarFormulario(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        model.addAttribute("ejercicio", new Ejercicio());
        return "ejercicio/modifica";
    }

    @GetMapping("/{id}/editar")
    public String mostrarEdicion(
            @PathVariable Long id,
            HttpSession session,
            Model model
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        ejercicioService.buscarPorId(id)
                .ifPresent(e -> model.addAttribute("ejercicio", e));
        return "ejercicio/modifica";
    }

    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Ejercicio ejercicio,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        ejercicioService.guardar(ejercicio);
        redirectAttributes.addFlashAttribute("mensaje", "Ejercicio guardado correctamente.");
        return "redirect:/ejercicios";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        ejercicioService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensaje", "Ejercicio eliminado.");
        return "redirect:/ejercicios";
    }
}
