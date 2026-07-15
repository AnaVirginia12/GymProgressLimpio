package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.SerieRegistrada;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SerieRegistradaRepository extends JpaRepository<SerieRegistrada, Long> {

    List<SerieRegistrada> findBySesionIdOrderByRegistradaEnAsc(Long sesionId);

    /**
     * Mejor marca histórica (PR) de un ejercicio para un usuario:
     * la serie con mayor peso × repeticiones.
     */
    @Query("""
        SELECT s FROM SerieRegistrada s
        WHERE s.ejercicioId = :ejercicioId
          AND s.sesion.usuarioId = :usuarioId
        ORDER BY (s.pesoKg * s.repeticiones) DESC
        LIMIT 1
    """)
    java.util.Optional<SerieRegistrada> findPrByEjercicioIdAndUsuarioId(
            @Param("ejercicioId") Long ejercicioId,
            @Param("usuarioId") Long usuarioId
    );

    /**
     * Todas las series de un ejercicio para un usuario (para gráfico de progreso).
     */
    @Query("""
        SELECT s FROM SerieRegistrada s
        WHERE s.ejercicioId = :ejercicioId
          AND s.sesion.usuarioId = :usuarioId
        ORDER BY s.registradaEn ASC
    """)
    List<SerieRegistrada> findByEjercicioIdAndUsuarioId(
            @Param("ejercicioId") Long ejercicioId,
            @Param("usuarioId") Long usuarioId
    );
}
