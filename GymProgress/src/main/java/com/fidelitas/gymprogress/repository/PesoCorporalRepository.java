package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.PesoCorporal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesoCorporalRepository extends JpaRepository<PesoCorporal, Long> {

    //todas las pesadas del usuario, de la más reciente a la más vieja
    List<PesoCorporal> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
    //List y no Optional porque el usuario se pesa muchas veces
    //el orden descendente sirve para las dos cosas que hace la app: el peso
    //actual es el primer elemento de la lista, y el gráfico usa la lista entera

    //el registro de un día concreto, si existe
    Optional<PesoCorporal> findByUsuarioIdAndFecha(Long usuarioId, LocalDate fecha);
    //sirve para no duplicar la pesada del mismo día

    //un registro por su id, pero comprobando que sea del usuario
    Optional<PesoCorporal> findByIdAndUsuarioId(Long id, Long usuarioId);
    /**
     * fijarse en el "AndUsuarioId", no basta con buscar por id. si se buscara
     * solo por id, cualquiera podría borrar el peso de otra persona cambiando
     * el número en el formulario. al exigir que además sea suyo, el registro
     * de otro simplemente no aparece y no se borra nada
     */
}
