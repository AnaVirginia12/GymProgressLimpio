package com.fidelitas.gymprogress.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Sistema de rachas de días consecutivos entrenando.
 * Se mantiene un único registro por usuario con el contador actual
 * y la racha máxima histórica.
 */
@Entity //convierte esto de una clase de java común en una tabla
@Table(name = "racha") //fija el nombre exacto de la tabla
public class Racha {

    @Id //marca la llave primaria. toda @Entity necesita una
    @GeneratedValue(strategy = GenerationType.IDENTITY) //IDENTITY significa "el número lo pone mysql, no java", cuando un usuario nuevo se hace, se manda en null el id, mysql le asigna el siguiente número libre y lo devuelve
    private Long id; //Long de objeto

    @Column(name = "usuario_id", nullable = false, unique = true) //unique indica que no puede haber dos filas con el mismo 'usuario_id' o sea cada usuario tiene exactamente 1 racha
    private Long usuarioId;

    //Días consecutivos actuales entrenando
    @Column(name = "dias_actuales", nullable = false)
    private Integer diasActuales = 0;
    // la racha viva, si falta un día se reinicia a 1

    //Máxima racha histórica alcanzada
    @Column(name = "dias_maximo", nullable = false)
    private Integer diasMaximo = 0;
    //récord histórico, nunca baja el número, siempre sube

    //Última fecha en que se registró un entrenamiento
    @Column(name = "ultimo_entrenamiento")
    private LocalDate ultimoEntrenamiento;

    //constructor vacío
    public Racha() {
    }

    //setters n getters
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
