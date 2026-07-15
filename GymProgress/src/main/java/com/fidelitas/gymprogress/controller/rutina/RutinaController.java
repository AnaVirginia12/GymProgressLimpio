package com.fidelitas.gymprogress.controller.rutina;

import com.fidelitas.gymprogress.service.rutina.RutinaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rutinas")
public class RutinaController {

    private final RutinaService rutinaService;

    public RutinaController(RutinaService rutinaService) {
        this.rutinaService = rutinaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("rutinas", rutinaService.listarRutinas());
        return "rutina/index";
    }

    @GetMapping("/programas")
    public String programas() {
        return "rutina/programas";
    }

    @PostMapping("/programas/seleccionar")
    public String seleccionarPrograma(
            @RequestParam String programa,
            RedirectAttributes redirectAttributes
    ) {
        rutinaService.seleccionarPrograma(programa);

        redirectAttributes.addFlashAttribute(
                "mensaje",
                "Programa seleccionado correctamente."
        );

        return "redirect:/rutinas";
    }

   @GetMapping("/hoy")
public String rutinaHoy(Model model) {

    var rutina = rutinaService.obtenerRutinaActiva();

    model.addAttribute("rutina", rutina);

    model.addAttribute(
            "duracionEstimada",
            rutinaService.calcularDuracionEstimadaMinutos(rutina)
    );

    return "rutina/hoy";
}
}