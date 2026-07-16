package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.Ejercicio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {

    List<Ejercicio> findAllByOrderByNombreAsc();

    List<Ejercicio> findByFavoritoTrueOrderByNombreAsc();

    @Query("""
        SELECT e FROM Ejercicio e
        WHERE (:texto IS NULL
               OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(e.grupoMuscular) LIKE LOWER(CONCAT('%', :texto, '%')))
          AND (:tipo IS NULL OR e.tipoEntrenamiento = :tipo)
        ORDER BY e.nombre ASC
        """)
    List<Ejercicio> buscar(@Param("texto") String texto, @Param("tipo") String tipo);
}
