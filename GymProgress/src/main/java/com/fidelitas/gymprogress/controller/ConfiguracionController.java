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

        //si el usuario existe se mete en el modelo, y si no, no se mete nada y
        //la plantilla recibiría 'usuario' en null. pasaría solo si la sesión
        //apunta a un usuario borrado
        usuarioService.buscarPorId(usuarioId).ifPresent(u -> model.addAttribute("usuario", u));
        return "configuracion/index";
    }

    /**
     * Guarda la preferencia de tema. Se llama vía fetch() desde el
     * slider que aparece en todas las pantallas, así que responde
     * siempre 200 y no redirige aunque no haya sesión activa.
     */
    /**
     * este método es distinto a todos los demás del proyecto: el @ResponseBody
     * con ResponseEntity<Void> significa que no devuelve una plantilla, devuelve
     * una respuesta http vacía con código 200. es el único endpoint que funciona
     * como una mini api
     *
     * ¿por qué? porque lo llama app.js con fetch desde el slider del tema, que
     * está en todas las pantallas. si devolviera una redirección, el navegador
     * recargaría la página cada vez que cambiás de tema
     */
    @PostMapping("/tema")
    @ResponseBody
    public ResponseEntity<Void> guardarTema(
            @RequestParam String tema,
            HttpSession session
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        //solo se aceptan "dark" y "light": sin esta comprobación alguien podría
        //mandar cualquier texto y quedaría guardado en la columna, rompiendo el
        //data-bs-theme del html
        if (usuarioId != null && ("dark".equals(tema) || "light".equals(tema))) {
            Usuario datosNuevos = new Usuario();
            datosNuevos.setTema(tema);
            usuarioService.actualizarPerfil(usuarioId, datosNuevos);
        }
        //responde 200 siempre, incluso si no hay sesión o el tema es inválido.
        //es a propósito: el slider funciona igual en el login, y si devolviera
        //error el catch de app.js tendría que manejarlo
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

        /**
         * se crea un Usuario vacío y solo se le ponen los dos campos que cambian.
         * como actualizarPerfil hace actualización parcial, o sea que solo pisa
         * lo que no es null, todos los demás datos del usuario se conservan
         *
         * así se reutiliza el mismo servicio sin necesitar un método aparte
         */
        Usuario datosNuevos = new Usuario();
        datosNuevos.setDescansoAutomatico(descansoAutomatico);
        datosNuevos.setDescansoPorDefectoSeg(descansoPorDefectoSeg);
        usuarioService.actualizarPerfil(usuarioId, datosNuevos);

        redirectAttributes.addFlashAttribute("mensaje", "Preferencias de descanso guardadas.");
        return "redirect:/configuracion";
    }
}
