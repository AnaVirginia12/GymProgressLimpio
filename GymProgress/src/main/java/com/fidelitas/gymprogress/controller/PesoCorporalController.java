package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.domain.PesoCorporal;
import com.fidelitas.gymprogress.domain.Usuario;
import com.fidelitas.gymprogress.service.PesoCorporalService;
import com.fidelitas.gymprogress.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 * Controlador para registrar el peso corporal del usuario
 * y ver su historial de peso.
 */
@Controller
@RequestMapping("/peso")
public class PesoCorporalController {

    private static final String SESION_USUARIO_ID = "usuarioId";

    private final PesoCorporalService pesoCorporalService;
    private final UsuarioService usuarioService;

    public PesoCorporalController(
            PesoCorporalService pesoCorporalService,
            UsuarioService usuarioService
    ) {
        this.pesoCorporalService = pesoCorporalService;
        this.usuarioService = usuarioService;
    }

    /**
     * Formulario para registrar peso con selector de unidad (kg / lb).
     */
    @GetMapping
    public String mostrarFormulario(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        // Unidad preferida del usuario para pre-seleccionar el toggle
        String unidad = usuarioService.buscarPorId(usuarioId)
                .map(Usuario::getUnidadPeso)
                .orElse("kg");

        List<PesoCorporal> historial = pesoCorporalService.historial(usuarioId);

        model.addAttribute("unidad", unidad);
        model.addAttribute("historial", historial);

        // HU — Registro periódico: recuerda al usuario si lleva tiempo sin pesarse.
        Long diasDesdeUltimo = pesoCorporalService.diasDesdeUltimoRegistro(usuarioId);
        model.addAttribute("diasDesdeUltimo", diasDesdeUltimo);
        model.addAttribute("recordatorioPeso", diasDesdeUltimo != null && diasDesdeUltimo >= 7);

        return "progreso/peso";
    }

    /**
     * Guarda el peso y redirige al historial de progreso.
     * Acepta el parámetro "unidad" (kg/lb) para la conversión.
     */
    @PostMapping
    public String guardarPeso(
            @RequestParam Double peso,
            @RequestParam(defaultValue = "kg") String unidad,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        pesoCorporalService.guardar(usuarioId, peso, unidad);

        // Actualiza la preferencia de unidad del usuario
        usuarioService.buscarPorId(usuarioId).ifPresent(u -> {
            u.setUnidadPeso(unidad);
            usuarioService.actualizarPerfil(usuarioId, u);
        });

        redirectAttributes.addFlashAttribute("mensaje", "Peso registrado correctamente.");
        return "redirect:/peso";
    }

    /**
     * Elimina un registro de peso del historial (por ejemplo, si se
     * ingresó por error).
     */
    @PostMapping("/{id}/eliminar")
    public String eliminarPeso(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        pesoCorporalService.eliminar(usuarioId, id);

        redirectAttributes.addFlashAttribute("mensaje", "Registro eliminado.");
        return "redirect:/peso";
    }
}
