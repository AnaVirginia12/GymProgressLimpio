package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.PesoCorporal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesoCorporalRepository extends JpaRepository<PesoCorporal, Long> {

    List<PesoCorporal> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
}
