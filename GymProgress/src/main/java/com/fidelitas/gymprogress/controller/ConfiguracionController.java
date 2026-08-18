package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.domain.Usuario;
import com.fidelitas.gymprogress.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador de la pantalla de ajustes: preferencia de tema
 * (oscuro/claro) y modo de descanso entre series (manual/automático).
 */
@Controller
@RequestMapping("/configuracion")
public class ConfiguracionController {

    private static final String SESION_USUARIO_ID = "usuarioId";

    private final UsuarioService usuarioService;

    public ConfiguracionController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String mostrar(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        usuarioService.buscarPorId(usuarioId).ifPresent(u -> model.addAttribute("usuario", u));
        return "configuracion/index";
    }

    /**
     * Guarda la preferencia de tema. Se llama vía fetch() desde el
     * slider que aparece en todas las pantallas, así que responde
     * siempre 200 y no redirige aunque no haya sesión activa.
     */
    @PostMapping("/tema")
    @ResponseBody
    public ResponseEntity<Void> guardarTema(
            @RequestParam String tema,
            HttpSession session
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId != null && ("dark".equals(tema) || "light".equals(tema))) {
            Usuario datosNuevos = new Usuario();
            datosNuevos.setTema(tema);
            usuarioService.actualizarPerfil(usuarioId, datosNuevos);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Guarda si el descanso entre series se inicia solo, o el usuario
     * lo dispara manualmente, junto con la duración por defecto.
     */
    @PostMapping("/descanso")
    public String guardarDescanso(
            @RequestParam Boolean descansoAutomatico,
            @RequestParam Integer descansoPorDefectoSeg,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        Usuario datosNuevos = new Usuario();
        datosNuevos.setDescansoAutomatico(descansoAutomatico);
        datosNuevos.setDescansoPorDefectoSeg(descansoPorDefectoSeg);
        usuarioService.actualizarPerfil(usuarioId, datosNuevos);

        redirectAttributes.addFlashAttribute("mensaje", "Preferencias de descanso guardadas.");
        return "redirect:/configuracion";
    }
}
