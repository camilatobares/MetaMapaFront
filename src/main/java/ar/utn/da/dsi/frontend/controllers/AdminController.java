package ar.utn.da.dsi.frontend.controllers;

import ar.utn.da.dsi.frontend.client.dto.HechoDTO;
import ar.utn.da.dsi.frontend.client.dto.input.ColeccionInputDTO;
import ar.utn.da.dsi.frontend.client.dto.input.HechoInputDTO;
import ar.utn.da.dsi.frontend.client.dto.output.*;
import ar.utn.da.dsi.frontend.services.colecciones.ColeccionService;
import ar.utn.da.dsi.frontend.services.estadisticas.EstadisticasService;
import ar.utn.da.dsi.frontend.services.hechos.HechoService;
import ar.utn.da.dsi.frontend.services.solicitudes.SolicitudService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ar.utn.da.dsi.frontend.exceptions.ValidationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

  private final ColeccionService coleccionService;
  private final SolicitudService solicitudService;
  private final HechoService hechoService;
  private final EstadisticasService estadisticasService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public AdminController(ColeccionService coleccionService, SolicitudService solicitudService, HechoService hechoService, EstadisticasService estadisticasService) {
    this.coleccionService = coleccionService;
    this.solicitudService = solicitudService;
    this.hechoService = hechoService;
    this.estadisticasService = estadisticasService;
  }

  @GetMapping
  public String mostrarPanelAdmin(Model model, Authentication authentication) {
    // 1. Colecciones
    try {
      model.addAttribute("colecciones", coleccionService.obtenerTodas());
    } catch (Exception e) {
      model.addAttribute("colecciones", List.of());
    }

    // 2. UNIFICAR SOLICITUDES (USANDO HISTORIAL COMPLETO)
    List<SolicitudUnificadaDTO> listaUnificada = new ArrayList<>();
    String adminId = (authentication != null) ? authentication.getName() : null;

    if (adminId != null) {
      // A) TODOS los Hechos (Nuevos, Aprobados, Rechazados)
      try {
        // CAMBIO CLAVE: Usamos buscarHistorialHechos() en lugar de buscarHechosPendientes()
        List<HechoDTO> todosHechos = hechoService.buscarHistorialHechos();
        if (todosHechos != null) {
          for (HechoDTO h : todosHechos) {
            if (h.getId() != null) {
              // El estado ya viene en el DTO (PENDIENTE, ACEPTADO, RECHAZADO)
              listaUnificada.add(new SolicitudUnificadaDTO(h.getId(), h.getTitulo(), "Nuevo Hecho", h.getEstado()));
            }
          }
        }
      } catch (Exception e) {
        System.out.println("Error historial hechos: " + e.getMessage());
      }

      // B) Solicitudes de Baja (Ya trae todas)
      try {
        List<SolicitudEliminacionOutputDTO> bajas = solicitudService.obtenerTodasParaAdmin();
        if (bajas != null) {
          for (SolicitudEliminacionOutputDTO b : bajas) {
            if (b.nroDeSolicitud() != null) {
              listaUnificada.add(new SolicitudUnificadaDTO(b.nroDeSolicitud(), b.nombreHecho(), "Eliminación", b.estado()));
            }
          }
        }
      } catch (Exception e) {
        System.out.println("Error historial bajas: " + e.getMessage());
      }

      // C) TODAS las Ediciones
      try {
        // CAMBIO CLAVE: Usamos buscarHistorialEdiciones() en lugar de buscarEdicionesPendientes()
        List<EdicionOutputDTO> ediciones = hechoService.buscarHistorialEdiciones();
        if (ediciones != null) {
          for (EdicionOutputDTO e : ediciones) {
            if (e.getId() != null) {
              String titulo = (e.getTituloPropuesto() != null) ? e.getTituloPropuesto() : "Edición Hecho ID " + e.getIdHechoOriginal();
              listaUnificada.add(new SolicitudUnificadaDTO(e.getId(), titulo, "Edición", e.getEstado()));
            }
          }
        }
      } catch (Exception e) {
        System.out.println("Error historial ediciones: " + e.getMessage());
      }
    }

    model.addAttribute("solicitudes", listaUnificada);

    // Datos auxiliares
    try {
      model.addAttribute("consensusLabels", hechoService.getConsensusLabels());
      model.addAttribute("availableSources", hechoService.getAvailableSources());
    } catch (Exception e) {
      model.addAttribute("consensusLabels", null);
      model.addAttribute("availableSources", null);
    }

    model.addAttribute("titulo", "Panel de Administración");
    return "admin";
  }

  // --- ENDPOINT MODAL (UNIFICADO) ---
  @GetMapping("/api/solicitudes/{id}/detalle")
  @ResponseBody
  public ResponseEntity<?> getDetalleUnificado(@PathVariable Long id, @RequestParam String tipo) {
    try {
      if ("Nuevo Hecho".equals(tipo)) {
        HechoDTO hecho = hechoService.buscarHechoEnDinamica(id);

        if (hecho == null) {
          return ResponseEntity.status(404).body(Map.of("error", "Hecho no encontrado"));
        }
        return ResponseEntity.ok(hecho);
      }
      else if ("Eliminación".equals(tipo)) {
        return ResponseEntity.ok(solicitudService.obtenerPorId(id.intValue(), "admin"));
      }
      else if ("Edición".equals(tipo)) {
        EdicionOutputDTO edicion = hechoService.buscarEdicionPorId(id);
        if (edicion == null) return ResponseEntity.notFound().build();

        HechoInputDTO original = new HechoInputDTO();
        try {
          if (edicion.getIdHechoOriginal() != null) {
            original = hechoService.buscarHechoInputEnDinamica(edicion.getIdHechoOriginal());
          }
        } catch (Exception e) {
          original.setTitulo("No disponible");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("propuesta", edicion);
        response.put("original", original);
        return ResponseEntity.ok(response);
      }
      return ResponseEntity.badRequest().body(Map.of("error", "Tipo desconocido"));
    } catch (Exception e) {
      return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
  }


  @PostMapping("/hechos/{id}/resolver")
  public String resolverHecho(
      @PathVariable("id") Long id,
      @RequestParam("estado") String estado,
      @RequestParam(value = "sugerencia", required = false) String sugerencia,
      RedirectAttributes redirectAttributes) {

    try {
      if ("ACEPTADO".equals(estado)) {
        hechoService.aprobar(id);
        redirectAttributes.addFlashAttribute("success", "Hecho aprobado y publicado.");

      } else if ("RECHAZADO".equals(estado)) {
        hechoService.rechazar(id);
        redirectAttributes.addFlashAttribute("success", "Hecho rechazado.");

      } else if ("ACEPTADO_CON_SUGERENCIAS".equals(estado)) {
        if (sugerencia == null) sugerencia = "";
        hechoService.aprobarConSugerencias(id, sugerencia);
        redirectAttributes.addFlashAttribute("success", "Hecho aprobado con sugerencias.");

      } else {
        redirectAttributes.addFlashAttribute("error", "Estado desconocido: " + estado);
      }

    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al resolver: " + e.getMessage());
    }

    return "redirect:/admin?tab=requests";
  }

  // --- ACTIONS Y COLECCIONES (MANTENIDOS) ---
  @PostMapping("/solicitudes/{id}/aprobar")
  public String aprobarSolicitud(@PathVariable("id") Integer id, Authentication auth, RedirectAttributes redirectAttributes) {
    solicitudService.aceptar(id, auth.getName());
    redirectAttributes.addFlashAttribute("success", "Solicitud de eliminación aprobada.");
    return "redirect:/admin?tab=requests";
  }
  @PostMapping("/solicitudes/{id}/rechazar")
  public String rechazarSolicitud(@PathVariable("id") Integer id, Authentication auth, RedirectAttributes redirectAttributes) {
    solicitudService.rechazar(id, auth.getName());
    redirectAttributes.addFlashAttribute("success", "Solicitud de eliminación rechazada.");
    return "redirect:/admin?tab=requests";
  }
  @PostMapping("/hechos/{id}/aprobar")
  public String aprobarHecho(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
    hechoService.aprobar(id);
    redirectAttributes.addFlashAttribute("success", "Hecho aprobado y publicado.");
    return "redirect:/admin?tab=requests";
  }
  @PostMapping("/hechos/{id}/rechazar")
  public String rechazarHecho(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
    hechoService.rechazar(id);
    redirectAttributes.addFlashAttribute("success", "Hecho rechazado.");
    return "redirect:/admin?tab=requests";
  }
  @PostMapping("/ediciones/{id}/aceptar")
  public String aceptarEdicion(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
    hechoService.aceptarEdicion(id);
    redirectAttributes.addFlashAttribute("success", "Edición aceptada y publicada.");
    return "redirect:/admin?tab=requests";
  }
  @PostMapping("/ediciones/{id}/rechazar")
  public String rechazarEdicion(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
    hechoService.rechazarEdicion(id);
    redirectAttributes.addFlashAttribute("success", "Edición rechazada.");
    return "redirect:/admin?tab=requests";
  }

  @GetMapping("/colecciones/nueva")
  public String mostrarFormularioNuevaColeccion(Model model) {
    model.addAttribute("coleccionDTO", new ColeccionInputDTO());
    model.addAttribute("titulo", "Crear Nueva Colección");
    model.addAttribute("accion", "crear");

    model.addAttribute("consensusLabels", hechoService.getConsensusLabels()); // Ya lo tenías
    model.addAttribute("fuentesDisponibles", List.of("DINAMICA", "ESTATICA"));
    model.addAttribute("criteriosDisponibles", List.of("TITULO", "CATEGORIA", "FECHA", "UBICACION"));

    model.addAttribute("listaCategorias", hechoService.getAvailableCategories());
    model.addAttribute("listaProvincias", List.of("Buenos Aires", "CABA", "Catamarca", "Chaco", "Chubut", "Córdoba", "Corrientes", "Entre Ríos", "Formosa", "Jujuy", "La Pampa", "La Rioja", "Mendoza", "Misiones", "Neuquén", "Río Negro", "Salta", "San Juan", "San Luis", "Santa Cruz", "Santa Fe", "Santiago del Estero", "Tierra del Fuego", "Tucumán"));

    return "admin-coleccion-form";
  }

  @PostMapping("/colecciones/crear")
  public String crearColeccion(@ModelAttribute ColeccionInputDTO dto, HttpSession session, RedirectAttributes redirectAttributes) {
    dto.setVisualizadorID(getUserIdFromSession(session));
    coleccionService.crear(dto);
    redirectAttributes.addFlashAttribute("success", "Colección creada exitosamente.");
    return "redirect:/admin?tab=collections";
  }

  @GetMapping("/colecciones/{id}/editar")
  public String mostrarFormularioEditarColeccion(@PathVariable("id") String id, Model model) {
    // 1. Obtener la colección existente desde el Agregador
    ColeccionOutputDTO dtoOutput = coleccionService.obtenerPorId(id);

    // 2. Instanciar el DTO de entrada para el formulario
    ColeccionInputDTO dtoInput = new ColeccionInputDTO();

    // 3. Mapear datos básicos (USANDO GETTERS)
    dtoInput.setTitulo(dtoOutput.getTitulo());
    dtoInput.setDescripcion(dtoOutput.getDescripcion());
    dtoInput.setHandleID(dtoOutput.getHandleID());
    dtoInput.setAlgoritmoConsenso(dtoOutput.getAlgoritmoConsenso());

    // 4. Mapear Fuentes (Lista directa)
    if (dtoOutput.getFuentes() != null) {
      dtoInput.setFuentes(new ArrayList<>(dtoOutput.getFuentes()));
    } else {
      dtoInput.setFuentes(new ArrayList<>());
    }

    // 5. Mapear Criterios (Parseo de "FiltroPorX: Valor" a listas separadas)
    List<String> listaNombres = new ArrayList<>();
    List<String> listaValores = new ArrayList<>();

    if (dtoOutput.getCriterios() != null) {
      for (String criterioStr : dtoOutput.getCriterios()) {
        // El backend suele devolver: "FiltroPorTitulo: Incendio"
        String[] partes = criterioStr.split(": ", 2);
        if (partes.length == 2) {
          String nombreClase = partes[0]; // Ej: FiltroPorTitulo
          String valor = partes[1];       // Ej: Incendio

          // Limpiamos el nombre para que coincida con el Enum del front (TITULO, CATEGORIA...)
          String tipo = nombreClase.replace("FiltroPor", "").toUpperCase();
          // Ajuste por si acaso "Ubicacion" viene con mayúscula/minúscula distinta
          if (tipo.equals("UBICACION")) tipo = "UBICACION";

          listaNombres.add(tipo);
          listaValores.add(valor);
        }
      }
    }
    dtoInput.setCriteriosPertenenciaNombres(listaNombres);
    dtoInput.setCriteriosPertenenciaValores(listaValores);

    // 6. Cargar datos al modelo
    model.addAttribute("coleccionDTO", dtoInput);
    model.addAttribute("titulo", "Editar Colección: " + dtoOutput.getTitulo());
    model.addAttribute("accion", "editar");

    // 7. Cargar listas para los selectores (Fuentes y Criterios disponibles)
    try {
      model.addAttribute("consensusLabels", hechoService.getConsensusLabels());
    } catch (Exception e) {
      model.addAttribute("consensusLabels", Map.of());
    }
    model.addAttribute("fuentesDisponibles", List.of("DINAMICA", "ESTATICA"));
    model.addAttribute("criteriosDisponibles", List.of("TITULO", "CATEGORIA", "FECHA", "UBICACION"));

    model.addAttribute("listaCategorias", hechoService.getAvailableCategories());
    model.addAttribute("listaProvincias", List.of("Buenos Aires", "CABA", "Catamarca", "Chaco", "Chubut", "Córdoba", "Corrientes", "Entre Ríos", "Formosa", "Jujuy", "La Pampa", "La Rioja", "Mendoza", "Misiones", "Neuquén", "Río Negro", "Salta", "San Juan", "San Luis", "Santa Cruz", "Santa Fe", "Santiago del Estero", "Tierra del Fuego", "Tucumán"));

    return "admin-coleccion-form";
  }

  @PostMapping("/colecciones/{id}/editar")
  public String guardarColeccionCompleta(@PathVariable("id") String id,
                                         @ModelAttribute ColeccionInputDTO dto,
                                         Authentication auth,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
    try {
      String userId = getUserIdFromSession(session);
      if (userId == null) {
        throw new RuntimeException("No se pudo obtener el ID del usuario de la sesión.");
      }

      dto.setVisualizadorID(userId);

      coleccionService.actualizar(id, dto);

      redirectAttributes.addFlashAttribute("success", "Colección " + dto.getTitulo() + " actualizada correctamente.");
      return "redirect:/admin?tab=collections";
    } catch (ValidationException e) {
      redirectAttributes.addFlashAttribute("error", "Error de validación: " + e.getFieldErrors().values().iterator().next());
      redirectAttributes.addFlashAttribute("coleccionDTO", dto);
      return "redirect:/admin/colecciones/" + id + "/editar";
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error al actualizar la colección: " + e.getMessage());
      redirectAttributes.addFlashAttribute("coleccionDTO", dto);
      return "redirect:/admin/colecciones/" + id + "/editar";
    }
  }

  @PostMapping("/colecciones/{id}/eliminar")
  public String eliminarColeccion(@PathVariable("id") String id,
                                  Authentication auth,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {

    // 2. OBTENER EL ID NUMÉRICO REAL (Ej: "1")
    String userId = getUserIdFromSession(session);

    if (userId == null) {
      redirectAttributes.addFlashAttribute("error", "No se pudo identificar al usuario. Inicie sesión nuevamente.");
      return "redirect:/login";
    }

    // 3. PASAR EL ID NUMÉRICO AL SERVICIO (Antes pasabas auth.getName() que es el email)
    coleccionService.eliminar(id, userId);

    redirectAttributes.addFlashAttribute("success", "Colección eliminada correctamente.");
    return "redirect:/admin?tab=collections";
  }


  @PostMapping("/hechos/importar-csv")
  public String importarCsv(@RequestParam("csvFile") MultipartFile file, RedirectAttributes attr) {
    try {
      hechoService.importarCsv(file);
      attr.addFlashAttribute("success", "CSV subido con éxito al sistema.");
    } catch (RuntimeException e) {
      String errorMessage = getWebClientErrorMessage(e);
      attr.addFlashAttribute("error", "Error al importar: " + errorMessage);
    }
    return "redirect:/admin";
  }

  private String getWebClientErrorMessage(RuntimeException e) {
    Throwable cause = e.getCause();
    if (cause instanceof WebClientResponseException) {
      WebClientResponseException webClientEx = (WebClientResponseException) cause;
      String errorBody = webClientEx.getResponseBodyAsString();
      try {
        return objectMapper.readTree(errorBody).path("message").asText(webClientEx.getStatusText());
      } catch (Exception jsonEx) {
        return "Error de comunicación: " + webClientEx.getStatusText() + " (" + webClientEx.getStatusCode().value() + "). Verificá los logs del backend.";
      }
    }
    return e.getMessage();
  }

  // MÉTODO AUXILIAR PARA NO REPETIR CÓDIGO
  private String getUserIdFromSession(HttpSession session) {
    try {
      String userJson = (String) session.getAttribute("userJson");
      if (userJson != null) {
        return String.valueOf(objectMapper.readTree(userJson).get("id").asLong());
      }
    } catch (Exception e) {
      System.err.println("Error obteniendo ID de sesión: " + e.getMessage());
    }
    return null; // O lanzar excepción si preferís
  }

  @GetMapping("/estadisticas")
  public String mostrarEstadisticas(@RequestParam(required = false) String handleIdColeccion, @RequestParam(required = false) String categoriaProvincia, @RequestParam(required = false) String categoriaHora, Model model) {
    try { model.addAttribute("colecciones", coleccionService.obtenerTodas()); } catch (Exception e) { model.addAttribute("colecciones", List.of()); }
    try { model.addAttribute("categorias", hechoService.getAvailableCategories()); } catch (Exception e) { model.addAttribute("categorias", List.of()); }
    try { model.addAttribute("distribucionCategorias", estadisticasService.getDistribucionCategorias()); model.addAttribute("spamRatio", estadisticasService.getSolicitudesSpamRatio()); } catch (Exception e) {}

    if (handleIdColeccion != null && !handleIdColeccion.isEmpty()) {
      try { model.addAttribute("resultadoProvinciaColeccion", estadisticasService.getDistribucionProvinciasPorColeccion(handleIdColeccion)); model.addAttribute("handleIdColeccionSeleccionada", handleIdColeccion); } catch (Exception e) {}
    }
    if (categoriaProvincia != null && !categoriaProvincia.isEmpty()) {
      try { model.addAttribute("resultadoProvinciaCategoria", estadisticasService.getDistribucionProvinciasPorCategoria(categoriaProvincia)); model.addAttribute("categoriaProvinciaSeleccionada", categoriaProvincia); } catch (Exception e) {}
    }
    if (categoriaHora != null && !categoriaHora.isEmpty()) {
      try { model.addAttribute("resultadoHoraCategoria", estadisticasService.getDistribucionHorasPorCategoria(categoriaHora)); model.addAttribute("categoriaHoraSeleccionada", categoriaHora); } catch (Exception e) {}
    }
    model.addAttribute("urlZipCompleto", estadisticasService.getExportUrlZipCompleto());
    model.addAttribute("urlExportarProvinciaColeccion", estadisticasService.getExportUrlProvinciaColeccion());
    model.addAttribute("urlExportarCategoriaHechos", estadisticasService.getExportUrlCategoriaHechos());
    model.addAttribute("urlExportarProvinciaCategoria", estadisticasService.getExportUrlProvinciaCategoria());
    model.addAttribute("urlExportarHoraCategoria", estadisticasService.getExportUrlHoraCategoria());
    model.addAttribute("urlExportarSpam", estadisticasService.getExportUrlSpam());
    model.addAttribute("titulo", "Estadísticas");
    return "estadisticas";
  }
}