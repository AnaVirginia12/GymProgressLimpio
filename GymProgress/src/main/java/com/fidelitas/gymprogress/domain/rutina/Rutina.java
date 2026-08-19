package com.fidelitas.gymprogress.domain.rutina;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rutina")
public class Rutina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false, length = 80)
    private String programa;

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    @OneToMany(
        mappedBy = "rutina",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("orden ASC")
    private List<RutinaEjercicio> ejercicios = new ArrayList<>();

    public Rutina() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getPrograma() {
        return programa;
    }

    public void setPrograma(String programa) {
        this.programa = programa;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(LocalDateTime creadaEn) {
        this.creadaEn = creadaEn;
    }

    public List<RutinaEjercicio> getEjercicios() {
        return ejercicios;
    }

    public void setEjercicios(List<RutinaEjercicio> ejercicios) {
        this.ejercicios = ejercicios;
    }

    public void agregarEjercicio(RutinaEjercicio rutinaEjercicio) {
        ejercicios.add(rutinaEjercicio);
        rutinaEjercicio.setRutina(this);
    }

    public void eliminarEjercicio(RutinaEjercicio rutinaEjercicio) {
        ejercicios.remove(rutinaEjercicio);
        rutinaEjercicio.setRutina(null);
    }
}