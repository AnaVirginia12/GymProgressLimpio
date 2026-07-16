package com.fidelitas.gymprogress.repository.rutina;

import com.fidelitas.gymprogress.domain.rutina.RutinaEjercicio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RutinaEjercicioRepository
        extends JpaRepository<RutinaEjercicio, Long> {

    List<RutinaEjercicio> findByRutinaIdOrderByOrdenAsc(Long rutinaId);
}