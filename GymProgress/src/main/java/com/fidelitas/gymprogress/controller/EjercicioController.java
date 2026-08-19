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

/**
 * Controlador del módulo de Ejercicios.
 * Aquí se manejan todas las acciones: ver la lista, buscar, crear,
 * editar, eliminar y marcar como favorito.
 */
@Controller
@RequestMapping("/ejercicios")
public class EjercicioController {

    private static final String SESION_USUARIO_ID = "usuarioId";

    // Opciones fijas que se muestran en el formulario (tipo de entrenamiento)
    private static final String[] TIPOS_ENTRENAMIENTO = {"Calistenia", "Cardio", "Gimnasio"};
    //static final quiere decir una sola copia para toda la aplicación, y que no
    //se puede reasignar. eso sí, final impide reasignar la variable pero no
    //impide cambiar el contenido del arreglo, lo verdaderamente inmutable
    //sería List.of(...)

    // Opciones fijas de grupos musculares que se muestran en el formulario
    /**
     * esta lista está acoplada al modelo de datos: la lógica de las alternativas
     * busca comparando el texto del grupo muscular, así que si en un ejercicio
     * alguien guardara "pecho" en minúscula o "Pectorales", la búsqueda no lo
     * encontraría. tener el desplegable con valores fijos es lo que garantiza
     * que todos escriban igual
     */
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

     /**
     * Muestra la lista de ejercicios. Permite buscar por texto y
     * filtrar por tipo de entrenamiento.
     */
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
        //estas dos líneas devuelven a la vista lo que el usuario buscó, para que
        //el formulario quede relleno después de buscar. sin ellas, cada búsqueda
        //dejaría el buscador en blanco y el usuario no sabría qué había buscado
        model.addAttribute("q", q);
        model.addAttribute("tipoSeleccionado", tipo);
        model.addAttribute("tipos", TIPOS_ENTRENAMIENTO);
        return "ejercicio/index";
    }

     /**
     * Muestra el formulario vacío para crear un ejercicio nuevo.
     */
    @GetMapping("/nuevo")
    public String nuevo(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        model.addAttribute("ejercicio", new Ejercicio());
        model.addAttribute("tipos", TIPOS_ENTRENAMIENTO);
        model.addAttribute("gruposMusculares", GRUPOS_MUSCULARES);
        //devuelve "modifica" y no "nuevo": la misma plantilla sirve para crear y
        //para editar, solo cambia si el objeto viene vacío o lleno
        return "ejercicio/modifica";
    }
    
    
     /**
     * Guarda el ejercicio nuevo que el usuario llenó en el formulario.
     * Si algo sale mal (por ejemplo, datos inválidos), regresa al
     * formulario mostrando el error en vez de romper la aplicación.
     */
    @PostMapping
    public String crear(
            @ModelAttribute Ejercicio ejercicio,
            /**
             * spring puede recibir listas. el formulario tiene varias casillas
             * con el mismo name:
             *   <input type="checkbox" name="grupos" value="Pecho">
             *   <input type="checkbox" name="grupos" value="Tríceps">
             * al marcar dos, el navegador manda grupos=Pecho&grupos=Tríceps y
             * spring lo convierte en una List<String>
             *
             * si no se marca ninguna el parámetro no viaja, por eso el
             * required = false, para que llegue null en vez de dar error 400
             */
            @RequestParam(name = "grupos", required = false) List<String> grupos,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long usuarioId = (Long) session.getAttribute(SESION_USUARIO_ID);
        if (usuarioId == null) {
            return "redirect:/login";
        }
        ejercicio.setGrupoMuscular(unirGrupos(grupos));
        //esta línea es el puente entre el formulario y el modelo: convierte la
        //lista en el texto separado por comas que espera la entidad.
        //@ModelAttribute no puede hacerlo solo porque el formulario manda un
        //campo llamado "grupos" y la entidad tiene uno llamado "grupoMuscular",
        //y no coinciden

        try {
            ejercicioService.crear(ejercicio);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ejercicios/nuevo";
        }

        redirectAttributes.addFlashAttribute("mensaje", "Ejercicio creado correctamente.");
        return "redirect:/ejercicios";
    }

    /**
     * Muestra el formulario ya lleno con los datos del ejercicio a editar.
     */
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

    /**
     * Elimina un ejercicio de la lista.
     */
    /**
     * eliminar y alternarFavorito son post y no get, por lo mismo que el logout:
     * si eliminar fuera get, un <img src="/ejercicios/5/eliminar"> en cualquier
     * página borraría el ejercicio 5
     *
     * una diferencia con el resto del proyecto: acá no se comprueba de quién es
     * el ejercicio, y no se puede, porque el catálogo es compartido entre todos
     * los usuarios, 'Ejercicio' no tiene campo usuarioId. o sea que cualquiera
     * puede editar o borrar los de todos. no es un descuido, es consecuencia
     * del diseño del modelo
     */
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

     /**
     * Marca o desmarca un ejercicio como favorito.
     */
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

        //hace falta la rutina porque el tempo recomendado depende del programa:
        //en hipertrofia se sugiere 3-1-1-0 y en fuerza 2-1-1-1
        Rutina rutina = rutinaService.obtenerRutinaActiva(usuarioId);
        String programa = rutina == null ? null : rutina.getPrograma();
        //el ternario protege del null que puede devolver obtenerRutinaActiva

        int[] tempo = ejercicioService.tempoRecomendado(ejercicio, programa);

        model.addAttribute("ejercicio", ejercicio);
        model.addAttribute("programa", programa);
        //se desarma el arreglo en cuatro atributos para que la plantilla quede
        //legible: ${tempoExcentrico} se entiende mucho mejor que ${tempo[0]}
        model.addAttribute("tempoExcentrico", tempo[0]);
        model.addAttribute("tempoPausaAbajo", tempo[1]);
        model.addAttribute("tempoConcentrico", tempo[2]);
        model.addAttribute("tempoPausaArriba", tempo[3]);
        //le dice a la vista si el tempo es el del ejercicio o una sugerencia,
        //así la pantalla puede mostrar "tempo del ejercicio" o "tempo recomendado
        //para hipertrofia". es un detalle honesto, el usuario sabe de dónde sale
        model.addAttribute("tempoPropio", ejercicio.tieneTempo());
        model.addAttribute("segundosRepeticion", tempo[0] + tempo[1] + tempo[2] + tempo[3]);
        model.addAttribute("alternativasCasa", ejercicioService.alternativasEnCasa(ejercicio));

        return "ejercicio/detalle";
    }


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
        //el atributo 'modo' es lo que hace que una sola plantilla sirva para las
        //dos pantallas:
        //   <div th:if="${modo == 'casa'}">Entrenar en casa</div>
        //   <div th:if="${modo == 'ajuste'}">Ajustar rutina</div>
        model.addAttribute("modo", "casa");

        return "ejercicio/alternativas";
    }


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

        //solo en modo ajuste se muestran los cambios que ya hiciste en esta
        //sesión, para que veas lo que llevás omitido o sustituido
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

    //devuelve a la pantalla de la que veníamos conservando el contexto: si no
    //se volviera a pegar el sesionId en la url, la siguiente acción ya no se
    //registraría como parte del entrenamiento
    private String destino(String modo, Long sesionId) {
        String ruta = "ajuste".equals(modo) ? "/ejercicios/ajustar" : "/ejercicios/alternativas";
        return "redirect:" + ruta + (sesionId == null ? "" : "?sesionId=" + sesionId);
    }

    /**
     * el puente entre las casillas del formulario y el campo de la base:
     *   ["Pecho", "Tríceps"] queda como "Pecho, Tríceps"
     *
     * devuelve null si no se marcó ninguna, y es a propósito: grupoMuscular es
     * obligatorio, así que el validar() del servicio lanzará la excepción y el
     * usuario verá "el grupo muscular es obligatorio"
     *
     * el separador es coma con espacio, y el trim del otro lado, en
     * EjercicioService, se lo saca. los dos métodos están diseñados para encajar
     */
    private String unirGrupos(List<String> grupos) {
        if (grupos == null || grupos.isEmpty()) {
            return null;
        }
        return String.join(", ", grupos);
    }
}
