package com.fidelitas.gymprogress.repository.rutina;

import com.fidelitas.gymprogress.domain.rutina.SemanaDescarga;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemanaDescargaRepository extends JpaRepository<SemanaDescarga, Long> {

    // HU37 — La sugerencia o descarga más reciente del usuario.
    Optional<SemanaDescarga> findFirstByUsuarioIdOrderByDetectadaEnDesc(Long usuarioId);

    // HU37 — Todas las de un estado concreto, para saber si ya hay una activa.
    List<SemanaDescarga> findByUsuarioIdAndEstadoOrderByDetectadaEnDesc(
            Long usuarioId, String estado
    );
}
