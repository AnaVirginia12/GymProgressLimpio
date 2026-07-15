package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.Ejercicio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {

    List<Ejercicio> findAllByOrderByNombreAsc();
}
