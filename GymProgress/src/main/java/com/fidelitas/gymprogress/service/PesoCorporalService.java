package com.fidelitas.gymprogress.service;

import com.fidelitas.gymprogress.domain.PesoCorporal;
import com.fidelitas.gymprogress.repository.PesoCorporalRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PesoCorporalService {

    private final PesoCorporalRepository pesoCorporalRepository;

    public PesoCorporalService(PesoCorporalRepository pesoCorporalRepository) {
        this.pesoCorporalRepository = pesoCorporalRepository;
    }

    //Guarda un registro de peso para el usuario
    public PesoCorporal guardar(Long usuarioId, Double valorPeso, String unidad) {
        PesoCorporal registro = new PesoCorporal();
        registro.setUsuarioId(usuarioId);
        registro.setFecha(LocalDate.now());

        if ("lb".equalsIgnoreCase(unidad)) {
            registro.setPesoLb(valorPeso);
        } else {
            registro.setPesoKg(valorPeso);
        }

        return pesoCorporalRepository.save(registro);
    }

    //Devuelve el historial de peso del usuario ordenado por fecha descendente.
    public List<PesoCorporal> historial(Long usuarioId) {
        return pesoCorporalRepository.findByUsuarioIdOrderByFechaDesc(usuarioId);
    }
}
