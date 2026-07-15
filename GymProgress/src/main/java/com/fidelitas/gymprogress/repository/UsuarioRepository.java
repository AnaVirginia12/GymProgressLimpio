package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByTokenSesion(String tokenSesion);
}
