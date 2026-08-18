package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.PesoCorporal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesoCorporalRepository extends JpaRepository<PesoCorporal, Long> {

    List<PesoCorporal> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    Optional<PesoCorporal> findByUsuarioIdAndFecha(Long usuarioId, LocalDate fecha);

    Optional<PesoCorporal> findByIdAndUsuarioId(Long id, Long usuarioId);
}
