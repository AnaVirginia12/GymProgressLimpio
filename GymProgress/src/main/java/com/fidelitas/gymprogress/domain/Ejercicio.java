package com.fidelitas.gymprogress.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "ejercicio")
public class Ejercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    /** Grupo muscular principal: Pecho, Espalda, Piernas, Hombros, Bíceps, etc. */
    @Column(name = "grupo_muscular", nullable = false, length = 80)
    private String grupoMuscular;

    @Column(name = "tipo_entrenamiento", nullable = false, length = 30)
    private String tipoEntrenamiento; // Calistenia, Cardio, Gimnasio

    @Column(name = "media_url", length = 300)
    private String mediaUrl; // URL de imagen o GIF demostrativo

    @Column(nullable = false)
    private Boolean favorito = false;

    @Column(name = "requiere_equipo", nullable = false)
    private Boolean requiereEquipo = true;

    @Column(name = "tempo_excentrico")
    private Integer tempoExcentrico;

    @Column(name = "tempo_pausa_abajo")
    private Integer tempoPausaAbajo;

    @Column(name = "tempo_concentrico")
    private Integer tempoConcentrico;

    @Column(name = "tempo_pausa_arriba")
    private Integer tempoPausaArriba;

    public Ejercicio() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getGrupoMuscular() {
        return grupoMuscular;
    }

    public void setGrupoMuscular(String grupoMuscular) {
        this.grupoMuscular = grupoMuscular;
    }

    public String getTipoEntrenamiento() {
        return tipoEntrenamiento;
    }

    public void setTipoEntrenamiento(String tipoEntrenamiento) {
        this.tipoEntrenamiento = tipoEntrenamiento;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public Boolean getFavorito() {
        return favorito;
    }

    public void setFavorito(Boolean favorito) {
        this.favorito = favorito;
    }

    public Boolean getRequiereEquipo() {
        return requiereEquipo;
    }

    public void setRequiereEquipo(Boolean requiereEquipo) {
        this.requiereEquipo = requiereEquipo;
    }

    public Integer getTempoExcentrico() {
        return tempoExcentrico;
    }

    public void setTempoExcentrico(Integer tempoExcentrico) {
        this.tempoExcentrico = tempoExcentrico;
    }

    public Integer getTempoPausaAbajo() {
        return tempoPausaAbajo;
    }

    public void setTempoPausaAbajo(Integer tempoPausaAbajo) {
        this.tempoPausaAbajo = tempoPausaAbajo;
    }

    public Integer getTempoConcentrico() {
        return tempoConcentrico;
    }

    public void setTempoConcentrico(Integer tempoConcentrico) {
        this.tempoConcentrico = tempoConcentrico;
    }

    public Integer getTempoPausaArriba() {
        return tempoPausaArriba;
    }

    public void setTempoPausaArriba(Integer tempoPausaArriba) {
        this.tempoPausaArriba = tempoPausaArriba;
    }
    
    @Transient
    public String getTempoTexto() {
        if (!tieneTempo()) {
            return null;
        }
        return valor(tempoExcentrico) + "-" + valor(tempoPausaAbajo)
                + "-" + valor(tempoConcentrico) + "-" + valor(tempoPausaArriba);
    }

    @Transient
    public boolean tieneTempo() {
        return tempoExcentrico != null || tempoPausaAbajo != null
                || tempoConcentrico != null || tempoPausaArriba != null;
    }

    @Transient
    public int getSegundosPorRepeticion() {
        return valor(tempoExcentrico) + valor(tempoPausaAbajo)
                + valor(tempoConcentrico) + valor(tempoPausaArriba);
    }

    private int valor(Integer v) {
        return v == null ? 0 : v;
    }
}
