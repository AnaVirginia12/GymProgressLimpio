package com.fidelitas.gymprogress.service;

import com.fidelitas.gymprogress.domain.Ejercicio;
import com.fidelitas.gymprogress.repository.EjercicioRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class EjercicioService {

    private final EjercicioRepository ejercicioRepository;

    public EjercicioService(EjercicioRepository ejercicioRepository) {
        this.ejercicioRepository = ejercicioRepository;
    }

    public List<Ejercicio> listar() {
        return ejercicioRepository.findAllByOrderByNombreAsc();
    }

    public List<Ejercicio> buscar(String texto, String tipo) {
        String textoFiltro = (texto == null || texto.isBlank()) ? null : texto.trim();
        String tipoFiltro = (tipo == null || tipo.isBlank()) ? null : tipo;
        return ejercicioRepository.buscar(textoFiltro, tipoFiltro);
    }

    public Optional<Ejercicio> obtenerPorId(Long id) {
        return ejercicioRepository.findById(id);
    }

    public Ejercicio crear(Ejercicio ejercicio) {
        validar(ejercicio);
        ejercicio.setId(null);
        ejercicio.setFavorito(false);
        return ejercicioRepository.save(ejercicio);
    }

    public Ejercicio actualizar(Long id, Ejercicio datos) {
        Ejercicio existente = ejercicioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ejercicio no encontrado."));

        validar(datos);

        existente.setNombre(datos.getNombre());
        existente.setGrupoMuscular(datos.getGrupoMuscular());
        existente.setTipoEntrenamiento(datos.getTipoEntrenamiento());
        existente.setDescripcion(datos.getDescripcion());
        existente.setMediaUrl(datos.getMediaUrl());
        existente.setRequiereEquipo(datos.getRequiereEquipo());
        existente.setTempoExcentrico(datos.getTempoExcentrico());
        existente.setTempoPausaAbajo(datos.getTempoPausaAbajo());
        existente.setTempoConcentrico(datos.getTempoConcentrico());
        existente.setTempoPausaArriba(datos.getTempoPausaArriba());
        // favorito se preserva; solo cambia vía alternarFavorito()

        return ejercicioRepository.save(existente);
    }

    public void eliminar(Long id) {
        ejercicioRepository.deleteById(id);
    }

    public Ejercicio alternarFavorito(Long id) {
        Ejercicio ejercicio = ejercicioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ejercicio no encontrado."));
        ejercicio.setFavorito(!Boolean.TRUE.equals(ejercicio.getFavorito()));
        return ejercicioRepository.save(ejercicio);
    }

    /*
     * Alternativas sin equipamiento para los grupos musculares de un
     * ejercicio de gimnasio.
     *
     * grupoMuscular puede traer varios grupos separados por coma, así que se
     * busca por cada uno y se juntan los resultados sin repetir.
     */
    public List<Ejercicio> alternativasEnCasa(Ejercicio original) {
        return buscarAlternativas(original, true);
    }

    /*
     * HU31 — Alternativas del mismo grupo muscular para sustituir un
     * ejercicio durante el entrenamiento. Aquí sí se admite equipo, porque
     * el usuario puede estar en el gimnasio y solo querer cambiarlo.
     */
    public List<Ejercicio> alternativasMismoGrupo(Ejercicio original) {
        return buscarAlternativas(original, false);
    }

    private List<Ejercicio> buscarAlternativas(Ejercicio original, boolean soloSinEquipo) {
        if (original == null || original.getGrupoMuscular() == null) {
            return List.of();
        }

        Map<Long, Ejercicio> encontrados = new LinkedHashMap<>();

        for (String grupo : original.getGrupoMuscular().split(",")) {
            String limpio = grupo.trim();
            if (limpio.isEmpty()) {
                continue;
            }

            List<Ejercicio> parciales = soloSinEquipo
                    ? ejercicioRepository.alternativasSinEquipo(limpio, original.getId())
                    : ejercicioRepository.alternativasMismoGrupo(limpio, original.getId());

            for (Ejercicio e : parciales) {
                encontrados.putIfAbsent(e.getId(), e);
            }
        }

        return new ArrayList<>(encontrados.values());
    }

    /*
     * Tempo recomendado según el tipo de ejercicio y el programa.
     *
     * Si el ejercicio ya trae su propio tempo se respeta; si no, se sugiere
     * uno según el programa: en fuerza la fase excéntrica es más corta y con
     * pausa abajo, en hipertrofia se alarga la excéntrica, y en resistencia
     * se busca un ritmo continuo.
     */
    public int[] tempoRecomendado(Ejercicio ejercicio, String programa) {
        if (ejercicio != null && ejercicio.tieneTempo()) {
            return new int[] {
                valor(ejercicio.getTempoExcentrico()),
                valor(ejercicio.getTempoPausaAbajo()),
                valor(ejercicio.getTempoConcentrico()),
                valor(ejercicio.getTempoPausaArriba())
            };
        }

        String tipo = ejercicio == null ? null : ejercicio.getTipoEntrenamiento();

        if ("Cardio".equalsIgnoreCase(tipo)) {
            return new int[] {1, 0, 1, 0};
        }

        if (programa == null) {
            return new int[] {2, 1, 1, 0};
        }

        return switch (programa) {
            case "Fuerza" -> new int[] {2, 1, 1, 1};
            case "Hipertrofia" -> new int[] {3, 1, 1, 0};
            case "Resistencia" -> new int[] {2, 0, 1, 0};
            default -> new int[] {2, 1, 1, 0};
        };
    }

    private int valor(Integer v) {
        return v == null ? 0 : v;
    }

    private void validar(Ejercicio ejercicio) {
        if (ejercicio.getNombre() == null || ejercicio.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del ejercicio es obligatorio.");
        }
        if (ejercicio.getGrupoMuscular() == null || ejercicio.getGrupoMuscular().isBlank()) {
            throw new IllegalArgumentException("El grupo muscular es obligatorio.");
        }
        if (ejercicio.getTipoEntrenamiento() == null || ejercicio.getTipoEntrenamiento().isBlank()) {
            throw new IllegalArgumentException("El tipo de entrenamiento es obligatorio.");
        }
    }
}
