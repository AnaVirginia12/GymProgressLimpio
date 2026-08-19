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
    //el segundo se usa solo para buscar los récords históricos, porque las
    //series de la sesión actual salen de sesion.getSeries() gracias al @OneToMany

    public SesionService(
            SesionRepository sesionRepository,
            SerieRegistradaRepository serieRegistradaRepository
    ) {
        this.sesionRepository = sesionRepository;
        this.serieRegistradaRepository = serieRegistradaRepository;
    }

    /* Iniciar sesión de entrenamiento                                      */

    /**
     * Crea y persiste una nueva sesión para el usuario.
     * HU — Guardado automático: la sesión queda en BD desde el primer instante.
     */
    @Transactional
    public Sesion iniciar(Long usuarioId, Long rutinaId) {
        /**
         * este método es idempotente, o sea que llamarlo una vez o cinco da el
         * mismo resultado: si ya hay una sesión abierta la devuelve en vez de
         * crear otra
         *
         * importa porque el usuario puede apretar dos veces "comenzar", darle
         * F5 o volver atrás con el navegador. sin esto, cada clic crearía una
         * sesión nueva y las series quedarían repartidas entre varias
         */
        // Si ya hay una sesión activa, la devuelve sin crear otra
        Optional<Sesion> activa = sesionRepository
                .findFirstByUsuarioIdAndFinalizadaEnIsNullOrderByIniciadaEnDesc(usuarioId);
        if (activa.isPresent()) {
            return activa.get();
        }

        Sesion sesion = new Sesion();
        sesion.setUsuarioId(usuarioId);
        sesion.setRutinaId(rutinaId);
        //no se le pone iniciadaEn porque ya viene puesto por el
        //= LocalDateTime.now() de la entidad, y finalizadaEn queda en null,
        //que es justamente lo que la marca como activa
        return sesionRepository.save(sesion);
    }

    /* Sesión activa                                                        */

    //el readOnly acá no es decorativo, hace falta: la Sesion que devuelve
    //tiene una lista 'series' perezosa, y si el controlador la lee después de
    //que se cerró la transacción, salta LazyInitializationException
    @Transactional(readOnly = true)
    public Optional<Sesion> obtenerActiva(Long usuarioId) {
        return sesionRepository
                .findFirstByUsuarioIdAndFinalizadaEnIsNullOrderByIniciadaEnDesc(usuarioId);
    }

    /* Guardar serie (con fallo muscular)                                   */

    /**
     * Almacena ejercicio, peso, repeticiones y fecha de cada serie.
     * Marcar serie como fallo muscular.
     * Guardado automático: cada serie se persiste de inmediato.
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
        /**
         * una casilla de html sin marcar no manda nada, así que el parámetro
         * llega en null cuando el usuario no la marcó:
         *   si llega true   true && true   queda true
         *   si llega false  true && false  queda false
         *   si llega null   corta en false y queda false
         *
         * sin esta conversión se guardaría null, y la columna es nullable = false
         */
        serie.setRegistradaEn(LocalDateTime.now());

        sesion.agregarSerie(serie);
        //agregarSerie mantiene la relación bidireccional. si acá se escribiera
        //sesion.getSeries().add(serie), la serie quedaría con sesion = null y
        //el INSERT fallaría por el nullable = false de sesion_id
        sesionRepository.save(sesion); // cascade guarda la serie automáticamente
        //se guarda la sesión, no la serie: como la relación tiene cascade ALL,
        //hibernate guarda también las series nuevas que encuentre en la lista
        return serie;
    }

    /* Finalizar sesión y calcular resumen                                  */

    /**
     * Resumen al finalizar entrenamiento (volumen, tiempo, PRs).
     * Cierra la sesión y retorna un mapa con las métricas.
     */
    @Transactional
    public Map<String, Object> finalizar(Long sesionId, Long usuarioId) {
        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada."));

        sesion.setFinalizadaEn(LocalDateTime.now());
        //poner la fecha de fin es lo que hace que la sesión deje de estar activa
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
        //mapToDouble convierte cada serie en un número. es distinto de map:
        //devuelve un DoubleStream, que trae métodos numéricos como .sum()
        double volumenTotal = series.stream()
                .mapToDouble(s -> s.getPesoKg() != null && s.getRepeticiones() != null
                        ? s.getPesoKg() * s.getRepeticiones() : 0.0)
                .sum();

        // Duración en minutos
        //si la sesión ya está finalizada usa esa fecha, y si no usa "ahora". eso
        //permite llamar a este método también con una sesión en curso, para
        //mostrar el tiempo transcurrido en vivo
        LocalDateTime fin = sesion.getFinalizadaEn() != null
                ? sesion.getFinalizadaEn() : LocalDateTime.now();
        long duracionMinutos = Duration.between(sesion.getIniciadaEn(), fin).toMinutes();

        /**
         * merge recibe tres cosas: una clave, un valor, y una función que dice
         * qué hacer si la clave ya existía
         *   si la clave no estaba, guarda el valor tal cual
         *   si ya estaba, llama a la función con el viejo y el nuevo y guarda
         *   lo que devuelva
         *
         * acá la función es "quedate con el de mayor volumen", así que al
         * terminar el bucle el mapa tiene, por cada ejercicio, la mejor serie
         * de hoy. por ejemplo con press de banca:
         *   serie 1: 80kg x 10 = 800  no estaba, guarda la serie 1
         *   serie 2: 85kg x 8  = 680  como 800 es mayor, conserva la serie 1
         *   serie 3: 90kg x 10 = 900  como 900 es mayor, guarda la serie 3
         */
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
            /**
             * el filter de la línea de abajo resuelve un problema que no es obvio
             *
             * el récord histórico se busca sobre todas las series del usuario, y
             * las de hoy ya están guardadas en la base porque se guardan una por
             * una apenas se registran. o sea que la propia serie de hoy puede
             * ser el "récord histórico"
             *
             * si no se excluyera, la comparación sería volumenHoy > volumenHoy,
             * que da false, y nunca se detectaría ningún récord. el bug sería
             * desconcertante: los récords no funcionarían nunca
             */
            boolean esPr = prHistorico
                    .filter(pr -> !pr.getSesion().getId().equals(sesion.getId()))
                    .map(pr -> volumenHoy > pr.getVolumen())
                    .orElse(true); // primer registro = PR automático

            if (esPr) {
                prs.put(entry.getValue().getEjercicioNombre(), entry.getValue().getPesoKg());
            }
        }

        /**
         * se devuelve un Map<String, Object>, o sea una bolsa de datos con
         * nombre. el controlador hace model.addAllAttributes(resumen) y todas
         * las claves quedan disponibles en el html
         *
         * es cómodo pero frágil: el Object obliga a hacer casts, y si te
         * equivocás al escribir una clave el compilador no dice nada. un record
         * sería más seguro, de hecho DeloadService ya usa records
         */
        Map<String, Object> resumen = new HashMap<>();
        resumen.put("sesion", sesion);
        resumen.put("volumenTotal", Math.round(volumenTotal * 10.0) / 10.0);
        //redondeo a 1 decimal: multiplicar por 10, redondear a entero y dividir
        //por 10.0. el punto cero es imprescindible, con /10 entero se perderían
        //los decimales
        resumen.put("duracionMinutos", duracionMinutos);
        resumen.put("totalSeries", series.size());
        resumen.put("prs", prs);
        return resumen;
    }

    
    //las sesiones finalizadas, de la más reciente a la más vieja
    @Transactional(readOnly = true)
    public List<Sesion> historial(Long usuarioId) {
        return sesionRepository
                .findByUsuarioIdAndFinalizadaEnIsNotNullOrderByIniciadaEnDesc(usuarioId);
    }
}
