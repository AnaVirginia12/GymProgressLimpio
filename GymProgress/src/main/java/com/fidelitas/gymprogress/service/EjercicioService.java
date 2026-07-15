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

    /** Devuelve todos los ejercicios ordenados por nombre. */
    public List<Ejercicio> listarTodos() {
        return ejercicioRepository.findAllByOrderByNombreAsc();
    }

    public Optional<Ejercicio> buscarPorId(Long id) {
        return ejercicioRepository.findById(id);
    }

    public Ejercicio guardar(Ejercicio ejercicio) {
        return ejercicioRepository.save(ejercicio);
    }

    public void eliminar(Long id) {
        ejercicioRepository.deleteById(id);
    }
}
