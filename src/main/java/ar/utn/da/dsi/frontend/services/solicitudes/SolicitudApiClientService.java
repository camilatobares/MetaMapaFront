package ar.utn.da.dsi.frontend.services.solicitudes;

import ar.utn.da.dsi.frontend.client.dto.ApiResponse;
import ar.utn.da.dsi.frontend.client.dto.input.SolicitudEliminacionInputDTO;
import ar.utn.da.dsi.frontend.client.dto.output.SolicitudEliminacionOutputDTO;
import ar.utn.da.dsi.frontend.services.ApiClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Service
public class SolicitudApiClientService {

	private final ApiClientService apiClientService;
	private final String solicitudesApiUrl;

	@Autowired
	public SolicitudApiClientService(ApiClientService apiClientService, @Value("${agregador.solicitudes.service.url}") String solicitudesApiUrl) {
		this.apiClientService = apiClientService;
		this.solicitudesApiUrl = solicitudesApiUrl;
	}

	public List<SolicitudEliminacionOutputDTO> obtenerTodasParaAdmin() {
		return apiClientService.executeWithToken(accessToken ->
				apiClientService.getWebClient().get()
						.uri(solicitudesApiUrl)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.retrieve()
						.bodyToMono(new ParameterizedTypeReference<ApiResponse<List<SolicitudEliminacionOutputDTO>>>() {})
						.map(response -> response.getDatos())
						.block()
		);
	}

	public List<SolicitudEliminacionOutputDTO> obtenerTodas(String visualizadorId) {
		String url = solicitudesApiUrl + "/mis-solicitudes";
		return apiClientService.executeWithToken(accessToken ->
				apiClientService.getWebClient().get()
						.uri(url)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.retrieve()
						.bodyToMono(new ParameterizedTypeReference<ApiResponse<List<SolicitudEliminacionOutputDTO>>>() {})
						.map(response -> response.getDatos())
						.block()
		);
	}

	public SolicitudEliminacionOutputDTO obtenerPorId(Integer id, String visualizadorId) {
		String url = solicitudesApiUrl + "/" + id;

		// Usamos ParameterizedTypeReference para capturar ApiResponse<DTO>
		return apiClientService.executeWithToken(accessToken ->
				apiClientService.getWebClient().get()
						.uri(url)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.retrieve()
						.bodyToMono(new ParameterizedTypeReference<ApiResponse<SolicitudEliminacionOutputDTO>>() {})
						.map(response -> response.getDatos()) // Extraemos el objeto real de 'datos'
						.block()
		);
	}

	public SolicitudEliminacionOutputDTO crear(SolicitudEliminacionInputDTO dto) {

		// 1. Obtener el token de la sesión (será null/blank si es anónimo)
		String accessToken = apiClientService.getAccessTokenFromSession();

		// 2. Iniciar la construcción de la solicitud (POST)
		WebClient.RequestBodyUriSpec requestBuilder = apiClientService.getWebClient().post();

		// 3. Obtener el objeto que permite añadir headers y cuerpo.
		// Usamos una variable final local para que pueda ser capturada por el bloque de mapeo.
		WebClient.RequestHeadersSpec<?> requestHeadersSpec = requestBuilder
				.uri(solicitudesApiUrl)
				.bodyValue(dto);

		// 4. Aplicar el header de autenticación SOLO si existe un token
		if (accessToken != null && !accessToken.isBlank()) {
			// Al llamar a header sobre requestHeadersSpec, obtenemos una nueva especificación con el header añadido.
			// PERO debemos forzar el tipo a WebClient.RequestHeadersSpec para evitar errores de tipo genérico en el compilador.
			requestHeadersSpec = requestHeadersSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
		}

		// 5. Ejecutar la llamada (con o sin token) y manejar la respuesta
		try {
			// Usamos ParameterizedTypeReference para desempaquetar el ApiResponse<SolicitudEliminacionOutputDTO>
			SolicitudEliminacionOutputDTO s = requestHeadersSpec
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<ApiResponse<SolicitudEliminacionOutputDTO>>() {})
					.map(response -> response.getDatos())
					.block();

			return s;
		} catch (Exception e) {
			// Capturamos cualquier error (incluidos los de comunicación HTTP/WebClient)
			throw new RuntimeException("Error al crear la solicitud en el backend.", e);
		}
	}

	public SolicitudEliminacionOutputDTO aceptar(Integer id, String visualizadorId) {
		String url = solicitudesApiUrl + "/" + id + "?aceptado=true";
		// Corrección del desempaquetado
		return apiClientService.executeWithToken(accessToken ->
				apiClientService.getWebClient().put()
						.uri(url)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.retrieve()
						.bodyToMono(new ParameterizedTypeReference<ApiResponse<SolicitudEliminacionOutputDTO>>() {})
						.map(response -> response.getDatos())
						.block()
		);
	}

	public SolicitudEliminacionOutputDTO rechazar(Integer id, String visualizadorId) {
		String url = solicitudesApiUrl + "/" + id + "?aceptado=false";
		// Corrección del desempaquetado
		return apiClientService.executeWithToken(accessToken ->
				apiClientService.getWebClient().put()
						.uri(url)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.retrieve()
						.bodyToMono(new ParameterizedTypeReference<ApiResponse<SolicitudEliminacionOutputDTO>>() {})
						.map(response -> response.getDatos())
						.block()
		);
	}
}