package ar.utn.da.dsi.frontend.services.colecciones;

import ar.utn.da.dsi.frontend.client.dto.input.ColeccionInputDTO;
import ar.utn.da.dsi.frontend.client.dto.output.ColeccionOutputDTO;
import ar.utn.da.dsi.frontend.services.ApiClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ColeccionApiService {
	private final ApiClientService apiClientService;
	private final String coleccionesApiUrl;

	@Autowired
	public ColeccionApiService(ApiClientService apiClientService, @Value("${agregador.colecciones.service.url}") String coleccionesApiUrl) {
		this.apiClientService = apiClientService;
		this.coleccionesApiUrl = coleccionesApiUrl;
	}

	public List<ColeccionOutputDTO> obtenerTodas() {
		List<ColeccionOutputDTO> ap = apiClientService.getListPublic(coleccionesApiUrl, ColeccionOutputDTO.class);
		System.out.println("DEBUG: Colecciones obtenidas: " + ap);
		return ap;
	}

	public ColeccionOutputDTO obtenerPorId(String id) {
		return apiClientService.getPublic(coleccionesApiUrl + "/" + id, ColeccionOutputDTO.class);
	}

	public void crear(ColeccionInputDTO dto) {
		// El endpoint de tu backend no devuelve la colección creada, así que usamos Void.class
		apiClientService.post(coleccionesApiUrl, dto, Void.class);
	}

	public void actualizar(String handleId, ColeccionInputDTO dto) {
		// Endpoint: PUT /colecciones/{handleId}
		String url = coleccionesApiUrl + "/" + handleId;

		apiClientService.put(url, dto, Void.class);
	}

	public void eliminar(String id, String visualizadorID) {
		String urlConParam = coleccionesApiUrl + "/" + id + "?visualizadorID=" + visualizadorID;
		apiClientService.delete(urlConParam);
	}

	/**
	 * Actualiza Título y Descripción (PUT /colecciones/{id})
	 */
	public void actualizarDatosBasicos(String id, ColeccionInputDTO dto) {
		apiClientService.put(coleccionesApiUrl + "/" + id, dto, Void.class);
	}

	/**
	 * Actualiza la configuración unificada (PUT /colecciones/{id}/editar)
	 */
	public void editarConfiguracionUnificada(String handleId, ColeccionInputDTO dto) {
		// Endpoint: PUT /colecciones/{handleId}/editar?visualizadorID={id}
		String url = coleccionesApiUrl + "/" + handleId + "/editar" + "?visualizadorID=" + dto.getVisualizadorID();

		Map<String, Object> body = new HashMap<>();

		if (dto.getAlgoritmoConsenso() != null) {
			body.put("algoritmoConsenso", dto.getAlgoritmoConsenso());
		}
		if (dto.getFuentes() != null) {
			body.put("fuentes", dto.getFuentes());
		}
		if (dto.getCriteriosPertenenciaNombres() != null && dto.getCriteriosPertenenciaValores() != null) {
			body.put("criteriosPertenenciaNombres", dto.getCriteriosPertenenciaNombres());
			body.put("criteriosPertenenciaValores", dto.getCriteriosPertenenciaValores());
		}
		if (dto.getIdDeHechosParaEliminar() != null) {
			body.put("idDeHechosParaEliminar", dto.getIdDeHechosParaEliminar());
		}

		if (!body.isEmpty()) {
			apiClientService.put(url, body, Void.class);
		}
	}

}