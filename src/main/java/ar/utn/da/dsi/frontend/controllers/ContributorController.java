package ar.utn.da.dsi.frontend.controllers;

import ar.utn.da.dsi.frontend.client.dto.HechoDTO;
import ar.utn.da.dsi.frontend.client.dto.output.EdicionOutputDTO;
import ar.utn.da.dsi.frontend.client.dto.output.SolicitudEliminacionOutputDTO;
import ar.utn.da.dsi.frontend.client.dto.output.SolicitudUnificadaDTO;
import ar.utn.da.dsi.frontend.services.hechos.HechoService;
import ar.utn.da.dsi.frontend.services.solicitudes.SolicitudService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/contributor")
public class ContributorController {

  @Value("${dinamica.service.url}")
  private String dinamicaUrl;

  private final SolicitudService solicitudService;
  private final HechoService hechoService;

  public ContributorController(SolicitudService solicitudService, HechoService hechoService) {
    this.solicitudService = solicitudService;
    this.hechoService = hechoService;
  }

  @GetMapping
  public String mostrarPanelContribuyente(Model model) {

    List<HechoDTO> todosMisHechos = new ArrayList<>();
    try {
      todosMisHechos = hechoService.buscarHechosPorUsuario();
    } catch (Exception e) {
      System.out.println("Error buscando hechos: " + e.getMessage());
    }

    List<SolicitudEliminacionOutputDTO> misBajas = new ArrayList<>();
    try {
      misBajas = solicitudService.obtenerTodas();
    } catch (Exception e) {
      System.out.println("Error buscando solicitudes de baja: " + e.getMessage());
    }

    List<EdicionOutputDTO> misEdiciones = new ArrayList<>();
    try {
      misEdiciones = hechoService.buscarEdicionesPorUsuario();
    } catch (Exception e) {
      System.out.println("Error buscando ediciones: " + e.getMessage());
    }

    // --- TABLA 1: "Mis Hechos Publicados" ---
    List<HechoDTO> hechosPublicados = todosMisHechos.stream()
        .filter(h -> "ACEPTADO".equals(h.getEstado()) || "ACEPTADO_CON_SUGERENCIAS".equals(h.getEstado()))
        .collect(Collectors.toList());

    model.addAttribute("misHechos", hechosPublicados);


    // --- TABLA 2: "Estado de Solicitudes" (Lista Unificada) ---
    List<SolicitudUnificadaDTO> listaUnificada = new ArrayList<>();

    // 1. Agregar CREACIONES (Nuevos Hechos)
    for (HechoDTO h : todosMisHechos) {
      // USAMOS GETTERS AQUÍ
      String estadoHecho = (h.getEstado() != null) ? h.getEstado() : "EN_REVISION";

      listaUnificada.add(new SolicitudUnificadaDTO(
          h.getId(),
          h.getTitulo(),
          "Nuevo Hecho",
          estadoHecho
      ));
    }

    // 2. Agregar ELIMINACIONES (Bajas)
    for (SolicitudEliminacionOutputDTO baja : misBajas) {
      listaUnificada.add(new SolicitudUnificadaDTO(
          baja.nroDeSolicitud(),
          baja.nombreHecho(),
          "Eliminación",
          baja.estado()
      ));
    }

    // 3. Agregar EDICIONES (Modificaciones)
    for (EdicionOutputDTO edi : misEdiciones) {
      String tituloMostrar = (edi.getTituloPropuesto() != null)
          ? edi.getTituloPropuesto()
          : "Edición Hecho ID " + edi.getIdHechoOriginal();

      listaUnificada.add(new SolicitudUnificadaDTO(
          edi.getId(),
          tituloMostrar,
          "Edición",
          edi.getEstado()
      ));
    }

    model.addAttribute("misSolicitudes", listaUnificada);

    String baseUrl = dinamicaUrl.replace("/dinamica", "");
    model.addAttribute("backendImagesUrl", baseUrl);

    return "contributor";
  }
}