package com.fidelitas.gymprogress.service;

import com.fidelitas.gymprogress.domain.CambioEjercicio;
import com.fidelitas.gymprogress.domain.Ejercicio;
import com.fidelitas.gymprogress.domain.rutina.Rutina;
import com.fidelitas.gymprogress.domain.rutina.RutinaEjercicio;
import com.fidelitas.gymprogress.repository.CambioEjercicioRepository;
import com.fidelitas.gymprogress.service.rutina.RutinaService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maneja los ajustes que el usuario puede hacer a su rutina en pleno
 * entrenamiento: sustituir un ejercicio por otro, u omitirlo
 */
@Service
public class AjusteRutinaService {

    //dos servicios y un repositorio. un servicio sí puede usar otros servicios,
    //lo que no debe hacer es saltarse capas
    private final RutinaService rutinaService;          //para leer y modificar la rutina
    private final EjercicioService ejercicioService;    //para buscar las alternativas
    private final CambioEjercicioRepository cambioRepository; //el único donde este servicio es dueño de una entidad

    public AjusteRutinaService(
            RutinaService rutinaService,
            EjercicioService ejercicioService,
            CambioEjercicioRepository cambioRepository
    ) {
        this.rutinaService = rutinaService;
        this.ejercicioService = ejercicioService;
        this.cambioRepository = cambioRepository;
    }

     /**
     * Representa una "propuesta" de cambio para un ejercicio de la
     * rutina: el ejercicio actual y una lista de posibles alternativas.
     */
    /**
     * un record declarado dentro de la clase. genera solo el constructor, los
     * métodos de lectura, equals, hashCode y toString
     *
     * agrupa los tres datos que necesita la pantalla, uno por cada ejercicio de
     * la rutina. si el servicio devolviera tres listas paralelas, el html
     * tendría que emparejarlas por índice y sería frágil
     */
    public record Propuesta(
            RutinaEjercicio detalle,
            Ejercicio actual,
            List<Ejercicio> alternativas
    ) {
        //un record puede tener métodos, lo que no puede tener son campos que
        //cambien. este protege contra null y contra lista vacía a la vez
        public boolean tieneAlternativas() {
            return alternativas != null && !alternativas.isEmpty();
        }
    }

    @Transactional(readOnly = true)
    //para "hoy no voy al gimnasio", el true filtra por sin equipo
    public List<Propuesta> proponerAlternativasEnCasa(Long usuarioId) {
        return proponer(usuarioId, true);
    }

    @Transactional(readOnly = true)
    //para "la máquina está ocupada", el false permite ejercicios con equipo
    public List<Propuesta> proponerSustituciones(Long usuarioId) {
        return proponer(usuarioId, false);
    }

    private List<Propuesta> proponer(Long usuarioId, boolean soloEnCasa) {
        Rutina rutina = rutinaService.obtenerRutinaActiva(usuarioId);
        List<Propuesta> propuestas = new ArrayList<>();

        if (rutina == null || rutina.getEjercicios() == null) {
            return propuestas;
        }

        for (RutinaEjercicio detalle : rutina.getEjercicios()) {
            Optional<Ejercicio> actual =
                    ejercicioService.obtenerPorId(detalle.getEjercicioId());

            if (actual.isEmpty()) {
                //el ejercicio pudo haberse borrado del catálogo: como 'ejercicioId'
                //es un Long suelto sin llave foránea, la rutina puede apuntar a
                //algo que ya no existe. se salta en silencio para no reventar
                continue;
            }

            Ejercicio ejercicio = actual.get();

            //si el ejercicio ya se puede hacer en casa no hace falta proponerle
            //alternativas, pero igual se agrega a la lista con la lista vacía,
            //porque el usuario tiene que ver toda su rutina en esa pantalla
            if (soloEnCasa && !Boolean.TRUE.equals(ejercicio.getRequiereEquipo())) {
                // Ya se puede hacer en casa, no necesita alternativa.
                propuestas.add(new Propuesta(detalle, ejercicio, List.of()));
                continue;
            }

            List<Ejercicio> alternativas = soloEnCasa
                    ? ejercicioService.alternativasEnCasa(ejercicio)
                    : ejercicioService.alternativasMismoGrupo(ejercicio);

            propuestas.add(new Propuesta(detalle, ejercicio, alternativas));
        }

        return propuestas;
    }
    
    @Transactional
    public String sustituir(
            Long usuarioId,
            Long rutinaEjercicioId,
            Long nuevoEjercicioId,
            Long sesionId
    ) {
        /**
         * el orden es deliberado: se valida todo antes de tocar nada, así si
         * algo falla la rutina queda intacta
         *
         * además el original se lee antes de sustituir porque después ya no se
         * puede: una vez que cambia el ejercicioId el original se perdió, y hace
         * falta su nombre para el registro del cambio
         */
        RutinaEjercicio detalle = buscarDetalle(usuarioId, rutinaEjercicioId);

        Ejercicio original = ejercicioService.obtenerPorId(detalle.getEjercicioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El ejercicio original ya no existe en el catálogo."));

        Ejercicio nuevo = ejercicioService.obtenerPorId(nuevoEjercicioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El ejercicio de reemplazo no existe."));

        rutinaService.sustituirEjercicio(usuarioId, rutinaEjercicioId, nuevoEjercicioId);

        /**
         * el cambio solo se registra si hay sesión, y eso permite que la misma
         * pantalla sirva en dos momentos:
         *   con sesionId, estás entrenando y se registra
         *   sin sesionId, estás armando la rutina un domingo y no se registra
         */
        if (sesionId != null) {
            CambioEjercicio cambio = new CambioEjercicio();
            cambio.setSesionId(sesionId);
            cambio.setTipo(CambioEjercicio.SUSTITUIDO);
            cambio.setEjercicioOriginalId(original.getId());
            cambio.setEjercicioOriginalNombre(original.getNombre());
            cambio.setEjercicioNuevoId(nuevo.getId());
            cambio.setEjercicioNuevoNombre(nuevo.getNombre());
            cambio.setSeriesAfectadas(detalle.getSeries());
            cambioRepository.save(cambio);
        }

        return "Se sustituyó " + original.getNombre() + " por " + nuevo.getNombre() + ".";
    }
    
    @Transactional
    public String omitir(Long usuarioId, Long rutinaEjercicioId, Long sesionId) {
        RutinaEjercicio detalle = buscarDetalle(usuarioId, rutinaEjercicioId);

        Ejercicio original = ejercicioService.obtenerPorId(detalle.getEjercicioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El ejercicio ya no existe en el catálogo."));

        /**
         * los números se capturan antes de eliminar, y es importante: si
         * totalSeries se llamara después, el total ya no incluiría las series
         * omitidas y el porcentaje saldría mal
         *   midiendo antes:   4 de 20 da 20%, que es lo correcto
         *   midiendo después: 4 de 16 daría 25%
         */
        int seriesOmitidas = detalle.getSeries() == null ? 0 : detalle.getSeries();
        int seriesTotales = totalSeries(usuarioId);

        rutinaService.eliminarEjercicio(usuarioId, rutinaEjercicioId);

        if (sesionId != null) {
            CambioEjercicio cambio = new CambioEjercicio();
            cambio.setSesionId(sesionId);
            cambio.setTipo(CambioEjercicio.OMITIDO);
            cambio.setEjercicioOriginalId(original.getId());
            cambio.setEjercicioOriginalNombre(original.getNombre());
            cambio.setSeriesAfectadas(seriesOmitidas);
            cambioRepository.save(cambio);
        }

        if (seriesTotales <= 0) {
            return "Se omitió " + original.getNombre() + ".";
        }

        //la f de 100f es imprescindible: sin ella los tres números serían
        //enteros y java haría división entera, descartando los decimales
        //  (2*100)/30 entero da 6, porque trunca
        //  (2*100f)/30 da 6.67 y con round queda 7, que es lo correcto
        int porcentaje = Math.round((seriesOmitidas * 100f) / seriesTotales);

        return "Se omitió " + original.getNombre() + ". Pierdes " + seriesOmitidas
                + (seriesOmitidas == 1 ? " serie" : " series")
                + ", un " + porcentaje + "% del volumen planeado para hoy.";
    }

    //Cambios registrados en una sesión, para el historial. 
    @Transactional(readOnly = true)
    public List<CambioEjercicio> cambiosDeSesion(Long sesionId) {
        return cambioRepository.findBySesionIdOrderByRegistradoEnAsc(sesionId);
    }

    private int totalSeries(Long usuarioId) {
        Rutina rutina = rutinaService.obtenerRutinaActiva(usuarioId);

        if (rutina == null || rutina.getEjercicios() == null) {
            return 0;
        }

        int total = 0;
        for (RutinaEjercicio detalle : rutina.getEjercicios()) {
            total += detalle.getSeries() == null ? 0 : detalle.getSeries();
        }
        return total;
    }

    private RutinaEjercicio buscarDetalle(Long usuarioId, Long rutinaEjercicioId) {
        Rutina rutina = rutinaService.obtenerRutinaActiva(usuarioId);

        if (rutina == null) {
            throw new IllegalArgumentException("No tienes una rutina activa.");
        }

        /**
         * esto es una comprobación de autorización disfrazada de búsqueda: se
         * busca el ejercicio dentro de la rutina del usuario, no en toda la tabla
         *
         * si se hubiera escrito rutinaEjercicioRepository.findById(id),
         * cualquiera podría modificar la rutina de otro cambiando el id del
         * formulario
         */
        return rutina.getEjercicios().stream()
                .filter(e -> e.getId().equals(rutinaEjercicioId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ese ejercicio no está en tu rutina."));
    }
}
