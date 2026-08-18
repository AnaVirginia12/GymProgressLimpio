package com.fidelitas.gymprogress.service.rutina;

import com.fidelitas.gymprogress.domain.rutina.Rutina;
import com.fidelitas.gymprogress.domain.rutina.RutinaEjercicio;
import com.fidelitas.gymprogress.repository.rutina.RutinaRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RutinaService {

    private final RutinaRepository rutinaRepository;

    public RutinaService(RutinaRepository rutinaRepository) {
        this.rutinaRepository = rutinaRepository;
    }

    public List<Rutina> listarRutinas(Long usuarioId) {
        return rutinaRepository
                .findByUsuarioIdOrderByCreadaEnDesc(usuarioId);
    }

    /*
     * Repeticiones y descanso que le corresponden a cada programa.
     *
     * Los valores son los que aparecen descritos en la pantalla de
     * programas: fuerza trabaja con pocas repeticiones y descansos largos,
     * resistencia al revés, e hipertrofia queda en el medio.
     */
    public record ParametrosPrograma(int repsObjetivo, int descansoSeg) {
    }

    public static ParametrosPrograma parametrosDe(String programa) {
        if (programa == null) {
            return new ParametrosPrograma(10, 60);
        }

        return switch (programa) {
            case "Fuerza" -> new ParametrosPrograma(5, 180);
            case "Hipertrofia" -> new ParametrosPrograma(10, 90);
            case "Resistencia" -> new ParametrosPrograma(18, 45);
            default -> new ParametrosPrograma(10, 60);
        };
    }

    /*
     * Resultado de cambiar de programa: la rutina y cuántos ejercicios se
     * reajustaron, para poder avisarle al usuario en la pantalla.
     */
    public record ResultadoPrograma(Rutina rutina, int ejerciciosAjustados) {
    }

    /*
     * Cambia el programa de la rutina activa.
     *
     * Los ejercicios se mantienen: un press de banca sirve igual para
     * fuerza que para resistencia, y borrarlos haría perder el historial
     * de progreso de cada uno. Lo que sí cambia son las repeticiones
     * objetivo y el descanso entre series, que es justamente lo que
     * diferencia a un programa de otro.
     *
     * Solo se reajusta cuando el programa cambia de verdad, para no pisar
     * los valores cada vez que el usuario vuelve a entrar a la pantalla.
     */
    @Transactional
    public ResultadoPrograma seleccionarPrograma(Long usuarioId, String programa) {
        Rutina rutina = rutinaRepository
                .findFirstByUsuarioIdAndActivaTrueOrderByCreadaEnDesc(usuarioId)
                .orElseGet(Rutina::new);

        String programaAnterior = rutina.getPrograma();

        rutina.setUsuarioId(usuarioId);
        rutina.setNombre(generarNombre(programa));
        rutina.setTipo("Gimnasio");
        rutina.setPrograma(programa);
        rutina.setActiva(true);

        int ajustados = 0;

        if (!Objects.equals(programaAnterior, programa)) {
            ajustados = aplicarParametrosDelPrograma(rutina, programa);
        }

        return new ResultadoPrograma(rutinaRepository.save(rutina), ajustados);
    }

    /*
     * Le pone a cada ejercicio de la rutina las repeticiones y el descanso
     * del programa nuevo. Devuelve cuántos ejercicios se tocaron.
     */
    private int aplicarParametrosDelPrograma(Rutina rutina, String programa) {
        List<RutinaEjercicio> ejercicios = rutina.getEjercicios();

        if (ejercicios == null || ejercicios.isEmpty()) {
            return 0;
        }

        ParametrosPrograma parametros = parametrosDe(programa);

        for (RutinaEjercicio detalle : ejercicios) {
            detalle.setRepsObjetivo(parametros.repsObjetivo());
            detalle.setDescansoSeg(parametros.descansoSeg());
        }

        return ejercicios.size();
    }

    @Transactional(readOnly = true)
    public Rutina obtenerRutinaActiva(Long usuarioId) {
        return rutinaRepository
                .findFirstByUsuarioIdAndActivaTrueOrderByCreadaEnDesc(usuarioId)
                .orElse(null);
    }

    @Transactional
    public void agregarEjercicio(
            Long usuarioId,
            Long ejercicioId,
            Integer series,
            Integer repsObjetivo,
            Integer descansoSeg
    ) {
        Rutina rutina = rutinaRepository
                .findFirstByUsuarioIdAndActivaTrueOrderByCreadaEnDesc(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Primero selecciona un programa de entrenamiento."
                ));

        if (ejercicioId == null) {
            throw new IllegalArgumentException("Selecciona un ejercicio.");
        }

        /*
         * Si el usuario no escribe reps o descanso, se toman los del
         * programa que tiene elegido en vez de un valor fijo.
         */
        ParametrosPrograma parametros = parametrosDe(rutina.getPrograma());

        RutinaEjercicio detalle = new RutinaEjercicio();
        detalle.setEjercicioId(ejercicioId);
        detalle.setOrden(siguienteOrden(rutina));
        detalle.setSeries(valorPositivo(series, 3));
        detalle.setRepsObjetivo(valorPositivo(repsObjetivo, parametros.repsObjetivo()));
        detalle.setDescansoSeg(valorPositivo(descansoSeg, parametros.descansoSeg()));

        rutina.agregarEjercicio(detalle);
        rutinaRepository.save(rutina);
    }

    @Transactional
    public void sustituirEjercicio(
            Long usuarioId,
            Long rutinaEjercicioId,
            Long nuevoEjercicioId
    ) {
        Rutina rutina = rutinaRepository
                .findFirstByUsuarioIdAndActivaTrueOrderByCreadaEnDesc(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay una rutina activa."
                ));

        if (nuevoEjercicioId == null) {
            throw new IllegalArgumentException("Selecciona el ejercicio de reemplazo.");
        }

        RutinaEjercicio detalle = rutina.getEjercicios()
                .stream()
                .filter(e -> e.getId().equals(rutinaEjercicioId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ese ejercicio no está en tu rutina."
                ));

        detalle.setEjercicioId(nuevoEjercicioId);
        rutinaRepository.save(rutina);
    }

    @Transactional
    public void eliminarEjercicio(Long usuarioId, Long rutinaEjercicioId) {
        Rutina rutina = rutinaRepository
                .findFirstByUsuarioIdAndActivaTrueOrderByCreadaEnDesc(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay una rutina activa."
                ));

        rutina.getEjercicios()
                .stream()
                .filter(e -> e.getId().equals(rutinaEjercicioId))
                .findFirst()
                .ifPresent(rutina::eliminarEjercicio);

        reordenar(rutina);
        rutinaRepository.save(rutina);
    }

    private int siguienteOrden(Rutina rutina) {
        return rutina.getEjercicios() == null
                ? 1
                : rutina.getEjercicios().size() + 1;
    }

    private void reordenar(Rutina rutina) {
        List<RutinaEjercicio> ejercicios = rutina.getEjercicios();
        for (int i = 0; i < ejercicios.size(); i++) {
            ejercicios.get(i).setOrden(i + 1);
        }
    }

    public int calcularDuracionEstimadaMinutos(Rutina rutina) {
        if (rutina == null) {
            return 0;
        }

        List<RutinaEjercicio> ejercicios = rutina.getEjercicios();

        /*
         * Mientras no existan ejercicios asignados, se utiliza una
         * estimación básica según el programa elegido.
         */
        if (ejercicios == null || ejercicios.isEmpty()) {
            return obtenerDuracionBasePorPrograma(rutina.getPrograma());
        }

        double totalSegundos = 0;

        for (RutinaEjercicio ejercicio : ejercicios) {
            int series = valorPositivo(ejercicio.getSeries(), 1);
            int repeticiones = valorPositivo(
                    ejercicio.getRepsObjetivo(),
                    1
            );

            int descansoSegundos = valorPositivo(
                    ejercicio.getDescansoSeg(),
                    60
            );

            double segundosPorRepeticion =
                    valorTempo(ejercicio.getTempoBajada())
                    + valorTempo(ejercicio.getTempoFondo())
                    + valorTempo(ejercicio.getTempoSubida())
                    + valorTempo(ejercicio.getTempoTope());

            /*
             * Si todavía no se definió el tempo, se estiman
             * 4 segundos por repetición.
             */
            if (segundosPorRepeticion <= 0) {
                segundosPorRepeticion = 4;
            }

            double tiempoEjecucion =
                    series * repeticiones * segundosPorRepeticion;

            double tiempoDescanso =
                    Math.max(series - 1, 0) * descansoSegundos;

            /*
             * Se añade un minuto aproximado para preparar o cambiar
             * de ejercicio.
             */
            double tiempoTransicion = 60;

            totalSegundos +=
                    tiempoEjecucion
                    + tiempoDescanso
                    + tiempoTransicion;
        }

        return (int) Math.ceil(totalSegundos / 60);
    }

    private int obtenerDuracionBasePorPrograma(String programa) {
        if (programa == null) {
            return 45;
        }

        return switch (programa) {
            case "Fuerza" -> 60;
            case "Hipertrofia" -> 55;
            case "Resistencia" -> 45;
            default -> 45;
        };
    }

    private int valorPositivo(Integer valor, int valorPredeterminado) {
        return valor != null && valor > 0
                ? valor
                : valorPredeterminado;
    }

    private double valorTempo(Double valor) {
        return valor != null && valor > 0
                ? valor
                : 0;
    }

    private String generarNombre(String programa) {
        return switch (programa) {
            case "Fuerza" -> "Programa de fuerza";
            case "Hipertrofia" -> "Programa de hipertrofia";
            case "Resistencia" -> "Programa de resistencia";
            default -> "Rutina personalizada";
        };
    }
}