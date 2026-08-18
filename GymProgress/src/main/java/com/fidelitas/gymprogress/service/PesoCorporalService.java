package com.fidelitas.gymprogress.service;

import com.fidelitas.gymprogress.domain.PesoCorporal;
import com.fidelitas.gymprogress.repository.PesoCorporalRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PesoCorporalService {

    private final PesoCorporalRepository pesoCorporalRepository;

    public PesoCorporalService(PesoCorporalRepository pesoCorporalRepository) {
        this.pesoCorporalRepository = pesoCorporalRepository;
    }

    /**
     * Guarda el registro de peso del día para el usuario. El registro
     * periódico es diario: si ya existía uno para hoy, se actualiza
     * en vez de duplicarlo.
     */
    public PesoCorporal guardar(Long usuarioId, Double valorPeso, String unidad) {
        LocalDate hoy = LocalDate.now();
        PesoCorporal registro = pesoCorporalRepository
                .findByUsuarioIdAndFecha(usuarioId, hoy)
                .orElseGet(PesoCorporal::new);

        registro.setUsuarioId(usuarioId);
        registro.setFecha(hoy);

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

    //Elimina un registro de peso, siempre que pertenezca al usuario.
    public void eliminar(Long usuarioId, Long id) {
        pesoCorporalRepository.findByIdAndUsuarioId(id, usuarioId)
                .ifPresent(pesoCorporalRepository::delete);
    }

    //Días transcurridos desde el último registro, o null si nunca ha registrado.
    public Long diasDesdeUltimoRegistro(Long usuarioId) {
        List<PesoCorporal> historial = historial(usuarioId);
        if (historial.isEmpty()) {
            return null;
        }
        return ChronoUnit.DAYS.between(historial.get(0).getFecha(), LocalDate.now());
    }
}
