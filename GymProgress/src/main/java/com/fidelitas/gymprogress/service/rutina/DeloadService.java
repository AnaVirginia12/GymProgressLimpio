package com.fidelitas.gymprogress.service.rutina;

import com.fidelitas.gymprogress.domain.SerieRegistrada;
import com.fidelitas.gymprogress.domain.Sesion;
import com.fidelitas.gymprogress.domain.rutina.SemanaDescarga;
import com.fidelitas.gymprogress.repository.SesionRepository;
import com.fidelitas.gymprogress.repository.rutina.SemanaDescargaRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU37 — Detección automática de semana de descarga (deload).
 *
 * Analiza las 4 últimas semanas de entrenamiento y busca señales de fatiga
 * acumulada. Si las encuentra, deja una sugerencia que el usuario puede
 * aceptar, posponer o ignorar. Mientras la descarga está activa, la rutina
 * se muestra con menos volumen e intensidad.
 */
@Service
public class DeloadService {

    private static final int SEMANAS_ANALIZADAS = 4;

    /** A partir de este porcentaje de series al fallo se considera fatiga alta. */
    private static final double UMBRAL_FALLO = 0.30;

    /** Caída de volumen respecto a la semana anterior que se considera bajón. */
    private static final double UMBRAL_CAIDA_VOLUMEN = 0.15;

    private final SesionRepository sesionRepository;
    private final SemanaDescargaRepository semanaDescargaRepository;

    public DeloadService(
            SesionRepository sesionRepository,
            SemanaDescargaRepository semanaDescargaRepository
    ) {
        this.sesionRepository = sesionRepository;
        this.semanaDescargaRepository = semanaDescargaRepository;
    }

    /** Resumen de una semana del análisis. */
    public record ResumenSemana(
            int numero,
            double volumen,
            int series,
            int seriesAlFallo,
            int sesiones
    ) {
        public double porcentajeFallo() {
            return series == 0 ? 0 : (double) seriesAlFallo / series;
        }
    }

    /** Lo que necesita la vista para pintar el estado del deload. */
    public record EstadoDeload(
            SemanaDescarga semana,
            List<ResumenSemana> semanas,
            boolean activa,
            boolean pendiente
    ) {
        public boolean tieneDatos() {
            return semanas != null && !semanas.isEmpty();
        }
    }

    /*
     * Criterio 1 — Analiza rendimiento y fatiga de las últimas 4 semanas.
     *
     * Se agrupan las sesiones finalizadas por semana (la 1 es la más
     * reciente) y de cada una se saca volumen total, número de series y
     * cuántas fueron al fallo muscular.
     */
    @Transactional(readOnly = true)
    public List<ResumenSemana> analizarUltimasSemanas(Long usuarioId) {
        List<Sesion> sesiones =
                sesionRepository.findByUsuarioIdAndFinalizadaEnIsNotNullOrderByIniciadaEnDesc(usuarioId);

        LocalDate hoy = LocalDate.now();
        List<ResumenSemana> resumen = new ArrayList<>();

        for (int semana = 1; semana <= SEMANAS_ANALIZADAS; semana++) {
            LocalDate desde = hoy.minusDays(7L * semana);
            LocalDate hasta = hoy.minusDays(7L * (semana - 1));

            double volumen = 0;
            int series = 0;
            int alFallo = 0;
            int cuantasSesiones = 0;

            for (Sesion sesion : sesiones) {
                LocalDate fecha = sesion.getIniciadaEn().toLocalDate();

                if (fecha.isBefore(desde) || !fecha.isBefore(hasta)) {
                    continue;
                }

                cuantasSesiones++;

                if (sesion.getSeries() == null) {
                    continue;
                }

                for (SerieRegistrada serie : sesion.getSeries()) {
                    series++;
                    if (serie.getVolumen() != null) {
                        volumen += serie.getVolumen();
                    }
                    if (Boolean.TRUE.equals(serie.getFalloMuscular())) {
                        alFallo++;
                    }
                }
            }

            resumen.add(new ResumenSemana(semana, volumen, series, alFallo, cuantasSesiones));
        }

        return resumen;
    }

    /*
     * Criterio 2 — Si detecta señales de sobreentrenamiento, sugiere descarga.
     *
     * Se buscan dos señales, y basta con una:
     *
     *   a) Fatiga alta sostenida: más del 30% de las series al fallo en las
     *      dos últimas semanas.
     *   b) Rendimiento a la baja: el volumen de la última semana cayó más de
     *      un 15% respecto a la anterior, habiendo entrenado igual o más.
     *
     * Solo se sugiere si hay datos suficientes (al menos 2 semanas con
     * entrenamientos) y no hay ya una sugerencia pendiente, una descarga
     * activa, o un aplazamiento vigente.
     */
    @Transactional
    public Optional<SemanaDescarga> evaluar(Long usuarioId) {
        Optional<SemanaDescarga> ultima =
                semanaDescargaRepository.findFirstByUsuarioIdOrderByDetectadaEnDesc(usuarioId);

        if (ultima.isPresent()) {
            SemanaDescarga s = ultima.get();

            if (s.estaPendiente() || s.estaActiva()) {
                return ultima;
            }

            boolean aplazada = s.getReintentarDesde() != null
                    && LocalDate.now().isBefore(s.getReintentarDesde());

            if (aplazada) {
                return Optional.empty();
            }
        }

        List<ResumenSemana> semanas = analizarUltimasSemanas(usuarioId);

        long semanasConDatos = semanas.stream().filter(s -> s.sesiones() > 0).count();
        if (semanasConDatos < 2) {
            return Optional.empty();
        }

        ResumenSemana estaSemana = semanas.get(0);
        ResumenSemana anterior = semanas.get(1);

        String motivo = detectarMotivo(estaSemana, anterior);

        if (motivo == null) {
            return Optional.empty();
        }

        SemanaDescarga sugerencia = new SemanaDescarga();
        sugerencia.setUsuarioId(usuarioId);
        sugerencia.setEstado(SemanaDescarga.SUGERIDA);
        sugerencia.setMotivo(motivo);

        return Optional.of(semanaDescargaRepository.save(sugerencia));
    }

    private String detectarMotivo(ResumenSemana estaSemana, ResumenSemana anterior) {
        boolean fatigaAlta = estaSemana.series() > 0
                && anterior.series() > 0
                && estaSemana.porcentajeFallo() > UMBRAL_FALLO
                && anterior.porcentajeFallo() > UMBRAL_FALLO;

        if (fatigaAlta) {
            int porcentaje = (int) Math.round(estaSemana.porcentajeFallo() * 100);
            return "Llevas dos semanas seguidas llegando al fallo en más del "
                    + porcentaje + "% de tus series. Es una señal clara de fatiga acumulada.";
        }

        boolean caidaRendimiento = anterior.volumen() > 0
                && estaSemana.sesiones() >= anterior.sesiones()
                && estaSemana.volumen() < anterior.volumen() * (1 - UMBRAL_CAIDA_VOLUMEN);

        if (caidaRendimiento) {
            int caida = (int) Math.round(
                    (1 - estaSemana.volumen() / anterior.volumen()) * 100);
            return "Tu volumen bajó un " + caida + "% esta semana entrenando lo mismo "
                    + "que la anterior. Suele indicar que el cuerpo pide descanso.";
        }

        return null;
    }

    /* Criterio 3 — El usuario puede aceptar, posponer o ignorar.            */

    @Transactional
    public SemanaDescarga aceptar(Long usuarioId, Long semanaId) {
        SemanaDescarga semana = buscar(usuarioId, semanaId);

        semana.setEstado(SemanaDescarga.ACEPTADA);
        semana.setInicio(LocalDate.now());
        semana.setFin(LocalDate.now().plusDays(6));

        return semanaDescargaRepository.save(semana);
    }

    @Transactional
    public SemanaDescarga posponer(Long usuarioId, Long semanaId) {
        SemanaDescarga semana = buscar(usuarioId, semanaId);

        semana.setEstado(SemanaDescarga.POSPUESTA);
        semana.setReintentarDesde(LocalDate.now().plusDays(7));

        return semanaDescargaRepository.save(semana);
    }

    @Transactional
    public SemanaDescarga ignorar(Long usuarioId, Long semanaId) {
        SemanaDescarga semana = buscar(usuarioId, semanaId);

        semana.setEstado(SemanaDescarga.IGNORADA);
        // Se ignora del todo: no se vuelve a sugerir en un mes.
        semana.setReintentarDesde(LocalDate.now().plusDays(30));

        return semanaDescargaRepository.save(semana);
    }

    /* Criterio 4 — Durante el deload la rutina baja volumen e intensidad.   */

    /** Semana de descarga activa hoy, si la hay. */
    @Transactional(readOnly = true)
    public Optional<SemanaDescarga> descargaActiva(Long usuarioId) {
        return semanaDescargaRepository
                .findByUsuarioIdAndEstadoOrderByDetectadaEnDesc(
                        usuarioId, SemanaDescarga.ACEPTADA)
                .stream()
                .filter(SemanaDescarga::estaActiva)
                .findFirst();
    }

    /** Factor de volumen a aplicar hoy: 0.6 en descarga, 1.0 el resto. */
    @Transactional(readOnly = true)
    public double factorVolumen(Long usuarioId) {
        return descargaActiva(usuarioId).isPresent()
                ? SemanaDescarga.FACTOR_VOLUMEN
                : 1.0;
    }

    /** Series ajustadas para hoy, mínimo 1 cuando había alguna. */
    public int seriesAjustadas(Integer seriesOriginales, double factor) {
        if (seriesOriginales == null || seriesOriginales <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(seriesOriginales * factor));
    }

    /** Estado completo para la vista. */
    @Transactional(readOnly = true)
    public EstadoDeload estado(Long usuarioId) {
        SemanaDescarga semana = semanaDescargaRepository
                .findFirstByUsuarioIdOrderByDetectadaEnDesc(usuarioId)
                .orElse(null);

        boolean activa = semana != null && semana.estaActiva();
        boolean pendiente = semana != null && semana.estaPendiente();

        return new EstadoDeload(semana, analizarUltimasSemanas(usuarioId), activa, pendiente);
    }

    private SemanaDescarga buscar(Long usuarioId, Long semanaId) {
        SemanaDescarga semana = semanaDescargaRepository.findById(semanaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe esa sugerencia de descarga."));

        if (!semana.getUsuarioId().equals(usuarioId)) {
            throw new IllegalArgumentException("Esa sugerencia no es tuya.");
        }

        return semana;
    }

    /** Días transcurridos desde el inicio de la descarga, para la vista. */
    public long diasDeDescarga(SemanaDescarga semana) {
        if (semana == null || semana.getInicio() == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(semana.getInicio(), LocalDate.now()) + 1;
    }
}
