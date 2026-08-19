package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.domain.Usuario;
import com.fidelitas.gymprogress.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Deja el usuario que inició sesión disponible en TODAS las pantallas,
 * sin tener que añadirlo a mano en cada controlador.
 *
 * La barra lateral lo usa para mostrar el nombre y la foto de perfil
 * abajo del todo.
 */
@ControllerAdvice
public class UsuarioActualAdvice {

    private static final String SESION_USUARIO_ID = "usuarioId";

    private final UsuarioService usuarioService;

    public UsuarioActualAdvice(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /*
     * Lo que devuelva este método queda en el modelo con el nombre
     * "usuarioActual" en cada petición que dibuje una pantalla.
     *
     * Devuelve null en login y registro, donde todavía no hay sesión.
     * Las plantillas lo comprueban antes de usarlo.
     */
    @ModelAttribute("usuarioActual")
    public Usuario usuarioActual(HttpSession session) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);

        if (usuarioId == null) {
            return null;
        }

        return usuarioService.buscarPorId(usuarioId).orElse(null);
    }
}
