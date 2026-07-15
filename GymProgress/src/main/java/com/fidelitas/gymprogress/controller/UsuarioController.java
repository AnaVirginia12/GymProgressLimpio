package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.domain.Usuario;
import com.fidelitas.gymprogress.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UsuarioController {

    private static final String SESION_USUARIO_ID = "usuarioId";

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

   
    // HU1 — Login

    @GetMapping("/login")
    public String mostrarLogin() {
        return "usuario/login";
    }

    @PostMapping("/login")
    public String procesarLogin(
            @RequestParam String correo,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return usuarioService.iniciarSesion(correo, password)
                .map(u -> {
                    session.setAttribute(SESION_USUARIO_ID, u.getId());
                    return "redirect:/";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute(
                            "error", "Correo o contraseña incorrectos."
                    );
                    return "redirect:/login";
                });
    }

    
    // HU1 — Logout
    

    @PostMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        Long id = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (id != null) {
            usuarioService.cerrarSesion(id);
        }
        session.invalidate();
        return "redirect:/login";
    }

    
    // HU4 — Registro con encuesta de onboarding
   

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "usuario/registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(
            @ModelAttribute Usuario usuario,
            RedirectAttributes redirectAttributes
    ) {
        try {
            usuarioService.registrar(usuario);
            redirectAttributes.addFlashAttribute(
                    "mensaje", "Cuenta creada correctamente. Inicia sesión."
            );
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/registro";
        }
    }

   
    // HU5 — Perfil editable
    

    @GetMapping("/perfil")
    public String mostrarPerfil(HttpSession session, Model model) {
        Long id = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (id == null) {
            return "redirect:/login";
        }
        usuarioService.buscarPorId(id).ifPresent(u -> model.addAttribute("usuario", u));
        return "usuario/perfil";
    }

    @PostMapping("/perfil")
    public String actualizarPerfil(
            @ModelAttribute Usuario datosNuevos,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long id = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (id == null) {
            return "redirect:/login";
        }
        usuarioService.actualizarPerfil(id, datosNuevos);
        redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado correctamente.");
        return "redirect:/perfil";
    }
}
