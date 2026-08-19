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

   
    //Login
    //el método más simple del proyecto: una línea que devuelve el nombre de la
    //plantilla. no recibe Model ni HttpSession porque no los necesita, y no
    //comprueba sesión, que es lógico: es la pantalla a la que se redirige
    //justamente cuando no hay
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
        //esta es la puerta de entrada de toda la aplicación
        return usuarioService.iniciarSesion(correo, password)
                .map(u -> {  //el camino cuando las credenciales son correctas
                    //esta es la línea de la sesión, el único lugar de todo el
                    //proyecto donde se escribe 'usuarioId'. de ella dependen las
                    //comprobaciones de los otros 25 métodos
                    //
                    //se guarda solo el id y no el objeto entero: la sesión ocupa
                    //memoria del servidor, y el objeto quedaría desactualizado
                    //en cuanto el usuario editara su perfil
                    session.setAttribute(SESION_USUARIO_ID, u.getId());
                    return "redirect:/";
                })
                /**
                 * es orElseGet y no orElse, y acá la diferencia no es de
                 * eficiencia sino de corrección: con orElse el bloque se
                 * ejecutaría siempre, también en los logins correctos, y
                 * aparecería un "correo o contraseña incorrectos" en rojo justo
                 * después de entrar bien
                 */
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute(
                            "error", "Correo o contraseña incorrectos."
                    );
                    return "redirect:/login";
                });
    }

    
    //Logout
    /**
     * es @PostMapping y no @GetMapping por seguridad: si cerrar sesión fuera un
     * get, bastaría con que alguien te hiciera cargar una imagen
     *   <img src="http://tu-app/logout">
     * para cerrarte la sesión desde cualquier página. ese ataque se llama csrf
     *
     * la regla general es get para leer y post para cambiar algo
     */
    @PostMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        Long id = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (id != null) {
            usuarioService.cerrarSesion(id);
        }
        session.invalidate();
        //esta es la que de verdad cierra la sesión, porque destruye la sesión del
        //servidor: a partir de acá getAttribute devuelve null en todas las
        //peticiones. va fuera del if, así se ejecuta siempre por las dudas
        return "redirect:/login";
    }

    
    //Registro con encuesta de onboarding
    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        //mete un Usuario vacío en el modelo. parece innecesario pero la
        //plantilla lo necesita para el th:object:
        //   <form th:object="${usuario}">
        //     <input th:field="*{nombre}">
        //el th:field genera solo el id, el name y el value, pero necesita que el
        //objeto exista. sin esta línea thymeleaf falla al renderizar
        model.addAttribute("usuario", new Usuario());
        return "usuario/registro";
    }

    @PostMapping("/registro")
    /**
     * con @ModelAttribute, en vez de recibir diez @RequestParam sueltos, spring
     * arma el objeto solo: mira los name del formulario y llama al setter que
     * corresponda, o sea name="correo" llama a setCorreo. eso se llama data binding
     *
     * hay un riesgo a tener en cuenta: spring rellena cualquier campo cuyo name
     * coincida, así que si alguien agregara
     *   <input type="hidden" name="id" value="1">
     * ese id llegaría puesto. lo prolijo sería hacer usuario.setId(null) antes
     * de registrar, igual que hace EjercicioService.crear()
     */
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

   
    //Perfil editable
   
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
        /**
         * acá está la defensa importante y es sutil: el objeto 'datosNuevos' que
         * armó spring puede traer un id si alguien manipuló el formulario, pero
         * ese id se ignora por completo, porque al servicio se le pasa el id que
         * salió de la sesión
         *
         * así, aunque alguien mande id=1, va a editar su propio perfil.
         * combinado con la lista blanca de campos del servicio, el perfil queda
         * bien protegido
         */
        Long id = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (id == null) {
            return "redirect:/login";
        }
        usuarioService.actualizarPerfil(id, datosNuevos);
        redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado correctamente.");
        return "redirect:/perfil";
    }
}
