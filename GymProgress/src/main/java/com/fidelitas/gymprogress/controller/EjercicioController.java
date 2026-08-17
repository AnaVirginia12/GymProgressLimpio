package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.domain.Ejercicio;
import com.fidelitas.gymprogress.service.EjercicioService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador del módulo de Ejercicios.
 * Aquí se manejan todas las acciones: ver la lista, buscar, crear,
 * editar, eliminar y marcar como favorito.
 */
@Controller
@RequestMapping("/ejercicios")
public class EjercicioController {

    private static final String SESION_USUARIO_ID = "usuarioId";

    // Opciones fijas que se muestran en el formulario (tipo de entrenamiento)
    private static final String[] TIPOS_ENTRENAMIENTO = {"Calistenia", "Cardio", "Gimnasio"};

    // Opciones fijas de grupos musculares que se muestran en el formulario
    private static final String[] GRUPOS_MUSCULARES = {
        "Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps",
        "Antebrazos", "Abdomen", "Glúteos", "Cuádriceps",
        "Isquiotibiales", "Pantorrillas"
    };

    private final EjercicioService ejercicioService;

    public EjercicioController(EjercicioService ejercicioService) {
        this.ejercicioService = ejercicioService;
    }

     /**
     * Muestra la lista de ejercicios. Permite buscar por texto y
     * filtrar por tipo de entrenamiento.
     */
    @GetMapping
    public String listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipo,
            HttpSession session,
            Model model
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        model.addAttribute("ejercicios", ejercicioService.buscar(q, tipo));
        model.addAttribute("q", q);
        model.addAttribute("tipoSeleccionado", tipo);
        model.addAttribute("tipos", TIPOS_ENTRENAMIENTO);
        return "ejercicio/index";
    }

     /**
     * Muestra el formulario vacío para crear un ejercicio nuevo.
     */
    @GetMapping("/nuevo")
    public String nuevo(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        model.addAttribute("ejercicio", new Ejercicio());
        model.addAttribute("tipos", TIPOS_ENTRENAMIENTO);
        model.addAttribute("gruposMusculares", GRUPOS_MUSCULARES);
        return "ejercicio/modifica";
    }
    
    
     /**
     * Guarda el ejercicio nuevo que el usuario llenó en el formulario.
     * Si algo sale mal (por ejemplo, datos inválidos), regresa al
     * formulario mostrando el error en vez de romper la aplicación.
     */
    @PostMapping
    public String crear(
            @ModelAttribute Ejercicio ejercicio,
            @RequestParam(name = "grupos", required = false) List<String> grupos,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        ejercicio.setGrupoMuscular(unirGrupos(grupos));

        try {
            ejercicioService.crear(ejercicio);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ejercicios/nuevo";
        }

        redirectAttributes.addFlashAttribute("mensaje", "Ejercicio creado correctamente.");
        return "redirect:/ejercicios";
    }

    /**
     * Muestra el formulario ya lleno con los datos del ejercicio a editar.
     */
    @GetMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            HttpSession session,
            Model model
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        Ejercicio ejercicio = ejercicioService.obtenerPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Ejercicio no encontrado."));
        model.addAttribute("ejercicio", ejercicio);
        model.addAttribute("tipos", TIPOS_ENTRENAMIENTO);
        model.addAttribute("gruposMusculares", GRUPOS_MUSCULARES);
        return "ejercicio/modifica";
    }

    @PostMapping("/{id}")
    public String actualizar(
            @PathVariable Long id,
            @ModelAttribute Ejercicio ejercicio,
            @RequestParam(name = "grupos", required = false) List<String> grupos,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        ejercicio.setGrupoMuscular(unirGrupos(grupos));

        try {
            ejercicioService.actualizar(id, ejercicio);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ejercicios/" + id + "/editar";
        }

        redirectAttributes.addFlashAttribute("mensaje", "Ejercicio actualizado correctamente.");
        return "redirect:/ejercicios";
    }

    /**
     * Elimina un ejercicio de la lista.
     */
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

     /**
     * Marca o desmarca un ejercicio como favorito.
     */
    @PostMapping("/{id}/favorito")
    public String alternarFavorito(
            @PathVariable Long id,
            HttpSession session
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        ejercicioService.alternarFavorito(id);
        return "redirect:/ejercicios";
    }

    private String unirGrupos(List<String> grupos) {
        if (grupos == null || grupos.isEmpty()) {
            return null;
        }
        return String.join(", ", grupos);
    }
}
