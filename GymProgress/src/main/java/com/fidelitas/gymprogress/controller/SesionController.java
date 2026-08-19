package com.fidelitas.gymprogress.controller;

import com.fidelitas.gymprogress.domain.Ejercicio;
import com.fidelitas.gymprogress.domain.Usuario;
import com.fidelitas.gymprogress.service.AjusteRutinaService;
import com.fidelitas.gymprogress.service.EjercicioService;
import com.fidelitas.gymprogress.service.SesionService;
import com.fidelitas.gymprogress.service.RachaService;
import com.fidelitas.gymprogress.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 * Controlador que maneja una sesión de entrenamiento en vivo:
 * iniciarla, registrar las series que se van haciendo, finalizarla
 * y mostrar el resumen al terminar.
 */
@Controller
@RequestMapping("/sesion")
public class SesionController {

    private static final String SESION_USUARIO_ID = "usuarioId";

    private final SesionService sesionService;
    private final EjercicioService ejercicioService;
    private final RachaService rachaService;
    private final AjusteRutinaService ajusteRutinaService;
    private final UsuarioService usuarioService;

    public SesionController(
            SesionService sesionService,
            EjercicioService ejercicioService,
            RachaService rachaService,
            AjusteRutinaService ajusteRutinaService,
            UsuarioService usuarioService
    ) {
        this.sesionService = sesionService;
        this.ejercicioService = ejercicioService;
        this.rachaService = rachaService;
        this.ajusteRutinaService = ajusteRutinaService;
        this.usuarioService = usuarioService;
    }

 /**
 * Controlador que maneja una sesión de entrenamiento en vivo:
 * iniciarla, registrar las series que se van haciendo, finalizarla
 * y mostrar el resumen al terminar.
 */
    @PostMapping("/iniciar")
    public String iniciar(
            @RequestParam(required = false) Long rutinaId,
            HttpSession session
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        var sesion = sesionService.iniciar(usuarioId, rutinaId);
        //como iniciar() es idempotente, si ya había una sesión abierta devuelve
        //esa. el usuario nunca termina con dos entrenamientos a la vez por más
        //veces que apriete el botón
        return "redirect:/sesion/" + sesion.getId();
    }

    /** HU — Muestra la sesión activa con el formulario para registrar series. */
    @GetMapping("/{sesionId}")
    public String mostrarActiva(
            @PathVariable Long sesionId,
            HttpSession session,
            Model model
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        /**
         * esta es la comprobación de seguridad, y es de las mejores del proyecto.
         *
         * fijarse en lo que hace: no busca la sesión por el id de la url, busca
         * la sesión activa del usuario y después comprueba que coincida
         *
         * eso corta dos problemas de un tiro: si ponen el id de la sesión de
         * otro usuario no coincide y redirige, y si ponen el id de una sesión ya
         * finalizada tampoco hay activa con ese id
         *
         * si se hubiera escrito sesionRepository.findById(sesionId), cualquiera
         * podría ver el entrenamiento de otro cambiando el número en la url
         *
         * y redirige en silencio a la portada en vez de mostrar un error, para
         * no confirmarle a un curioso que esa sesión existe
         */
        var sesion = sesionService.obtenerActiva(usuarioId);
        if (sesion.isEmpty() || !sesion.get().getId().equals(sesionId)) {
            return "redirect:/";
        }

        List<Ejercicio> ejercicios = ejercicioService.listar();
        model.addAttribute("sesion", sesion.get());
        model.addAttribute("ejercicios", ejercicios);

        // HU — Descanso manual o automático según la preferencia guardada en ajustes.
        Usuario usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
        model.addAttribute(
                "descansoAutomatico",
                usuario == null || usuario.getDescansoAutomatico() == null
                        || usuario.getDescansoAutomatico()
        );
        model.addAttribute(
                "descansoPorDefectoSeg",
                usuario != null && usuario.getDescansoPorDefectoSeg() != null
                        ? usuario.getDescansoPorDefectoSeg() : 90
        );
        return "sesion/activa";
    }

    /* Guardar serie (auto-save inmediato)                                  */

    /**
     * HU — Almacena ejercicio, peso, repeticiones y fecha.
     * HU — Marcar serie como fallo muscular.
     * HU — Guardado automático (cada serie se persiste de inmediato).
     */
    @PostMapping("/{sesionId}/serie")
    public String guardarSerie(
            @PathVariable Long sesionId,
            @RequestParam Long ejercicioId,
            @RequestParam String ejercicioNombre,
            @RequestParam Integer numeroSerie,
            @RequestParam Double pesoKg,
            @RequestParam Integer repeticiones,
            //el defaultValue en false porque las casillas sin marcar no mandan
            //nada. es la primera de dos defensas contra ese caso, la segunda
            //está en el servicio con  falloMuscular != null && falloMuscular
            @RequestParam(defaultValue = "false") Boolean falloMuscular,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        sesionService.guardarSerie(
                sesionId, ejercicioId, ejercicioNombre,
                numeroSerie, pesoKg, repeticiones, falloMuscular
        );

        redirectAttributes.addFlashAttribute("serieGuardada", true);
        /**
         * acá el patrón post-redirect-get está en su forma más útil: sin la
         * redirección, cada F5 durante el entrenamiento volvería a enviar el
         * formulario y duplicaría la última serie. en una pantalla donde el
         * usuario está con el teléfono en la mano entre series, eso pasaría
         * todo el tiempo
         */
        //lo que falta: este método no comprueba que la sesión sea del usuario,
        //a diferencia de mostrarActiva(). alguien podría mandar un post con el
        //sesionId de otra persona y agregarle series a su entrenamiento
        return "redirect:/sesion/" + sesionId;
    }

    // Finalizar sesión                                                          
    @PostMapping("/{sesionId}/finalizar")
    public String finalizar(
            @PathVariable Long sesionId,
            HttpSession session
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        Map<String, Object> resumen = sesionService.finalizar(sesionId, usuarioId);

        //Racha: registra el entrenamiento del día
        //este es el único punto de todo el proyecto donde la racha sube, y es
        //seguro llamarlo varias veces el mismo día porque no suma doble
        rachaService.registrarEntrenamiento(usuarioId);

        /**
         * el resumen se calcula acá pero se muestra en otra petición, después de
         * la redirección. se pasa por la HttpSession en vez de un flash
         * attribute porque es un Map con varios objetos adentro
         *
         * el precio es que hay que borrarlo a mano, cosa que hace mostrarResumen()
         */
        session.setAttribute("resumenSesion", resumen);
        return "redirect:/sesion/" + sesionId + "/resumen";
    }

    // Resumen de la sesión finalizada        

    //Muestra el resumen con volumen, tiempo y PRs
    @GetMapping("/{sesionId}/resumen")
    public String mostrarResumen(
            @PathVariable Long sesionId,
            HttpSession session,
            Model model
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }

        /**
         * @SuppressWarnings calla un aviso del compilador. getAttribute devuelve
         * un Object y hay que convertirlo a Map<String,Object>, pero java no
         * puede verificar que ese Map tenga de verdad String como claves, porque
         * los tipos genéricos se borran al compilar. entonces avisa que la
         * conversión no es segura
         *
         * acá es aceptable porque nosotros mismos guardamos ese mapa tres
         * métodos más arriba, pero es señal de que el diseño se puede mejorar:
         * si calcularResumen devolviera un record, esta anotación no haría falta
         */
        @SuppressWarnings("unchecked")
        Map<String, Object> resumen =
                (Map<String, Object>) session.getAttribute("resumenSesion");

        //si alguien entra escribiendo la url, o recarga después de que el
        //resumen se borró, no hay nada que mostrar. en vez de reventar lo manda
        //al historial, donde de todos modos puede ver ese entrenamiento
        if (resumen == null) {
            return "redirect:/historial";
        }

        //vuelca todas las claves del mapa al modelo de una vez. es cómodo, pero
        //desde acá no se ve qué claves hay: para saberlo hay que ir a leer
        //calcularResumen()
        model.addAllAttributes(resumen);

        model.addAttribute("cambios", ajusteRutinaService.cambiosDeSesion(sesionId));

        /**
         * limpiar es imprescindible por dos motivos: por memoria, porque la
         * sesión vive en el servidor y sin limpiar cada entrenamiento dejaría
         * ahí un mapa hasta que la sesión caduque, y por corrección, porque si
         * no se borrara, entrar al resumen de un entrenamiento viejo mostraría
         * los datos del último
         */
        //y es lo que hace que recargar el resumen te mande al historial: la
        //segunda vez 'resumen' ya es null
        session.removeAttribute("resumenSesion");
        return "sesion/resumen";
    }
}
