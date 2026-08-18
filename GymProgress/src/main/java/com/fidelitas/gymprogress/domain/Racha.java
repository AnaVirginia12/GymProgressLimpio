package com.fidelitas.gymprogress.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Sistema de rachas de días consecutivos entrenando.
 * Se mantiene un único registro por usuario con el contador actual
 * y la racha máxima histórica.
 */
@Entity
@Table(name = "racha")
public class Racha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    //Días consecutivos actuales entrenando
    @Column(name = "dias_actuales", nullable = false)
    private Integer diasActuales = 0;

    //Máxima racha histórica alcanzada
    @Column(name = "dias_maximo", nullable = false)
    private Integer diasMaximo = 0;

    //Última fecha en que se registró un entrenamiento
    @Column(name = "ultimo_entrenamiento")
    private LocalDate ultimoEntrenamiento;

    public Racha() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Integer getDiasActuales() { return diasActuales; }
    public void setDiasActuales(Integer diasActuales) { this.diasActuales = diasActuales; }

    public Integer getDiasMaximo() { return diasMaximo; }
    public void setDiasMaximo(Integer diasMaximo) { this.diasMaximo = diasMaximo; }

    public LocalDate getUltimoEntrenamiento() { return ultimoEntrenamiento; }
    public void setUltimoEntrenamiento(LocalDate ultimoEntrenamiento) {
        this.ultimoEntrenamiento = ultimoEntrenamiento;
    }
}
