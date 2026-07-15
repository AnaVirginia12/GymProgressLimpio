package com.fidelitas.gymprogress.service;

import com.fidelitas.gymprogress.domain.Sesion;
import com.fidelitas.gymprogress.domain.SerieRegistrada;
import com.fidelitas.gymprogress.repository.SesionRepository;
import com.fidelitas.gymprogress.repository.SerieRegistradaRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SesionService {

    private final SesionRepository sesionRepository;
    private final SerieRegistradaRepository serieRegistradaRepository;

    public SesionService(
            SesionRepository sesionRepository,
            SerieRegistradaRepository serieRegistradaRepository
    ) {
        this.sesionRepository = sesionRepository;
        this.serieRegistradaRepository = serieRegistradaRepository;
    }

    /* ------------------------------------------------------------------ */
    /* Iniciar sesión de entrenamiento                                      */
    /* ------------------------------------------------------------------ */

    /**
     * Crea y persiste una nueva sesión para el usuario.
     * HU — Guardado automático: la sesión queda en BD desde el primer instante.
     */
    @Transactional
    public Sesion iniciar(Long usuarioId, Long rutinaId) {
        // Si ya hay una sesión activa, la devuelve sin crear otra
        Optional<Sesion> activa = sesionRepository
                .findFirstByUsuarioIdAndFinalizadaEnIsNullOrderByIniciadaEnDesc(usuarioId);
        if (activa.isPresent()) {
            return activa.get();
        }

        Sesion sesion = new Sesion();
        sesion.setUsuarioId(usuarioId);
        sesion.setRutinaId(rutinaId);
        return sesionRepository.save(sesion);
    }

    /* ------------------------------------------------------------------ */
    /* Sesión activa                                                        */
    /* ------------------------------------------------------------------ */

    @Transactional(readOnly = true)
    public Optional<Sesion> obtenerActiva(Long usuarioId) {
        return sesionRepository
                .findFirstByUsuarioIdAndFinalizadaEnIsNullOrderByIniciadaEnDesc(usuarioId);
    }

    /* ------------------------------------------------------------------ */
    /* Guardar serie (con fallo muscular)                                   */
    /* ------------------------------------------------------------------ */

    /**
     * HU — Almacena ejercicio, peso, repeticiones y fecha de cada serie.
     * HU — Marcar serie como fallo muscular.
     * HU — Guardado automático: cada serie se persiste de inmediato.
     */
    @Transactional
    public SerieRegistrada guardarSerie(
            Long sesionId,
            Long ejercicioId,
            String ejercicioNombre,
            Integer numeroSerie,
            Double pesoKg,
            Integer repeticiones,
            Boolean falloMuscular
    ) {
        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada."));

        SerieRegistrada serie = new SerieRegistrada();
        serie.setEjercicioId(ejercicioId);
        serie.setEjercicioNombre(ejercicioNombre);
        serie.setNumeroSerie(numeroSerie);
        serie.setPesoKg(pesoKg);
        serie.setRepeticiones(repeticiones);
        serie.setFalloMuscular(falloMuscular != null && falloMuscular);
        serie.setRegistradaEn(LocalDateTime.now());

        sesion.agregarSerie(serie);
        sesionRepository.save(sesion); // cascade guarda la serie automáticamente
        return serie;
    }

    /* ------------------------------------------------------------------ */
    /* Finalizar sesión y calcular resumen                                  */
    /* ------------------------------------------------------------------ */

    /**
     * HU — Resumen al finalizar entrenamiento (volumen, tiempo, PRs).
     * Cierra la sesión y retorna un mapa con las métricas.
     */
    @Transactional
    public Map<String, Object> finalizar(Long sesionId, Long usuarioId) {
        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada."));

        sesion.setFinalizadaEn(LocalDateTime.now());
        sesionRepository.save(sesion);

        return calcularResumen(sesion, usuarioId);
    }

    /**
     * Calcula volumen total, duración en minutos, número de series
     * y detecta récords personales (PRs) en esta sesión.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> calcularResumen(Sesion sesion, Long usuarioId) {
        List<SerieRegistrada> series = sesion.getSeries();

        // Volumen total (kg × reps de todas las series)
        double volumenTotal = series.stream()
                .mapToDouble(s -> s.getPesoKg() != null && s.getRepeticiones() != null
                        ? s.getPesoKg() * s.getRepeticiones() : 0.0)
                .sum();

        // Duración en minutos
        LocalDateTime fin = sesion.getFinalizadaEn() != null
                ? sesion.getFinalizadaEn() : LocalDateTime.now();
        long duracionMinutos = Duration.between(sesion.getIniciadaEn(), fin).toMinutes();

        // PRs: para cada ejercicio distinto de la sesión, comprueba si el volumen
        // de la mejor serie de hoy supera el histórico anterior
        Map<Long, SerieRegistrada> mejorPorEjercicioHoy = new LinkedHashMap<>();
        for (SerieRegistrada s : series) {
            mejorPorEjercicioHoy.merge(
                    s.getEjercicioId(), s,
                    (a, b) -> a.getVolumen() >= b.getVolumen() ? a : b
            );
        }

        Map<String, Double> prs = new LinkedHashMap<>();
        for (Map.Entry<Long, SerieRegistrada> entry : mejorPorEjercicioHoy.entrySet()) {
            Optional<SerieRegistrada> prHistorico = serieRegistradaRepository
                    .findPrByEjercicioIdAndUsuarioId(entry.getKey(), usuarioId);

            // Si el volumen de hoy supera el PR histórico anterior (excluyendo la sesión actual)
            double volumenHoy = entry.getValue().getVolumen();
            boolean esPr = prHistorico
                    .filter(pr -> !pr.getSesion().getId().equals(sesion.getId()))
                    .map(pr -> volumenHoy > pr.getVolumen())
                    .orElse(true); // primer registro = PR automático

            if (esPr) {
                prs.put(entry.getValue().getEjercicioNombre(), entry.getValue().getPesoKg());
            }
        }

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("sesion", sesion);
        resumen.put("volumenTotal", Math.round(volumenTotal * 10.0) / 10.0);
        resumen.put("duracionMinutos", duracionMinutos);
        resumen.put("totalSeries", series.size());
        resumen.put("prs", prs);
        return resumen;
    }

    /* ------------------------------------------------------------------ */
    /* Historial completo                                                   */
    /* ------------------------------------------------------------------ */

    /**
     * HU — Historial completo de entrenamientos anteriores.
     */
    @Transactional(readOnly = true)
    public List<Sesion> historial(Long usuarioId) {
        return sesionRepository
                .findByUsuarioIdAndFinalizadaEnIsNotNullOrderByIniciadaEnDesc(usuarioId);
    }
}
