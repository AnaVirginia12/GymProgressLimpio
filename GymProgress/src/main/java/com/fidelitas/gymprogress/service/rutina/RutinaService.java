package com.fidelitas.gymprogress.service.rutina;

import com.fidelitas.gymprogress.domain.rutina.Rutina;
import com.fidelitas.gymprogress.domain.rutina.RutinaEjercicio;
import com.fidelitas.gymprogress.repository.rutina.RutinaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RutinaService {

    private static final Long USUARIO_TEMPORAL_ID = 1L;

    private final RutinaRepository rutinaRepository;

    public RutinaService(RutinaRepository rutinaRepository) {
        this.rutinaRepository = rutinaRepository;
    }

    public List<Rutina> listarRutinas() {
        return rutinaRepository
                .findByUsuarioIdOrderByCreadaEnDesc(USUARIO_TEMPORAL_ID);
    }

    public Rutina seleccionarPrograma(String programa) {
        Rutina rutina = rutinaRepository
                .findFirstByUsuarioIdAndActivaTrueOrderByCreadaEnDesc(
                        USUARIO_TEMPORAL_ID
                )
                .orElseGet(Rutina::new);

        rutina.setUsuarioId(USUARIO_TEMPORAL_ID);
        rutina.setNombre(generarNombre(programa));
        rutina.setTipo("Gimnasio");
        rutina.setPrograma(programa);
        rutina.setActiva(true);

        return rutinaRepository.save(rutina);
    }

    @Transactional(readOnly = true)
    public Rutina obtenerRutinaActiva() {
        return rutinaRepository
                .findFirstByUsuarioIdAndActivaTrueOrderByCreadaEnDesc(
                        USUARIO_TEMPORAL_ID
                )
                .orElse(null);
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