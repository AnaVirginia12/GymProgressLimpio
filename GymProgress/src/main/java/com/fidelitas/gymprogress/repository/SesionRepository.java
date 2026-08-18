package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.Sesion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SesionRepository extends JpaRepository<Sesion, Long> {

    /** Sesión activa (no finalizada) del usuario. */
    Optional<Sesion> findFirstByUsuarioIdAndFinalizadaEnIsNullOrderByIniciadaEnDesc(
            Long usuarioId
    );

    /** Historial completo de sesiones finalizadas, más reciente primero. */
    List<Sesion> findByUsuarioIdAndFinalizadaEnIsNotNullOrderByIniciadaEnDesc(
            Long usuarioId
    );
}
