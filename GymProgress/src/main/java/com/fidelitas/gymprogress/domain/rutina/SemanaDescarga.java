package com.fidelitas.gymprogress.domain.rutina;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *  Semana de descarga (deload).
 *
 * Cuando el análisis de las últimas 4 semanas detecta señales de
 * sobreentrenamiento se crea una sugerencia en estado SUGERIDA. El usuario
 * decide: aceptarla (la rutina baja volumen e intensidad durante 7 días),
 * posponerla una semana, o ignorarla.
 */
@Entity
@Table(name = "semana_descarga")
public class SemanaDescarga {

    public static final String SUGERIDA = "SUGERIDA";
    public static final String ACEPTADA = "ACEPTADA";
    public static final String POSPUESTA = "POSPUESTA";
    public static final String IGNORADA = "IGNORADA";

    //Durante el deload la rutina se hace al 60% del volumen habitual
    public static final double FACTOR_VOLUMEN = 0.6;

    //Y con un 10% menos de carga
    public static final double FACTOR_INTENSIDAD = 0.9;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 20)
    private String estado = SUGERIDA;

    /** Por qué se sugirió, en texto para mostrárselo al usuario. */
    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(name = "detectada_en", nullable = false)
    private LocalDateTime detectadaEn = LocalDateTime.now();

    // Solo se llenan cuando el usuario acepta
    @Column(name = "inicio")
    private LocalDate inicio;

    @Column(name = "fin")
    private LocalDate fin;

    //Si la pospone, no se vuelve a sugerir hasta esta fecha
    @Column(name = "reintentar_desde")
    private LocalDate reintentarDesde;

    public SemanaDescarga() {
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getDetectadaEn() {
        return detectadaEn;
    }

    public void setDetectadaEn(LocalDateTime detectadaEn) {
        this.detectadaEn = detectadaEn;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public void setInicio(LocalDate inicio) {
        this.inicio = inicio;
    }

    public LocalDate getFin() {
        return fin;
    }

    public void setFin(LocalDate fin) {
        this.fin = fin;
    }

    public LocalDate getReintentarDesde() {
        return reintentarDesde;
    }

    public void setReintentarDesde(LocalDate reintentarDesde) {
        this.reintentarDesde = reintentarDesde;
    }

    // Está aceptada y hoy cae dentro de la semana
    @Transient
    public boolean estaActiva() {
        if (!ACEPTADA.equals(estado) || inicio == null || fin == null) {
            return false;
        }
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(inicio) && !hoy.isAfter(fin);
    }

    @Transient
    public boolean estaPendiente() {
        return SUGERIDA.equals(estado);
    }

    //Días que quedan de descarga, contando hoy
    @Transient
    public long getDiasRestantes() {
        if (!estaActiva()) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), fin) + 1;
    }
}
