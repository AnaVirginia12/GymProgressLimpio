package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.SerieRegistrada;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SerieRegistradaRepository extends JpaRepository<SerieRegistrada, Long> {

    //las series de una sesión, en orden cronológico
    List<SerieRegistrada> findBySesionIdOrderByRegistradaEnAsc(Long sesionId);
    /**
     * acá hay algo sutil: 'SerieRegistrada' no tiene un campo llamado
     * 'sesionId', tiene un campo 'sesion' de tipo Sesion. Spring Data igual lo
     * entiende, interpreta "SesionId" como "el id del campo sesion" y navega
     * la relación solo
     */

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
    //s.sesion.usuarioId con el punto hace que JPQL salte a la tabla
    //relacionada y hibernate arma el JOIN solo, porque 'SerieRegistrada' no
    //guarda el usuarioId, lo guarda su 'Sesion'
    //
    //el ORDER BY (peso * reps) ordena por una operación, no por un campo:
    //calcula el volumen al vuelo y con el LIMIT 1 devuelve la mejor serie
    //
    //es más eficiente de lo que parece, porque la alternativa sería traer todas
    //las series a java y recorrerlas. así la base hace el trabajo y viaja una
    //sola fila por la red
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
    //igual que la de arriba pero sin LIMIT y ordenada ascendente por fecha:
    //todo el histórico en orden cronológico, que es lo que necesita un gráfico
    List<SerieRegistrada> findByEjercicioIdAndUsuarioId(
            @Param("ejercicioId") Long ejercicioId,
            @Param("usuarioId") Long usuarioId
    );
}
