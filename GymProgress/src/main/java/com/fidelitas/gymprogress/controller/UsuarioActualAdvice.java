package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.domain.Usuario;
import com.fidelitas.gymprogress.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * deja el usuario que inició sesión disponible en todas las pantallas, sin
 * tener que añadirlo a mano en cada controlador. la barra lateral lo usa para
 * mostrar el nombre y la foto de perfil abajo del todo
 *
 * @ControllerAdvice es distinto de @Controller: no atiende ninguna url, lo que
 * hace es aplicarse a todos los controladores a la vez. combinado con
 * @ModelAttribute, permite meter un dato en el modelo de todas las pantallas
 * sin tener que agregarlo a mano en los nueve controladores
 */
@ControllerAdvice
public class UsuarioActualAdvice {

    private static final String SESION_USUARIO_ID = "usuarioId";

    private final UsuarioService usuarioService;

    public UsuarioActualAdvice(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    //lo que devuelva este método queda guardado en el modelo de cada pantalla
    //que se dibuje, o sea que no hay que pedirlo en ningún controlador
    //el nombre entre paréntesis es con el que queda en el modelo, o sea que en
    //cualquier plantilla se puede escribir ${usuarioActual.nombre}
    @ModelAttribute("usuarioActual")
    public Usuario usuarioActual(HttpSession session) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);

        if (usuarioId == null) {
            return null;
            //devuelve null en login y registro, donde todavía no hay sesión. las
            //plantillas lo comprueban antes de usarlo con th:if
        }

        return usuarioService.buscarPorId(usuarioId).orElse(null);
    }
}
