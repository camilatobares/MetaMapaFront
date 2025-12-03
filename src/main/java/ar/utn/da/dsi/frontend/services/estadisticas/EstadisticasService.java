package ar.utn.da.dsi.frontend.services.estadisticas;

import ar.utn.da.dsi.frontend.client.dto.output.CategoriaReportadaListDTO;
import ar.utn.da.dsi.frontend.client.dto.output.HoraHechosPorCategoriaListDTO;
import ar.utn.da.dsi.frontend.client.dto.output.ProvinciaHechosPorCategoriaListDTO;
import ar.utn.da.dsi.frontend.client.dto.output.ProvinciaHechosPorColeccionListDTO;
import ar.utn.da.dsi.frontend.client.dto.output.SolicitudSpamDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EstadisticasService {

  private final EstadisticasApiClientService apiClient;

  @Autowired
  public EstadisticasService(EstadisticasApiClientService apiClient) {
    this.apiClient = apiClient;
  }

  // Métrica 1: Distribución de provincias por colección
  public ProvinciaHechosPorColeccionListDTO getDistribucionProvinciasPorColeccion(String handleId) {
    return apiClient.getDistribucionProvincias(handleId);
  }

  // Métrica 2: Distribución de categorías global
  public CategoriaReportadaListDTO getDistribucionCategorias() {
    return apiClient.getDistribucionCategorias();
  }

  // Métrica 3: Distribución de provincias por categoría
  public ProvinciaHechosPorCategoriaListDTO getDistribucionProvinciasPorCategoria(String categoria) {
    return apiClient.getDistribucionProvinciasPorCategoria(categoria);
  }

  // Métrica 4: Distribución de horas por categoría
  public HoraHechosPorCategoriaListDTO getDistribucionHorasPorCategoria(String categoria) {
    return apiClient.getDistribucionHorasPorCategoria(categoria);
  }

  // Métrica 5: Ratio de solicitudes spam
  public SolicitudSpamDTO getSolicitudesSpamRatio() {
    SolicitudSpamDTO so = apiClient.getSolicitudesSpamRatio();
    System.out.println("DEBUG: Servicio Estadísticas - Solicitudes Spam Ratio: " + so);
    return so;
  }

  // --- Métodos para la exportación (Devuelven URLs) ---
  public String getExportUrlZipCompleto() {
    return apiClient.getUrlExportarReporteCompletoZIP();
  }
  public String getExportUrlProvinciaColeccion() {
    return apiClient.getUrlExportarProvinciaColeccion();
  }
  public String getExportUrlCategoriaHechos() {
    return apiClient.getUrlExportarCategoriasHechos();
  }
  public String getExportUrlProvinciaCategoria() {
    return apiClient.getUrlExportarProvinciaCategoria();
  }
  public String getExportUrlHoraCategoria() {
    return apiClient.getUrlExportarHoraCategoria();
  }
  public String getExportUrlSpam() {
    return apiClient.getUrlExportarSpam();
  }
}