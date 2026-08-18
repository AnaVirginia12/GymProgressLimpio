package com.fidelitas.gymprogress.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Representa a un usuario de la aplicación: sus datos de acceso
 * (correo, contraseña) y su perfil de entrenamiento (objetivo,
 * nivel, días disponibles, etc.).
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    private String correo;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String nombre;

    // URL o path relativo de la foto de perfil 
    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    //Objetivo principal: Fuerza, Hipertrofia, Resistencia, etc. 
    @Column(length = 80)
    private String objetivo;

    // Nivel: Principiante, Intermedio, Avanzado 
    @Column(length = 50)
    private String nivel;

    //Días disponibles por semana (1-7) 
    @Column(name = "dias_semana")
    private Integer diasSemana;

    //Minutos disponibles por sesión
    @Column(name = "minutos_sesion")
    private Integer minutosSesion;

    //Equipamiento disponible: Gimnasio, Casa, Al aire libre 
    @Column(length = 100)
    private String equipamiento;

    //Lesiones o limitaciones físicas reportadas 
    @Column(length = 500)
    private String lesiones;

    //Preferencia de unidad: kg o lb
    @Column(name = "unidad_peso", length = 2, nullable = false)
    private String unidadPeso = "kg";

    //Preferencia de apariencia: dark o light (valores de data-bs-theme)
    @Column(name = "tema", length = 10, nullable = false)
    private String tema = "dark";

    //Si el descanso entre series inicia solo al guardar una serie, o el usuario lo inicia manualmente
    @Column(name = "descanso_automatico", nullable = false)
    private Boolean descansoAutomatico = true;

    //Duración por defecto (segundos) del temporizador de descanso
    @Column(name = "descanso_por_defecto_seg", nullable = false)
    private Integer descansoPorDefectoSeg = 90;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    //Token de sesión para sincronización entre dispositivos 
    @Column(name = "token_sesion", length = 64, unique = true)
    private String tokenSesion;

    public Usuario() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public Integer getDiasSemana() { return diasSemana; }
    public void setDiasSemana(Integer diasSemana) { this.diasSemana = diasSemana; }

    public Integer getMinutosSesion() { return minutosSesion; }
    public void setMinutosSesion(Integer minutosSesion) { this.minutosSesion = minutosSesion; }

    public String getEquipamiento() { return equipamiento; }
    public void setEquipamiento(String equipamiento) { this.equipamiento = equipamiento; }

    public String getLesiones() { return lesiones; }
    public void setLesiones(String lesiones) { this.lesiones = lesiones; }

    public String getUnidadPeso() { return unidadPeso; }
    public void setUnidadPeso(String unidadPeso) { this.unidadPeso = unidadPeso; }

    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }

    public Boolean getDescansoAutomatico() { return descansoAutomatico; }
    public void setDescansoAutomatico(Boolean descansoAutomatico) { this.descansoAutomatico = descansoAutomatico; }

    public Integer getDescansoPorDefectoSeg() { return descansoPorDefectoSeg; }
    public void setDescansoPorDefectoSeg(Integer descansoPorDefectoSeg) { this.descansoPorDefectoSeg = descansoPorDefectoSeg; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public String getTokenSesion() { return tokenSesion; }
    public void setTokenSesion(String tokenSesion) { this.tokenSesion = tokenSesion; }
}
