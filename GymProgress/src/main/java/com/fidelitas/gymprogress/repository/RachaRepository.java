package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.Racha;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RachaRepository extends JpaRepository<Racha, Long> {

    Optional<Racha> findByUsuarioId(Long usuarioId);
    /**
     * devuelve Optional y no List justamente por el unique = true que tiene
     * 'Racha.usuarioId'. la firma del método refleja la regla del modelo: cada
     * usuario tiene como mucho una racha
     *
     * devuelve vacío para un usuario que nunca entrenó, y 'RachaService' lo
     * resuelve con orElseGet, creando la primera racha en ese momento
     */
}
