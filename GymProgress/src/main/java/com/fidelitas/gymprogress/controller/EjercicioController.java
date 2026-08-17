package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.domain.Ejercicio;
import com.fidelitas.gymprogress.domain.rutina.Rutina;
import com.fidelitas.gymprogress.service.AjusteRutinaService;
import com.fidelitas.gymprogress.service.EjercicioService;
import com.fidelitas.gymprogress.service.rutina.RutinaService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ejercicios")
public class EjercicioController {

    private static final String SESION_USUARIO_ID = "usuarioId";

    private static final String[] TIPOS_ENTRENAMIENTO = {"Calistenia", "Cardio", "Gimnasio"};

    private static final String[] GRUPOS_MUSCULARES = {
        "Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps",
        "Antebrazos", "Abdomen", "Glúteos", "Cuádriceps",
        "Isquiotibiales", "Pantorrillas"
    };

    private final EjercicioService ejercicioService;
    private final AjusteRutinaService ajusteRutinaService;
    private final RutinaService rutinaService;

    public EjercicioController(
            EjercicioService ejercicioService,
            AjusteRutinaService ajusteRutinaService,
            RutinaService rutinaService
    ) {
        this.ejercicioService = ejercicioService;
        this.ajusteRutinaService = ajusteRutinaService;
        this.rutinaService = rutinaService;
    }

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

    /* HU9 — Detalle del ejercicio con el tempo y la guía visual.            */

    @GetMapping("/{id}")
    public String detalle(
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

        Rutina rutina = rutinaService.obtenerRutinaActiva(usuarioId);
        String programa = rutina == null ? null : rutina.getPrograma();

        int[] tempo = ejercicioService.tempoRecomendado(ejercicio, programa);

        model.addAttribute("ejercicio", ejercicio);
        model.addAttribute("programa", programa);
        model.addAttribute("tempoExcentrico", tempo[0]);
        model.addAttribute("tempoPausaAbajo", tempo[1]);
        model.addAttribute("tempoConcentrico", tempo[2]);
        model.addAttribute("tempoPausaArriba", tempo[3]);
        model.addAttribute("tempoPropio", ejercicio.tieneTempo());
        model.addAttribute("segundosRepeticion", tempo[0] + tempo[1] + tempo[2] + tempo[3]);
        model.addAttribute("alternativasCasa", ejercicioService.alternativasEnCasa(ejercicio));

        return "ejercicio/detalle";
    }

    /* HU7 — Alternativas en casa cuando el usuario no va al gimnasio.       */

    @GetMapping("/alternativas")
    public String alternativas(
            @RequestParam(required = false) Long sesionId,
            HttpSession session,
            Model model
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        model.addAttribute("propuestas",
                ajusteRutinaService.proponerAlternativasEnCasa(usuarioId));
        model.addAttribute("sesionId", sesionId);
        model.addAttribute("modo", "casa");

        return "ejercicio/alternativas";
    }

    /* HU31 — Omitir o sustituir durante el entrenamiento.                   */

    @GetMapping("/ajustar")
    public String ajustar(
            @RequestParam(required = false) Long sesionId,
            HttpSession session,
            Model model
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        model.addAttribute("propuestas",
                ajusteRutinaService.proponerSustituciones(usuarioId));
        model.addAttribute("sesionId", sesionId);
        model.addAttribute("modo", "ajuste");

        if (sesionId != null) {
            model.addAttribute("cambios", ajusteRutinaService.cambiosDeSesion(sesionId));
        }

        return "ejercicio/alternativas";
    }

    /* HU7 y HU31 — Aceptar la alternativa sugerida (sustituir).             */

    @PostMapping("/rutina/{rutinaEjercicioId}/sustituir")
    public String sustituir(
            @PathVariable Long rutinaEjercicioId,
            @RequestParam Long nuevoEjercicioId,
            @RequestParam(required = false) Long sesionId,
            @RequestParam(defaultValue = "casa") String modo,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        try {
            String aviso = ajusteRutinaService.sustituir(
                    usuarioId, rutinaEjercicioId, nuevoEjercicioId, sesionId);
            redirectAttributes.addFlashAttribute("mensaje", aviso);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return destino(modo, sesionId);
    }

    /* HU31 — Omitir un ejercicio y avisar del impacto en el volumen.        */

    @PostMapping("/rutina/{rutinaEjercicioId}/omitir")
    public String omitir(
            @PathVariable Long rutinaEjercicioId,
            @RequestParam(required = false) Long sesionId,
            @RequestParam(defaultValue = "ajuste") String modo,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        try {
            String aviso = ajusteRutinaService.omitir(usuarioId, rutinaEjercicioId, sesionId);
            redirectAttributes.addFlashAttribute("mensaje", aviso);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return destino(modo, sesionId);
    }

    private String destino(String modo, Long sesionId) {
        String ruta = "ajuste".equals(modo) ? "/ejercicios/ajustar" : "/ejercicios/alternativas";
        return "redirect:" + ruta + (sesionId == null ? "" : "?sesionId=" + sesionId);
    }

    private String unirGrupos(List<String> grupos) {
        if (grupos == null || grupos.isEmpty()) {
            return null;
        }
        return String.join(", ", grupos);
    }
}
