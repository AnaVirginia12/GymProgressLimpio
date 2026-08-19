package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.Sesion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Los dos métodos de acá son la misma consulta con la condición invertida, y
 * juntos implementan la regla de 'Sesion': cuando finalizadaEn es null, esa
 * sesión está en curso ahora mismo
 */
public interface SesionRepository extends JpaRepository<Sesion, Long> {

    /** Sesión activa (no finalizada) del usuario. */
    Optional<Sesion> findFirstByUsuarioIdAndFinalizadaEnIsNullOrderByIniciadaEnDesc(
            Long usuarioId
    );
    /**
     * ese nombre kilométrico se lee por partes:
     *   find                  es un SELECT
     *   First                 es un LIMIT 1
     *   By                    empiezan las condiciones, o sea el WHERE
     *   UsuarioId             usuario_id = ?
     *   And                   AND
     *   FinalizadaEnIsNull    finalizada_en IS NULL
     *   OrderByIniciadaEnDesc ORDER BY iniciada_en DESC
     */
    //devuelve Optional porque hay como mucho una sesión en curso, o ninguna
    //el findFirst con el orden descendente es una defensa: si por algún error
    //quedaran dos sesiones abiertas, se queda con la más reciente y no revienta

    /** Historial completo de sesiones finalizadas, más reciente primero. */
    List<Sesion> findByUsuarioIdAndFinalizadaEnIsNotNullOrderByIniciadaEnDesc(
            Long usuarioId
    );
    //IsNotNull en vez de IsNull, o sea las que ya terminaron
    //devuelve List y no Optional porque el historial son muchas
}
