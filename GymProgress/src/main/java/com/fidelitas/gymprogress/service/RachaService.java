package com.fidelitas.gymprogress.service;

import com.fidelitas.gymprogress.domain.Racha;
import com.fidelitas.gymprogress.repository.RachaRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sistema de rachas de días consecutivos entrenando.
 */
@Service
public class RachaService {

    private final RachaRepository rachaRepository;

    public RachaService(RachaRepository rachaRepository) {
        this.rachaRepository = rachaRepository;
    }

    /**
     * Registra que el usuario entrenó hoy.
     * - Si ya entrenó hoy: no hace nada.
     * - Si entrenó ayer: incrementa la racha.
     * - Si pasó más de un día: reinicia la racha a 1.
     */
    @Transactional
    public Racha registrarEntrenamiento(Long usuarioId) {
        Racha racha = rachaRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    Racha nueva = new Racha();
                    nueva.setUsuarioId(usuarioId);
                    return nueva;
                });

        LocalDate hoy = LocalDate.now();
        LocalDate ultimo = racha.getUltimoEntrenamiento();

        if (hoy.equals(ultimo)) {
            // Ya se contó hoy
            return racha;
        }

        if (ultimo != null && ultimo.plusDays(1).equals(hoy)) {
            // Día consecutivo
            racha.setDiasActuales(racha.getDiasActuales() + 1);
        } else {
            // Racha rota o primera vez
            racha.setDiasActuales(1);
        }

        if (racha.getDiasActuales() > racha.getDiasMaximo()) {
            racha.setDiasMaximo(racha.getDiasActuales());
        }

        racha.setUltimoEntrenamiento(hoy);
        return rachaRepository.save(racha);
    }

    //Devuelve la racha actual del usuario (0 si no existe).
    @Transactional(readOnly = true)
    public Racha obtener(Long usuarioId) {
        return rachaRepository.findByUsuarioId(usuarioId).orElseGet(() -> {
            Racha vacia = new Racha();
            vacia.setUsuarioId(usuarioId);
            return vacia;
        });
    }
}
