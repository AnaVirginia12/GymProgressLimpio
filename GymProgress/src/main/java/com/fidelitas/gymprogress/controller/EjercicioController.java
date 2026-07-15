package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.domain.Ejercicio;
import com.fidelitas.gymprogress.service.EjercicioService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ejercicios")
public class EjercicioController {

    private static final String[] TIPOS_ENTRENAMIENTO = {"Calistenia", "Cardio", "Gimnasio"};

    private static final String[] GRUPOS_MUSCULARES = {
        "Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps",
        "Antebrazos", "Abdomen", "Glúteos", "Cuádriceps",
        "Isquiotibiales", "Pantorrillas"
    };

    private final EjercicioService ejercicioService;

    public EjercicioController(EjercicioService ejercicioService) {
        this.ejercicioService = ejercicioService;
    }

    @GetMapping
    public String listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipo,
            Model model
    ) {
        model.addAttribute("ejercicios", ejercicioService.buscar(q, tipo));
        model.addAttribute("q", q);
        model.addAttribute("tipoSeleccionado", tipo);
        model.addAttribute("tipos", TIPOS_ENTRENAMIENTO);
        return "ejercicio/index";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("ejercicio", new Ejercicio());
        model.addAttribute("tipos", TIPOS_ENTRENAMIENTO);
        model.addAttribute("gruposMusculares", GRUPOS_MUSCULARES);
        return "ejercicio/modifica";
    }

    @PostMapping
    public String crear(
            Ejercicio ejercicio,
            @RequestParam(name = "grupos", required = false) List<String> grupos,
            RedirectAttributes redirectAttributes
    ) {
        ejercicio.setGrupoMuscular(unirGrupos(grupos));
        ejercicioService.crear(ejercicio);
        redirectAttributes.addFlashAttribute("mensaje", "Ejercicio creado correctamente.");
        return "redirect:/ejercicios";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
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
            Ejercicio ejercicio,
            @RequestParam(name = "grupos", required = false) List<String> grupos,
            RedirectAttributes redirectAttributes
    ) {
        ejercicio.setGrupoMuscular(unirGrupos(grupos));
        ejercicioService.actualizar(id, ejercicio);
        redirectAttributes.addFlashAttribute("mensaje", "Ejercicio actualizado correctamente.");
        return "redirect:/ejercicios";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ejercicioService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensaje", "Ejercicio eliminado.");
        return "redirect:/ejercicios";
    }

    @PostMapping("/{id}/favorito")
    public String alternarFavorito(@PathVariable Long id) {
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