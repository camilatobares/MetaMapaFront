package ar.utn.da.dsi.frontend.controllers;

import ar.utn.da.dsi.frontend.client.dto.input.HechoInputDTO;
import ar.utn.da.dsi.frontend.client.dto.input.SolicitudEliminacionInputDTO;
import ar.utn.da.dsi.frontend.exceptions.ValidationException;
import ar.utn.da.dsi.frontend.services.hechos.HechoService;
import ar.utn.da.dsi.frontend.services.solicitudes.SolicitudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/solicitudes")
public class SolicitudController {

  private final SolicitudService solicitudService;
  private final HechoService hechoService;

  @Autowired
  public SolicitudController(SolicitudService solicitudService, HechoService hechoService) {
    this.solicitudService = solicitudService;
    this.hechoService = hechoService;
  }

  @GetMapping("/nueva")
  public String mostrarFormularioNuevaSolicitud(@RequestParam("hechoId") Long hechoId, Model model) {
    HechoInputDTO hechoDTO = hechoService.getHechoAgregadorDTOporId(hechoId);

    SolicitudEliminacionInputDTO solicitudDTO = new SolicitudEliminacionInputDTO();

    model.addAttribute("hecho", hechoDTO);
    model.addAttribute("solicitudDTO", solicitudDTO);
    model.addAttribute("titulo", "Solicitar Eliminación de Hecho");

    return "solicitud-form";
  }

  @PostMapping("/crear")
  public String crearSolicitud(
      @ModelAttribute SolicitudEliminacionInputDTO solicitudDTO,
      @Nullable Authentication auth,
      RedirectAttributes redirectAttributes) { // <--- 1. Agregar este parámetro

    try {
      solicitudService.crear(solicitudDTO);

      // 2. Usar addFlashAttribute (Esto viaja oculto y seguro hasta la siguiente vista)
      redirectAttributes.addFlashAttribute("success", "Solicitud de eliminación creada correctamente. Un administrador la revisará.");

    } catch (ValidationException e) {
      // Extraemos el primer mensaje de error detallado del mapa de ValidationException
      String errorMessage = "Error de validación: " + e.getFieldErrors().values().iterator().next();

      redirectAttributes.addFlashAttribute("error", errorMessage);
      // Mantenemos los datos introducidos para el redirect
      redirectAttributes.addFlashAttribute("solicitudDTO", solicitudDTO);
      // Redirigimos al formulario original (necesitamos el hechoId, que está en solicitudDTO.getId())
      return "redirect:/solicitudes/nueva?hechoId=" + solicitudDTO.getId();

    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Hubo un problema al crear la solicitud: " + e.getMessage());
    }

    return "redirect:/"; // 3. Redirigir a la home LIMPIO (sin ?success=...)
  }
}