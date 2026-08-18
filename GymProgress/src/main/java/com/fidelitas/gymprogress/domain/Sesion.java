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
    private LocalDateTime iniciadaEn = LocalDateTime.now();

    //Null mientras la sesión está activa
    @Column(name = "finalizada_en")
    private LocalDateTime finalizadaEn;

    /**
     * Guardado automático del progreso:
     * La sesión persiste inmediatamente al crearse y se actualiza con cada serie.
     */
    @OneToMany(
        mappedBy = "sesion",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("registradaEn ASC")
    private List<SerieRegistrada> series = new ArrayList<>();

    public Sesion() {
    }

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
        series.add(serie);
        serie.setSesion(this);
    }

    public boolean estaActiva() {
        return finalizadaEn == null;
    }
}
