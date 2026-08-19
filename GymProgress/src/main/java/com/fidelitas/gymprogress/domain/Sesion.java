package com.fidelitas.gymprogress.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una sesión de entrenamiento (un día de gimnasio).
 * Se crea al "Iniciar rutina" y se cierra al finalizarla.
 */
@Entity //convierte esto de una clase de java común en una tabla
@Table(name = "sesion") //fija el nombre exacto de la tabla
public class Sesion {

    @Id //marca la llave primaria. toda @Entity necesita una
    @GeneratedValue(strategy = GenerationType.IDENTITY) //IDENTITY significa "el número lo pone mysql, no java", cuando un usuario nuevo se hace, se manda en null el id, mysql le asigna el siguiente número libre y lo devuelve
    private Long id; //Long de objeto

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "rutina_id")
    private Long rutinaId;

    @Column(name = "iniciada_en", nullable = false)
    private LocalDateTime iniciadaEn = LocalDateTime.now();//se pone al crear el objeto

    //Null mientras la sesión está activa
    //empieza en null y se llena al paretar el botón "finalizar entrenamiento"
    @Column(name = "finalizada_en")
    private LocalDateTime finalizadaEn;
    /**
     * 'SesionService' busca la sesión activa con 'fianlizadaEn IS NULL'.
     * el historial muestra solo las que tienen 'finalizadaEn' con valor.
     * 'DeloadService' solo muestra las finalizadas para su análisis.
     * La duración del entrenamiento es la dos
     */

    /**
     * Guardado automático del progreso:
     * La sesión persiste inmediatamente al crearse y se actualiza con cada serie.
     */
    @OneToMany( //una sesión tiene muchas series "'1':0"
        mappedBy = "sesion", //esta relación ya está definida del otro lado, en el campo 'sesion de la clase 'serieRegistrada', "soy el espejo, no dueño". evita que hibernate cree una tabla intermedia de más
            //la llave foránea está en una sola de las dos tablas: 'serie_registrada' tiene la columna 'sesion_id'. la tabla 'sesion' no tiene ninguna columna que apunte a las series
        cascade = CascadeType.ALL, //lo que le pasa a la sesión le pasa a sus series. si guardás la sesión, se guardan las series nuevas, si borrás la sesión, se borran
        //sin esto se tendría que guardar cada serie por separado, y borrar una sesión dejaría series huérfanas 
        orphanRemoval = true //si saco una serie de la lista se borra de toda la base
    )
    @OrderBy("registradaEn ASC") //cuando hibernate traiga las series, que las ordene por fecha de registro
    private List<SerieRegistrada> series = new ArrayList<>(); 

    //constructor vacío
    public Sesion() {
    }

    //setters n getters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getRutinaId() { return rutinaId; }
    public void setRutinaId(Long rutinaId) { this.rutinaId = rutinaId; }

    public LocalDateTime getIniciadaEn() { return iniciadaEn; }
    public void setIniciadaEn(LocalDateTime iniciadaEn) { this.iniciadaEn = iniciadaEn; }

    public LocalDateTime getFinalizadaEn() { return finalizadaEn; }
    public void setFinalizadaEn(LocalDateTime finalizadaEn) { this.finalizadaEn = finalizadaEn; }

    public List<SerieRegistrada> getSeries() { return series; }
    public void setSeries(List<SerieRegistrada> series) { this.series = series; }

    //Comodidad: agrega una serie manteniendo la relación bidireccional
    public void agregarSerie(SerieRegistrada serie) {
        series.add(serie); //la serie entra a la lista
        serie.setSesion(this); //la serie apunta de vuelta a la sesión. el this es "este objeto sesión en el que estamos"
    }

    public boolean estaActiva() {
        return finalizadaEn == null;
    }
}
