package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.CambioEjercicio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CambioEjercicioRepository
        extends JpaRepository<CambioEjercicio, Long> {

    //los cambios de un entrenamiento, o sea las omisiones y sustituciones,
    //en orden cronológico
    List<CambioEjercicio> findBySesionIdOrderByRegistradoEnAsc(Long sesionId);
    //es lo que alimenta el bloque "Ajustes de la rutina" del resumen final
}
