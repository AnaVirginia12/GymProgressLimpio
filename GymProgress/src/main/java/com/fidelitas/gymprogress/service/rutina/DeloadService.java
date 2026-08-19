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
 * Detección automática de semana de descarga (deload).
 *
 * Analiza las 4 últimas semanas de entrenamiento y busca señales de fatiga
 * acumulada. Si las encuentra, deja una sugerencia que el usuario puede
 * aceptar, posponer o ignorar. Mientras la descarga está activa, la rutina
 * se muestra con menos volumen e intensidad.
 */
@Service
public class DeloadService {

    //estas tres constantes son toda la política de detección. están arriba, con
    //nombre y comentario, en vez de dispersas por el código
    private static final int SEMANAS_ANALIZADAS = 4;
    //aunque se analizan 4 semanas, para que se dispare una sugerencia bastan 2
    //semanas con datos

    //A partir de este porcentaje de series al fallo se considera fatiga alta
    private static final double UMBRAL_FALLO = 0.30;

    //Caída de volumen respecto a la semana anterior que se considera bajón
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

    //Resumen de una semana del análisis
    public record ResumenSemana(
            int numero,
            double volumen,
            int series,
            int seriesAlFallo,
            int sesiones
    ) {
        public double porcentajeFallo() {
            /**
             * el (double) es el detalle más importante de la clase.
             * 'seriesAlFallo' y 'series' son los dos int, y sin ese cast java
             * haría división entera:
             *   44 / 126          daría 0, se pierde todo
             *   (double) 44 / 126 da 0.349, que es lo correcto
             *
             * sin el cast esto devolvería 0 siempre, la comparación 0 > 0.30
             * daría false y la sugerencia de descarga no se dispararía nunca.
             * un solo cast sostiene toda la funcionalidad
             */
            //y el series == 0 evita dividir por cero
            return series == 0 ? 0 : (double) seriesAlFallo / series;
        }
    }

    //Lo que necesita la vista para pintar el estado del deload
    //todo lo que la pantalla necesita, en un solo objeto. comparado con el
    //Map<String,Object> de SesionService, este record es mejor: tiene tipos y
    //el compilador comprueba los nombres
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
     * Analiza rendimiento y fatiga de las últimas 4 semanas.
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
            //ventanas de una semana hacia atrás. si hoy es 18 de agosto:
            //  semana 1 va del 11 al 18, o sea esta semana
            //  semana 2 va del 4 al 11
            //  semana 3 va del 28 de julio al 4
            //  semana 4 va del 21 al 28 de julio
            LocalDate desde = hoy.minusDays(7L * semana);
            LocalDate hasta = hoy.minusDays(7L * (semana - 1));

            double volumen = 0;
            int series = 0;
            int alFallo = 0;
            int cuantasSesiones = 0;

            for (Sesion sesion : sesiones) {
                LocalDate fecha = sesion.getIniciadaEn().toLocalDate();

                /**
                 * esta es la condición para descartar: si es anterior al inicio,
                 * o si no es anterior al fin, la saltea. o sea que se queda con
                 * las fechas donde desde <= fecha < hasta
                 *
                 * la asimetría es a propósito, el extremo de abajo se incluye y
                 * el de arriba no: eso impide que un mismo día se cuente en dos
                 * semanas. el 11 de agosto es el 'hasta' de la semana 1, que se
                 * excluye, y el 'desde' de la semana 2, que se incluye
                 */
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
                    //acá se conecta todo el proyecto: el falloMuscular que el
                    //usuario marca con una casilla en la pantalla de
                    //entrenamiento es lo que termina disparando la sugerencia
                    if (Boolean.TRUE.equals(serie.getFalloMuscular())) {
                        alFallo++;
                    }
                }
            }

            resumen.add(new ResumenSemana(semana, volumen, series, alFallo, cuantasSesiones));
        }

        return resumen;
    }

  
    @Transactional
    public Optional<SemanaDescarga> evaluar(Long usuarioId) {
        Optional<SemanaDescarga> ultima =
                semanaDescargaRepository.findFirstByUsuarioIdOrderByDetectadaEnDesc(usuarioId);

        if (ultima.isPresent()) {
            SemanaDescarga s = ultima.get();

            //primer filtro: si ya hay una sugerencia esperando respuesta, o una
            //descarga corriendo, devuelve esa sin crear otra. sin esto, cada
            //visita a la pantalla crearía una sugerencia nueva
            if (s.estaPendiente() || s.estaActiva()) {
                return ultima;
            }

            boolean aplazada = s.getReintentarDesde() != null
                    && LocalDate.now().isBefore(s.getReintentarDesde());

            //segundo filtro: si el usuario dijo "ahora no", se respeta hasta la
            //fecha de reintentarDesde. sin esto la app volvería a sugerirlo al
            //día siguiente y sería insoportable
            if (aplazada) {
                return Optional.empty();
            }
        }

        List<ResumenSemana> semanas = analizarUltimasSemanas(usuarioId);

        long semanasConDatos = semanas.stream().filter(s -> s.sesiones() > 0).count();
        //tercer filtro: hacen falta al menos 2 semanas con entrenamientos. no se
        //puede detectar fatiga acumulada en alguien que empezó anteayer
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
        /**
         * primera señal: más del 30% de series al fallo dos semanas seguidas
         *
         * el "dos seguidas" es la clave. una semana dura es normal, a veces uno
         * se exige más. dos seguidas ya es un patrón, y eso es lo que distingue
         * la fatiga acumulada de un mal día
         */
        boolean fatigaAlta = estaSemana.series() > 0
                && anterior.series() > 0
                && estaSemana.porcentajeFallo() > UMBRAL_FALLO
                && anterior.porcentajeFallo() > UMBRAL_FALLO;

        if (fatigaAlta) {
            int porcentaje = (int) Math.round(estaSemana.porcentajeFallo() * 100);
            return "Llevas dos semanas seguidas llegando al fallo en más del "
                    + porcentaje + "% de tus series. Es una señal clara de fatiga acumulada.";
        }

        /**
         * segunda señal: el volumen bajó más de 15%, pero solo cuenta si
         * entrenaste igual o más veces que la semana anterior
         *
         * esa segunda condición es la más pensada del servicio: si entrenaste 2
         * días en vez de 4 es obvio que el volumen bajó, no estás fatigado, fuiste
         * menos al gimnasio. sin ella, la app te sugeriría descansar justo cuando
         * ya descansaste
         *
         * lo que la señal busca es lo preocupante: mismo esfuerzo, menos resultado
         */
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

    //El usuario puede aceptar, posponer o ignorar

    @Transactional
    public SemanaDescarga aceptar(Long usuarioId, Long semanaId) {
        SemanaDescarga semana = buscar(usuarioId, semanaId);

        semana.setEstado(SemanaDescarga.ACEPTADA);
        semana.setInicio(LocalDate.now());
        semana.setFin(LocalDate.now().plusDays(6));
        //son 6 y no 7 porque el día de inicio cuenta: del día 1 al día 7 hay 6
        //días de diferencia. combinado con el estaActiva() de la entidad, que
        //incluye los dos extremos, dan exactamente 7 días

        return semanaDescargaRepository.save(semana);
    }

    @Transactional
    public SemanaDescarga posponer(Long usuarioId, Long semanaId) {
        SemanaDescarga semana = buscar(usuarioId, semanaId);

        semana.setEstado(SemanaDescarga.POSPUESTA);
        semana.setReintentarDesde(LocalDate.now().plusDays(7));
        //7 días de silencio: "ahora no" es distinto de "no me interesa"

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

    //Semana de descarga activa hoy, si la hay
    @Transactional(readOnly = true)
    public Optional<SemanaDescarga> descargaActiva(Long usuarioId) {
        return semanaDescargaRepository
                .findByUsuarioIdAndEstadoOrderByDetectadaEnDesc(
                        usuarioId, SemanaDescarga.ACEPTADA)
                .stream()
                //el filtro se hace en java y no en la consulta porque "activa
                //hoy" depende de la fecha actual, que el sql no sabe
                .filter(SemanaDescarga::estaActiva)
                .findFirst();
    }

    //Factor de volumen a aplicar hoy: 0.6 en descarga, 1.0 el resto
    @Transactional(readOnly = true)
    public double factorVolumen(Long usuarioId) {
        return descargaActiva(usuarioId).isPresent()
                ? SemanaDescarga.FACTOR_VOLUMEN
                : 1.0;
    }

    //Series ajustadas para hoy, mínimo 1 cuando había alguna
    public int seriesAjustadas(Integer seriesOriginales, double factor) {
        if (seriesOriginales == null || seriesOriginales <= 0) {
            return 0;
        }
        //el Math.max con 1 garantiza que si el ejercicio tenía alguna serie, en
        //descarga siga teniendo al menos una. una semana de descarga es entrenar
        //suave, no dejar de entrenar
        return Math.max(1, (int) Math.round(seriesOriginales * factor));
    }

    //Estado completo para la vista
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

        //la misma protección que en AjusteRutinaService pero al revés: acá se
        //busca por id y después se comprueba el dueño. sin esto, cualquiera
        //podría aceptar o ignorar la descarga de otro cambiando el número de la url
        if (!semana.getUsuarioId().equals(usuarioId)) {
            throw new IllegalArgumentException("Esa sugerencia no es tuya.");
        }

        return semana;
    }

    //Días transcurridos desde el inicio de la descarga, para la vista
    public long diasDeDescarga(SemanaDescarga semana) {
        if (semana == null || semana.getInicio() == null) {
            return 0;
        }
        //el +1 porque el día de inicio cuenta: entre lunes y miércoles hay 2
        //días de diferencia, pero llevás 3 contando el lunes
        return ChronoUnit.DAYS.between(semana.getInicio(), LocalDate.now()) + 1;
    }
}
