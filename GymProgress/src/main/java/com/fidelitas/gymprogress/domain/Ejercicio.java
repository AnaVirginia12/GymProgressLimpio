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

    /** Grupo muscular principal: Pecho, Espalda, Piernas, Hombros, Bíceps, etc. */
    @Column(name = "grupo_muscular", length = 80)
    private String grupoMuscular;

    /** Descripción o notas sobre la ejecución */
    @Column(length = 500)
    private String descripcion;

    public Ejercicio() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getGrupoMuscular() { return grupoMuscular; }
    public void setGrupoMuscular(String grupoMuscular) { this.grupoMuscular = grupoMuscular; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
