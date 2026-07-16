package com.fidelitas.gymprogress.service;

import com.fidelitas.gymprogress.domain.Ejercicio;
import com.fidelitas.gymprogress.repository.EjercicioRepository;
import java.util.List;
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
