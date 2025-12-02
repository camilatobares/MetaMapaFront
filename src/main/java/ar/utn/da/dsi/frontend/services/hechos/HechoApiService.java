package ar.utn.da.dsi.frontend.services.hechos;

import ar.utn.da.dsi.frontend.client.dto.HechoDTO;
import ar.utn.da.dsi.frontend.client.dto.PaginaDTO;
import ar.utn.da.dsi.frontend.client.dto.input.HechoInputDTO;
import ar.utn.da.dsi.frontend.client.dto.output.EdicionOutputDTO;
import ar.utn.da.dsi.frontend.services.ApiClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.lang.Nullable;
import ar.utn.da.dsi.frontend.client.dto.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class HechoApiService {

  private final ApiClientService apiClientService;
  private final String dinamicaUrl;
  private final String estaticaApiUrl;
  private final WebClient webClient;
  private final ObjectMapper objectMapper;
  private final String  agregadorApiUrl;

  @Autowired
  public HechoApiService(ApiClientService apiClientService, @Value("${dinamica.service.url}") String dinamicaUrl, @Value("${estatica.service.url}") String estaticaApiUrl, @Value("${agregador.service.url}") String agregadorApiUrl) {
    this.apiClientService = apiClientService;
    this.dinamicaUrl = dinamicaUrl;
    this.estaticaApiUrl = estaticaApiUrl;
    this.webClient = WebClient.builder().build();
    this.objectMapper = new ObjectMapper();
    this.objectMapper.findAndRegisterModules();
    this.agregadorApiUrl = agregadorApiUrl;
  }

  public List<HechoDTO> getHistorialHechos() {
    String url = dinamicaUrl + "/hechos/historial";
    return apiClientService.executeWithToken(accessToken ->
        webClient.get().uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<HechoDTO>>>() {})
            .map(r -> r.getDatos() != null ? r.getDatos() : List.<HechoDTO>of())
            .block()
    );
  }

  public List<EdicionOutputDTO> getHistorialEdiciones() {
    String url = dinamicaUrl + "/ediciones/historial";
    return apiClientService.executeWithToken(accessToken ->
        webClient.get().uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<EdicionOutputDTO>>>() {})
            .map(r -> r.getDatos() != null ? r.getDatos() : List.<EdicionOutputDTO>of())
            .block()
    );
  }

  /**
   * Llama al backend para obtener los hechos de una colección.
   * Mapea filtros a los endpoints específicos del Agregador.
   */
  public PaginaDTO<HechoDTO> getHechosDeColeccion(String handleId, String modo, String fechaDesde, String fechaHasta, String categoria, String titulo, int page) {

    boolean hayFiltrosDetallados = (fechaDesde != null && !fechaDesde.isEmpty()) ||
        (fechaHasta != null && !fechaHasta.isEmpty()) ||
        (categoria != null && !categoria.isEmpty()) ||
        (titulo != null && !titulo.isEmpty());

    String urlBase;

    // Lógica de selección de endpoint
    if (hayFiltrosDetallados) {
      // Usamos el endpoint de FILTRADO del Agregador
      urlBase = agregadorApiUrl + "/colecciones/" + handleId + "/hechos/filtrar";
    } else if (modo != null && !modo.isEmpty()) {
      // Usamos el endpoint de NAVEGACION del Agregador
      urlBase = agregadorApiUrl + "/colecciones/" + handleId + "/hechos/navegacion";
    } else {
      // Por defecto, usamos navegación si no hay nada específico
      urlBase = agregadorApiUrl + "/colecciones/" + handleId + "/hechos/navegacion";
    }

    // Usamos UriComponentsBuilder para construir la URL con los parámetros
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlBase);

    // 1. Agregar parámetros de Paginación (siempre se envían)
    builder.queryParam("page", page);
    builder.queryParam("size", 20); // Tamaño de página fijo a 20

    // 2. Agregar parámetros según el tipo de endpoint
    if (urlBase.contains("/hechos/navegacion")) {
      // Lógica para modo Curado/Irrestricto
      if ("curada".equalsIgnoreCase(modo)) {
        builder.queryParam("esModoCurado", true);
      } else {
        builder.queryParam("esModoCurado", false); // Irrestricta por defecto
      }
    } else if (urlBase.contains("/hechos/filtrar")) {
      // Lógica de Filtros (Validación Individual)
      if (fechaDesde != null && !fechaDesde.isEmpty()) {
        builder.queryParam("fechaInicio", fechaDesde);
      }
      if (fechaHasta != null && !fechaHasta.isEmpty()) {
        builder.queryParam("fechaFin", fechaHasta);
      }
      if (categoria != null && !categoria.isEmpty()) {
        builder.queryParam("categoria", categoria);
      }
      if (titulo != null && !titulo.isEmpty()) {
        builder.queryParam("titulo", titulo);
      }
    }

    String urlCompleta = builder.toUriString();

    // Llamada al servicio base esperando un PaginaDTO
    // Nota: Necesitas usar ParameterizedTypeReference para manejar los genéricos correctamente
    return apiClientService.getWebClient().get()
        .uri(builder.toUriString())
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<PaginaDTO<HechoDTO>>() {})
        .block();
  }



  // GET /colecciones/{handleID}/hechos/{hechoID} (Obtener Hecho Específico)
  /**
   * Llama al Agregador para obtener un hecho específico de una colección.
   */
  public HechoDTO getHechoEspecificoDeColeccion(String handleId, String hechoId) {
    // Endpoint GET /colecciones/{handleID}/hechos/{hechoID}
    // Se asume que el hecho puede ser recuperado con el DTO existente.
    String url = agregadorApiUrl + "/colecciones/" + handleId + "/hechos/" + hechoId;

    // Es una ruta de consulta pública (asumimos, como el resto de las consultas a hechos)
    return apiClientService.getPublic(url, HechoDTO.class);
  }

  // Método auxiliar para la navegación irrestricta por defecto
  private List<HechoDTO> getHechosDeColeccionSinFiltros(String handleId) {
    String url = agregadorApiUrl + "/colecciones/" + handleId + "/hechos/navegacion?esModoCurado=false";
    return apiClientService.getListPublic(url, HechoDTO.class);
  }

  /**
   * Llama al backend para obtener los hechos de un usuario.
   * (Usado por ContributorController para el panel "Mis Hechos")
   */
  public List<HechoDTO> getHechosPorUsuario(String userId) {
    String url = dinamicaUrl + "/hechos/usuario/" + userId;

    // USAMOS executeWithToken PARA INYECTAR EL TOKEN AUTOMÁTICAMENTE
    return apiClientService.executeWithToken(accessToken ->
        webClient.get()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            // ACÁ ESTÁ LA MAGIA: Le decimos que esperamos un ApiResponse que contiene una Lista
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<HechoDTO>>>() {})
            .map(response -> {
              if (response.getDatos() == null) return List.<HechoDTO>of();
              return response.getDatos();
            })
            .block()
    );
  }

  /**
   * Llama al backend para obtener las categorías de hechos disponibles.
   * (Usado por WebController para los filtros en la página /facts)
   */
  public List<String> getAvailableCategories() {
    // El endpoint del agregador es /hechos/categorias
    String url = agregadorApiUrl + "/hechos/categorias";
    String[] categoriasArray = apiClientService.getPublic(url, String[].class);

    if (categoriasArray == null) {
      return List.of();
    }

    return Arrays.asList(categoriasArray);
  }

  public Map<String, String> getConsensusLabels() {
    // CORRECCIÓN: Apuntamos al Agregador (/colecciones/metadata/consensus)
    String url = agregadorApiUrl + "/colecciones/metadata/consensus"; //

    return apiClientService.get(url, Map.class);
  }

  public List<String> getAvailableSources() {
    // Asumo que el endpoint del backend es /hechos/metadata/sources
    String url = dinamicaUrl + "/metadata/sources";
    return apiClientService.getList(url, String.class);
  }


  //LO PIDE EB AGREGADOR PARA MOSTRAR DETALLES DE HECHO - Lo usa el mapa público y el detalle de hechos publicados.
  public HechoDTO getHechoPorId(Long id) {
    String url = agregadorApiUrl + "/hechos/" + id;

    try {
      // CORRECCIÓN: Se elimina el executeWithToken y el Header.
      // La llamada se hace directamente a webClient (no autenticada) pero
      // se mantiene la lógica para desempaquetar la respuesta ApiResponse.
      return webClient.get()
          .uri(url)
          .retrieve()
          .bodyToMono(new ParameterizedTypeReference<ApiResponse<HechoDTO>>() {})
          .flatMap(response -> {
            // Si 'datos' es null (ej: error 404/500 con wrapper), se retorna Mono.empty() (que .block() convierte en null).
            if (response.getDatos() == null) {
              return Mono.empty();
            }
            return Mono.just(response.getDatos());
          })
          .block();
    } catch (Exception e) {
      System.out.println("Error al buscar hecho por ID " + id + ": " + e.getMessage());
      return null;
    }
  }

  // Lo usaremos en el Controller para: Editar, Revisar Solicitud, Mis Hechos Pendientes.
  public HechoDTO getHechoDinamicaPorId(Long id) {
    String url = dinamicaUrl + "/hechos/" + id;

    try {
      return apiClientService.executeWithToken(accessToken ->
          webClient.get()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
              .retrieve()
              // Si Dinámica devuelve ApiResponse, úsalo. Si devuelve objeto directo, usa HechoDTO.class
              .bodyToMono(new ParameterizedTypeReference<ApiResponse<HechoDTO>>() {})
              .map(response -> {
                if (response == null || response.getDatos() == null) {
                  throw new RuntimeException("El hecho no devolvió datos (Posiblemente rechazado/oculto).");
                }
                return response.getDatos();
              })
              .block()
      );
    } catch (Exception e) {
      System.out.println("Error buscando en Dinámica ID " + id + ": " + e.getMessage());
      return null;
    }
  }

  /**
   * CREAR HECHO DINÁMICO: Envía la solicitud como Multipart (JSON + Archivo).
   * Soporta usuarios logueados (con token) y anónimos (sin token).
   */
  public HechoDTO crearHecho(HechoInputDTO dto, @Nullable MultipartFile archivo) {
    String url = dinamicaUrl + "/hechos";

    try {
      String hechoJson = objectMapper.writeValueAsString(dto);

      MultipartBodyBuilder builder = new MultipartBodyBuilder();
      builder.part("hechoData", hechoJson, MediaType.APPLICATION_JSON);

      if (archivo != null && !archivo.isEmpty()) {
        builder.part("archivo", archivo.getResource());
      }

      // 1. Intentamos obtener el token de la sesión
      String accessToken = apiClientService.getAccessTokenFromSession();

      // 2. Configuramos el WebClient base (POST y Body)
      var requestSpec = webClient.post()
          .uri(url)
          .contentType(MediaType.MULTIPART_FORM_DATA) // Importante para multipart
          .body(BodyInserters.fromMultipartData(builder.build()));

      // 3. Si hay token, agregamos el Header. Si no, va sin header.
      if (accessToken != null && !accessToken.isBlank()) {
        requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
      }

      // 4. Ejecutamos
      return requestSpec
          .retrieve()
          .bodyToMono(HechoDTO.class)
          .block();

    } catch (Exception e) {
      throw new RuntimeException("Error al crear Hecho en Fuente Dinámica: " + e.getMessage(), e);
    }
  }

  /**
   * ACTUALIZAR HECHO DINÁMICO: Envía la solicitud de edición como Multipart (JSON + Archivo) al endpoint /hechos/{id}/editar.
   */
  public HechoDTO actualizarHecho(Long id, HechoInputDTO dto, @Nullable MultipartFile archivo) {
    String url = dinamicaUrl + "/hechos/" + id + "/editar"; // Endpoint: PUT /hechos/{id}/editar

    Map<String, Object> edicionData = new LinkedHashMap<>();
    edicionData.put("tituloPropuesto", dto.getTitulo());
    edicionData.put("descripcionPropuesta", dto.getDescripcion());

    if (dto.getCategoria() != null) {
      edicionData.put("categoriaPropuesta", dto.getCategoria());
    }

    edicionData.put("latitudPropuesta", dto.getLatitud());
    edicionData.put("longitudPropuesta", dto.getLongitud());
    edicionData.put("fechaAcontecimientoPropuesta", dto.getFechaAcontecimiento());

    try {
      String edicionJson = objectMapper.writeValueAsString(edicionData);

      MultipartBodyBuilder builder = new MultipartBodyBuilder();
      // Parte JSON: "edicionData" con Content-Type application/json
      builder.part("edicionData", edicionJson, MediaType.APPLICATION_JSON);

      // Parte Archivo: "archivo" si existe
      if (archivo != null && !archivo.isEmpty()) {
        builder.part("archivo", archivo.getResource());
      }

      return apiClientService.executeWithToken(accessToken ->
          webClient.put()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
              .body(BodyInserters.fromMultipartData(builder.build()))
              .retrieve()
              .bodyToMono(HechoDTO.class)
              .block()
      );

    } catch (Exception e) {
      throw new RuntimeException("Error al actualizar Hecho en Fuente Dinámica: " + e.getMessage(), e);
    }
  }

  //Envía un POST multipart/form-data para importar un CSV
  public void importarCsv(MultipartFile file) {
    String url = estaticaApiUrl + "/cargar-csv";

    String accessToken = apiClientService.getAccessTokenFromSession();
    if (accessToken == null) {
      throw new RuntimeException("No hay token de acceso disponible para la subida");
    }

    try {
      webClient.post()
          .uri(url)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(BodyInserters.fromMultipartData("archivoCSV", file.getResource()))
          .retrieve()
          .bodyToMono(Void.class)
          .block();
    } catch (Exception e) {
      throw new RuntimeException("Error al importar CSV: " + e.getMessage(), e);
    }
  }

  // TRAER HECHOS PENDIENTES (Para Admin)
  public List<HechoDTO> getHechosPendientes() {
    String url = dinamicaUrl + "/hechos/pendientes"; // http://localhost:8082/dinamica/hechos/pendientes

    return apiClientService.executeWithToken(accessToken ->
        apiClientService.getWebClient().get() // Usamos el getter del WebClient
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<HechoDTO>>>() {})
            .map(response -> response.getDatos() != null ? response.getDatos() : List.<HechoDTO>of())
            .block()
    );
  }

  // APROBAR HECHO
  public void aprobarHecho(Long id) {
    String url = dinamicaUrl + "/hechos/" + id + "/aprobar";
    apiClientService.put(url, "", Void.class);
  }

  public void aprobarHechoConSugerencias(Long id, String sugerencia) {
    String url = dinamicaUrl + "/hechos/" + id + "/aceptar-con-sugerencia";

    Map<String, String> body = Map.of("sugerencia", sugerencia);

    apiClientService.put(url, body, Void.class);
  }

  // OPCIONAL: RECHAZAR CON MOTIVO
  public void rechazarHechoConMotivo(Long id, String motivo) {
    String url = dinamicaUrl + "/hechos/" + id + "/rechazar";
    Map<String, String> body = Map.of("detalle", motivo);
    apiClientService.put(url, body, Void.class);
  }

  // RECHAZAR HECHO
  public void rechazarHecho(Long id) {
    // URL esperada: http://localhost:8082/dinamica/hechos/{id}/rechazar
    String url = dinamicaUrl + "/hechos/" + id + "/rechazar";
    apiClientService.put(url, null, Void.class);
  }

  public List<EdicionOutputDTO> getEdicionesPendientes() {
    // URL: http://localhost:8082/dinamica/ediciones/pendientes
    String url = dinamicaUrl + "/ediciones/pendientes";

    return apiClientService.executeWithToken(accessToken ->
        apiClientService.getWebClient().get()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<EdicionOutputDTO>>>() {})
            .map(response -> response.getDatos() != null ? response.getDatos() : List.<EdicionOutputDTO>of())
            .block()
    );
  }

  public void aceptarEdicion(Long idEdicion) {
    // URL: http://localhost:8082/dinamica/ediciones/{id}/aceptar
    String url = dinamicaUrl + "/ediciones/" + idEdicion + "/aceptar";

    apiClientService.executeWithToken(accessToken ->
        apiClientService.getWebClient().put()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(Void.class)
            .block()
    );
  }

  public void rechazarEdicion(Long idEdicion) {
    // URL: http://localhost:8082/dinamica/ediciones/{id}/rechazar
    String url = dinamicaUrl + "/ediciones/" + idEdicion + "/rechazar";

    apiClientService.executeWithToken(accessToken ->
        apiClientService.getWebClient().put()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(Void.class)
            .block()
    );
  }

  public EdicionOutputDTO buscarEdicionPorId(Long id) {
    String url = dinamicaUrl + "/ediciones/" + id; // e.g. http://localhost:8082/dinamica/ediciones/{id}

    return apiClientService.executeWithToken(accessToken ->
        apiClientService.getWebClient().get()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<EdicionOutputDTO>>() {})
            .map(apiResp -> apiResp.getDatos())   // <-- DESempaquetar "datos"
            .block()
    );
  }

  public List<EdicionOutputDTO> getEdicionesPorUsuario(String userId) {
    String url = dinamicaUrl + "/ediciones/usuario/" + userId;

    return apiClientService.executeWithToken(accessToken ->
        apiClientService.getWebClient().get()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<EdicionOutputDTO>>>() {})
            .map(response -> response.getDatos() != null ? response.getDatos() : List.<EdicionOutputDTO>of())
            .block()
    );
  }
}