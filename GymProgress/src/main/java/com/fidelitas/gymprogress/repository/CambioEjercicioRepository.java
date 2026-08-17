package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.CambioEjercicio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CambioEjercicioRepository
        extends JpaRepository<CambioEjercicio, Long> {

    List<CambioEjercicio> findBySesionIdOrderByRegistradoEnAsc(Long sesionId);
}
