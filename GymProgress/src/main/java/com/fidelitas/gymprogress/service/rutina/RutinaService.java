package com.fidelitas.gymprogress.service.rutina;

import com.fidelitas.gymprogress.domain.rutina.Rutina;
import com.fidelitas.gymprogress.domain.rutina.RutinaEjercicio;
import com.fidelitas.gymprogress.repository.rutina.RutinaRepository;
import java.util.List;
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

    public Rutina seleccionarPrograma(Long usuarioId, String programa) {
        Rutina rutina = rutinaRepository
                .findFirstByUsuarioIdAndActivaTrueOrderByCreadaEnDesc(usuarioId)
                .orElseGet(Rutina::new);

        rutina.setUsuarioId(usuarioId);
        rutina.setNombre(generarNombre(programa));
        rutina.setTipo("Gimnasio");
        rutina.setPrograma(programa);
        rutina.setActiva(true);

        return rutinaRepository.save(rutina);
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

        RutinaEjercicio detalle = new RutinaEjercicio();
        detalle.setEjercicioId(ejercicioId);
        detalle.setOrden(siguienteOrden(rutina));
        detalle.setSeries(valorPositivo(series, 3));
        detalle.setRepsObjetivo(valorPositivo(repsObjetivo, 10));
        detalle.setDescansoSeg(valorPositivo(descansoSeg, 60));

        rutina.agregarEjercicio(detalle);
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