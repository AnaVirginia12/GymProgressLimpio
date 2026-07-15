package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.Racha;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RachaRepository extends JpaRepository<Racha, Long> {

    Optional<Racha> findByUsuarioId(Long usuarioId);
}
