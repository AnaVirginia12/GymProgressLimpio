package com.fidelitas.gymprogress.repository.rutina;

import com.fidelitas.gymprogress.domain.rutina.Rutina;
import com.fidelitas.gymprogress.domain.rutina.Rutina;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RutinaRepository extends JpaRepository<Rutina, Long> {

    List<Rutina> findByUsuarioIdOrderByCreadaEnDesc(Long usuarioId);

    Optional<Rutina> findFirstByUsuarioIdAndActivaTrueOrderByCreadaEnDesc(
            Long usuarioId
    );
}