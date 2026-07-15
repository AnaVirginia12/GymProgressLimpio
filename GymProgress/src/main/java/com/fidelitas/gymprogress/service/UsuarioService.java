package com.fidelitas.gymprogress.service;

import com.fidelitas.gymprogress.domain.Usuario;
import com.fidelitas.gymprogress.repository.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // HU1 — Autentica al usuario con correo y contraseña.
    
    public Optional<Usuario> iniciarSesion(String correo, String password) {
        return usuarioRepository.findByCorreo(correo)
                .filter(u -> u.getPassword().equals(password))
                .map(u -> {
                    u.setTokenSesion(UUID.randomUUID().toString().replace("-", ""));
                    return usuarioRepository.save(u);
                });
    }

    // HU4 — Registra un usuario nuevo con los datos de la encuesta de onboarding.
     
    public Usuario registrar(Usuario usuario) {
        if (usuarioRepository.findByCorreo(usuario.getCorreo()).isPresent()) {
            throw new IllegalArgumentException("El correo ya está registrado.");
        }
        return usuarioRepository.save(usuario);
    }

    // HU5 — Cierra la sesión del usuario limpiando su token.
     
    public void cerrarSesion(Long usuarioId) {
        usuarioRepository.findById(usuarioId).ifPresent(u -> {
            u.setTokenSesion(null);
            usuarioRepository.save(u);
        });
    }

    // HU5 — Actualiza los datos editables del perfil (nombre, foto, objetivo,días, minutos, equipamiento, lesiones, unidadPeso).
     
    public Usuario actualizarPerfil(Long usuarioId, Usuario datosNuevos) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (datosNuevos.getNombre() != null) {
            u.setNombre(datosNuevos.getNombre());
        }
        if (datosNuevos.getFotoUrl() != null) {
            u.setFotoUrl(datosNuevos.getFotoUrl());
        }
        if (datosNuevos.getObjetivo() != null) {
            u.setObjetivo(datosNuevos.getObjetivo());
        }
        if (datosNuevos.getNivel() != null) {
            u.setNivel(datosNuevos.getNivel());
        }
        if (datosNuevos.getDiasSemana() != null) {
            u.setDiasSemana(datosNuevos.getDiasSemana());
        }
        if (datosNuevos.getMinutosSesion() != null) {
            u.setMinutosSesion(datosNuevos.getMinutosSesion());
        }
        if (datosNuevos.getEquipamiento() != null) {
            u.setEquipamiento(datosNuevos.getEquipamiento());
        }
        if (datosNuevos.getLesiones() != null) {
            u.setLesiones(datosNuevos.getLesiones());
        }
        if (datosNuevos.getUnidadPeso() != null) {
            u.setUnidadPeso(datosNuevos.getUnidadPeso());
        }

        return usuarioRepository.save(u);
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }
}
