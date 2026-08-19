package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.Ejercicio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * El repositorio del catálogo de ejercicios. Es el único con consultas
 * escritas a mano, porque las de las alternativas son demasiado complicadas
 * para deducirlas del nombre del método
 */
public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {

    //SELECT * FROM ejercicio ORDER BY nombre ASC
    List<Ejercicio> findAllByOrderByNombreAsc();
    //el "AllBy" sin condiciones después se ve raro pero es la forma correcta de
    //decir "sin WHERE, pero con ORDER BY"

    //solo los favoritos. el "True" en el nombre genera WHERE favorito = true
    List<Ejercicio> findByFavoritoTrueOrderByNombreAsc();

    /**
     * @Query se usa cuando el nombre del método no alcanza. un método llamado
     * findByNombreContainingIgnoreCaseOrGrupoMuscular... sería impronunciable
     *
     * las triples comillas son un "text block", permiten escribir en varias
     * líneas sin llenar todo de \n y comillas
     *
     * ojo que esto es JPQL, no SQL: dice "FROM Ejercicio" (la clase, con
     * mayúscula) y "e.grupoMuscular" (el campo en camelCase), no la tabla ni la
     * columna. hibernate lo traduce a SQL usando lo que dicen las anotaciones
     */
    @Query("""
        SELECT e FROM Ejercicio e
        WHERE (:texto IS NULL
               OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(e.grupoMuscular) LIKE LOWER(CONCAT('%', :texto, '%')))
          AND (:tipo IS NULL OR e.tipoEntrenamiento = :tipo)
        ORDER BY e.nombre ASC
        """)
    //el truco del ":texto IS NULL OR ..." hace los filtros opcionales: si no
    //mandan texto, esa condición se cumple siempre. así un solo método sirve
    //para los cuatro casos: sin filtros, solo texto, solo tipo, o los dos
    //
    //los LOWER() de los dos lados hacen que "PRESS", "Press" y "press" den igual
    //y los % son comodines: coincide si aparece en cualquier parte del nombre
    List<Ejercicio> buscar(@Param("texto") String texto, @Param("tipo") String tipo);
    //@Param conecta el parámetro de java con el :texto de la consulta

    @Query("""
        SELECT e FROM Ejercicio e
        WHERE e.requiereEquipo = false
          AND (:excluirId IS NULL OR e.id <> :excluirId)
          AND (:grupo IS NULL
               OR LOWER(e.grupoMuscular) LIKE LOWER(CONCAT('%', :grupo, '%')))
        ORDER BY e.favorito DESC, e.nombre ASC
        """)
    //requiereEquipo = false es la condición dura, sin ella no existe la HU7
    //e.id <> :excluirId es para que no se proponga a sí mismo (<> es "distinto de")
    //el LIKE sobre grupoMuscular es porque el campo guarda varios grupos
    //separados por coma, como "Pecho, Tríceps", así que no se puede comparar
    //con = y hay que buscar si el grupo aparece dentro del texto
    //el ORDER BY favorito DESC pone primero los favoritos, porque true va antes
    //que false, y dentro de cada grupo los ordena alfabéticamente
    List<Ejercicio> alternativasSinEquipo(
            @Param("grupo") String grupo,
            @Param("excluirId") Long excluirId
    );

    @Query("""
        SELECT e FROM Ejercicio e
        WHERE (:excluirId IS NULL OR e.id <> :excluirId)
          AND (:grupo IS NULL
               OR LOWER(e.grupoMuscular) LIKE LOWER(CONCAT('%', :grupo, '%')))
        ORDER BY e.favorito DESC, e.nombre ASC
        """)
    //la misma consulta pero sin la condición de requiereEquipo, se usa cuando
    //estás en el gimnasio y solo querés cambiar un ejercicio porque la máquina
    //está ocupada: ahí te sirve cualquier otro del mismo grupo, use máquina o no
    List<Ejercicio> alternativasMismoGrupo(
            @Param("grupo") String grupo,
            @Param("excluirId") Long excluirId
    );
}
