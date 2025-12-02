package ar.utn.da.dsi.frontend.controllers;

import ar.utn.da.dsi.frontend.client.dto.HechoDTO;
import ar.utn.da.dsi.frontend.client.dto.input.HechoInputDTO;
import ar.utn.da.dsi.frontend.services.hechos.HechoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/hechos")
public class HechoController {

  private final HechoService hechoService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  public HechoController(HechoService hechoService) {
    this.hechoService = hechoService;
  }

  @GetMapping("/nuevo")
  public String mostrarFormularioNuevoHecho(Model model) {
    if (!model.containsAttribute("hechoDTO")) {
      model.addAttribute("hechoDTO", new HechoInputDTO());
    }
    model.addAttribute("titulo", "Subir un nuevo Hecho");
    model.addAttribute("accion", "crear");
    try {
      model.addAttribute("categorias", hechoService.getAvailableCategories());
    } catch (Exception e) {
      model.addAttribute("categorias", List.of());
    }

    return "hecho-form";
  }

  @PostMapping("/crear")
  public String crearHecho(
      @ModelAttribute HechoInputDTO hechoDTO,
      @RequestParam(value = "archivo", required = false) MultipartFile archivo,
      @Nullable Authentication auth,
      RedirectAttributes redirectAttributes) {
    String userId = null;
    if (auth != null) {
      userId = auth.getName();
    }

    hechoDTO.setVisualizadorID(userId);

    try {
      hechoService.crear(hechoDTO, archivo);

      if (auth != null) {
        redirectAttributes.addFlashAttribute("success", "Sugerencia enviada y pendiente de revisión en Mi Panel.");
        return "redirect:/contributor";
      }
      redirectAttributes.addFlashAttribute("success", "Sugerencia recibida. Será revisada por un administrador.");
      return "redirect:/";

    } catch (RuntimeException e) {
      String errorMessage = getWebClientErrorMessage(e);

      redirectAttributes.addFlashAttribute("error", errorMessage);
      redirectAttributes.addFlashAttribute("hechoDTO", hechoDTO);
      return "redirect:/hechos/nuevo";
    }
  }

  @GetMapping("/{id}/editar")
  public String mostrarFormularioEditarHecho(@PathVariable("id") Long id, Model model) {

    HechoDTO hechoCheck = hechoService.buscarHechoEnDinamica(id);

    if (hechoCheck == null) {
      throw new RuntimeException("No se encontró el hecho en Dinámica");
    }

    // EXTRAER DATOS IMPORTANTES
    String estado = hechoCheck.getEstado();
    boolean tienePendiente = hechoCheck.isTieneEdicionPendiente();

    // VALIDACIÓN
    boolean puedeEditar = "ACEPTADO".equalsIgnoreCase(estado) && !tienePendiente;

    if (!puedeEditar) {
      model.addAttribute("error",
          "Este hecho no puede editarse porque no está aceptado o tiene una edición pendiente.");
      return "redirect:/hechos/" + id;
    }

    model.addAttribute("tieneEdicionPendiente", tienePendiente);

    if (!model.containsAttribute("hechoDTO")) {
      model.addAttribute("hechoDTO", hechoService.buscarHechoInputEnDinamica(id));
    }

    HechoInputDTO hechoDTO = (HechoInputDTO) model.getAttribute("hechoDTO");

    model.addAttribute("titulo", "Editar Hecho: " + hechoDTO.getTitulo());
    model.addAttribute("accion", "editar");

    try {
      model.addAttribute("categorias", hechoService.getAvailableCategories());
    } catch (Exception e) {
      model.addAttribute("categorias", List.of());
    }

    return "hecho-form";
  }



  @PostMapping("/{id}/editar")
  public String editarHecho(
      @PathVariable("id") Long id,
      @ModelAttribute HechoInputDTO hechoDTO,
      @RequestParam(value = "archivo", required = false) MultipartFile archivo,
      Authentication auth,
      RedirectAttributes redirectAttributes) {

    String userId = auth.getName();
    hechoDTO.setVisualizadorID(userId);

    try {
      hechoService.actualizar(id, hechoDTO, archivo);
      redirectAttributes.addFlashAttribute("success", "Edición propuesta enviada y pendiente de revisión.");
      return "redirect:/contributor";
    } catch (RuntimeException e) {
      String errorMessage = getWebClientErrorMessage(e);

      redirectAttributes.addFlashAttribute("error", errorMessage);
      redirectAttributes.addFlashAttribute("hechoDTO", hechoDTO);
      return "redirect:/hechos/" + id + "/editar";
    }
  }

  /**
   * Endpoint usado por AdminController para resolver solicitudes de Nuevo Hecho.
   * (Maneja ACEPTADO, RECHAZADO, ACEPTADO_CON_SUGERENCIAS).
   */
  @PostMapping("/hechos/{id}/resolver")
  public String resolverHecho(@PathVariable("id") Long id,
                              @RequestParam("estado") String estado,
                              @RequestParam(value = "detalle", required = false) String detalle,
                              RedirectAttributes redirectAttributes) {
    try {
      if ("ACEPTADO".equals(estado)) {
        hechoService.aprobar(id);
        redirectAttributes.addFlashAttribute("success", "Hecho aprobado y visible.");

      } else if ("RECHAZADO".equals(estado)) {
        if (detalle != null && !detalle.isBlank()) {
          hechoService.rechazarConMotivo(id, detalle);
        } else {
          hechoService.rechazar(id);
        }
        redirectAttributes.addFlashAttribute("success", "Hecho rechazado.");

      } else if ("ACEPTADO_CON_SUGERENCIAS".equals(estado)) {
        if (detalle != null && !detalle.isBlank()) {
          hechoService.aprobarConSugerencias(id, detalle);
        } else {
          hechoService.aprobar(id);
        }
        redirectAttributes.addFlashAttribute("success", "Hecho aprobado con sugerencias.");
      }

      return "redirect:/admin?tab=requests";
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al resolver el hecho: " + e.getMessage());
      return "redirect:/admin?tab=requests";
    }
  }

  /**
   * Método de utilidad para extraer el mensaje de error del WebClient.
   */
  private String getWebClientErrorMessage(RuntimeException e) {
    Throwable cause = e.getCause();
    if (cause instanceof WebClientResponseException) {
      WebClientResponseException webClientEx = (WebClientResponseException) cause;
      String errorBody = webClientEx.getResponseBodyAsString();
      try {
        // Intentamos leer el mensaje de error del JSON del backend (asumiendo ApiResponse/GlobalExceptionHandler)
        return objectMapper.readTree(errorBody).path("message").asText(webClientEx.getStatusText());
      } catch (Exception jsonEx) {
        return "Error de comunicación: " + webClientEx.getStatusText() + " (" + webClientEx.getStatusCode().value() + "). Verificá los logs del backend.";
      }
    }
    return e.getMessage();
  }
}