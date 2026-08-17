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

    private final RutinaService rutinaService;
    private final EjercicioService ejercicioService;
    private final CambioEjercicioRepository cambioRepository;

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
    public record Propuesta(
            RutinaEjercicio detalle,
            Ejercicio actual,
            List<Ejercicio> alternativas
    ) {
        public boolean tieneAlternativas() {
            return alternativas != null && !alternativas.isEmpty();
        }
    }

    @Transactional(readOnly = true)
    public List<Propuesta> proponerAlternativasEnCasa(Long usuarioId) {
        return proponer(usuarioId, true);
    }

    @Transactional(readOnly = true)
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
                continue;
            }

            Ejercicio ejercicio = actual.get();

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
        RutinaEjercicio detalle = buscarDetalle(usuarioId, rutinaEjercicioId);

        Ejercicio original = ejercicioService.obtenerPorId(detalle.getEjercicioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El ejercicio original ya no existe en el catálogo."));

        Ejercicio nuevo = ejercicioService.obtenerPorId(nuevoEjercicioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El ejercicio de reemplazo no existe."));

        rutinaService.sustituirEjercicio(usuarioId, rutinaEjercicioId, nuevoEjercicioId);

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

        return rutina.getEjercicios().stream()
                .filter(e -> e.getId().equals(rutinaEjercicioId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ese ejercicio no está en tu rutina."));
    }
}
