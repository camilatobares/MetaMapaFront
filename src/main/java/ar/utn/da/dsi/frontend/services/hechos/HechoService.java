package ar.utn.da.dsi.frontend.services.hechos;

import ar.utn.da.dsi.frontend.client.dto.HechoDTO;
import ar.utn.da.dsi.frontend.client.dto.PaginaDTO;
import ar.utn.da.dsi.frontend.client.dto.input.HechoInputDTO;
import ar.utn.da.dsi.frontend.client.dto.output.EdicionOutputDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@Service
public class HechoService {

  private final HechoApiService apiClient;

  @Autowired
  public HechoService(HechoApiService apiClient) {
    this.apiClient = apiClient;
  }

  // --- MÉTODOS PARA HISTORIAL (TRAZABILIDAD) ---
  public List<HechoDTO> buscarHistorialHechos() {
    return apiClient.getHistorialHechos();
  }

  public List<EdicionOutputDTO> buscarHistorialEdiciones() {
    return apiClient.getHistorialEdiciones();
  }
  // --------------------------------------------

  public PaginaDTO<HechoDTO> getHechosDeColeccion(String handleId, String modo, String fechaDesde, String fechaHasta, String categoria, String titulo, int page) {
    return apiClient.getHechosDeColeccion(handleId, modo, fechaDesde, fechaHasta, categoria, titulo, page);
  }

  public List<HechoDTO> buscarHechosPorUsuario(String userId) {
    return apiClient.getHechosPorUsuario(userId);
  }

  public List<String> getAvailableCategories() {
    return apiClient.getAvailableCategories();
  }

  public Map<String, String> getConsensusLabels() {
    return apiClient.getConsensusLabels();
  }

  public List<String> getAvailableSources() {
    return apiClient.getAvailableSources();
  }

  public void crear(HechoInputDTO dto, @Nullable MultipartFile archivo) {
    apiClient.crearHecho(dto, archivo);
  }

  public HechoInputDTO getHechoAgregadorDTOporId(Long id) {
    HechoDTO dtoApi = apiClient.getHechoPorId(id);

    HechoInputDTO dtoForm = new HechoInputDTO();
    if (dtoApi != null) {
      dtoForm.setId(dtoApi.getId()); // .getId()
      dtoForm.setTitulo(dtoApi.getTitulo()); // .getTitulo()
      dtoForm.setDescripcion(dtoApi.getDescripcion());
      dtoForm.setCategoria(dtoApi.getCategoria());
      dtoForm.setFechaAcontecimiento(dtoApi.getFechaAcontecimiento());
      dtoForm.setLatitud(dtoApi.getLatitud());
      dtoForm.setLongitud(dtoApi.getLongitud());
      dtoForm.setContenidoMultimedia(dtoApi.getContenidoMultimedia());
      dtoForm.setCollectionHandle(dtoApi.getCollectionHandle());

      if (dtoApi.getUserId() != null) {
        dtoForm.setVisualizadorID(String.valueOf(dtoApi.getUserId()));
      }
    }
    return dtoForm;
  }

  public HechoDTO buscarHechoEnDinamica(Long id) {
    return apiClient.getHechoDinamicaPorId(id);
  }

  public HechoInputDTO buscarHechoInputEnDinamica(Long id) {
    HechoDTO dtoApi = apiClient.getHechoDinamicaPorId(id);

    HechoInputDTO dtoForm = new HechoInputDTO();
    if (dtoApi != null) {
      dtoForm.setId(dtoApi.getId()); // .getId()
      dtoForm.setTitulo(dtoApi.getTitulo()); // .getTitulo()
      dtoForm.setDescripcion(dtoApi.getDescripcion());
      dtoForm.setCategoria(dtoApi.getCategoria());
      dtoForm.setFechaAcontecimiento(dtoApi.getFechaAcontecimiento());
      dtoForm.setLatitud(dtoApi.getLatitud());
      dtoForm.setLongitud(dtoApi.getLongitud());
      dtoForm.setContenidoMultimedia(dtoApi.getContenidoMultimedia());
      dtoForm.setCollectionHandle(dtoApi.getCollectionHandle());

      if (dtoApi.getUserId() != null) {
        dtoForm.setVisualizadorID(String.valueOf(dtoApi.getUserId()));
      }
    }
    return dtoForm;
  }


  public HechoDTO actualizar(Long id, HechoInputDTO dto, @Nullable MultipartFile archivo) {
    return apiClient.actualizarHecho(id, dto, archivo);
  }

  public void importarCsv(MultipartFile file) {
    apiClient.importarCsv(file);
  }

  public List<HechoDTO> buscarHechosPendientes() {
    return apiClient.getHechosPendientes();
  }

  public void aprobar(Long id) {
    apiClient.aprobarHecho(id);
  }

  public void rechazar(Long id) {
    apiClient.rechazarHecho(id);
  }

  public void aprobarConSugerencias(Long id, String sugerencia) {
    apiClient.aprobarHechoConSugerencias(id, sugerencia);
  }

  public void rechazarConMotivo(Long id, String motivo) {
    apiClient.rechazarHechoConMotivo(id, motivo);
  }

  public List<EdicionOutputDTO> buscarEdicionesPendientes() {
    return apiClient.getEdicionesPendientes();
  }

  public void aceptarEdicion(Long id) {
    apiClient.aceptarEdicion(id);
  }

  public void rechazarEdicion(Long id) {
    apiClient.rechazarEdicion(id);
  }

  public EdicionOutputDTO buscarEdicionPorId(Long id) {
    return apiClient.buscarEdicionPorId(id);
  }

  public List<EdicionOutputDTO> buscarEdicionesPorUsuario(String userId) {
    return apiClient.getEdicionesPorUsuario(userId);
  }

  public HechoDTO buscarHechoCompleto(Long id) {
    // Llama a tu API Client para traer el hecho por ID
    return apiClient.getHechoPorId(id);
  }
}