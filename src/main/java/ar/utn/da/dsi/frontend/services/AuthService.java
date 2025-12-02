package ar.utn.da.dsi.frontend.services;

import ar.utn.da.dsi.frontend.client.dto.AuthResponseDTO;
import ar.utn.da.dsi.frontend.client.dto.RolesPermisosDTO;
import ar.utn.da.dsi.frontend.client.dto.input.RegistroInputDTO;
import ar.utn.da.dsi.frontend.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);
	private final WebClient webClient;
	private final ApiClientService apiClientService;
	private final String authServiceUrl;

	@Autowired
	public AuthService(ApiClientService apiClientService, @Value("${auth.service.url}") String authServiceUrl) {
		this.webClient = WebClient.builder().build();
		this.apiClientService = apiClientService;
		this.authServiceUrl = authServiceUrl;
	}

	public AuthResponseDTO login(String username, String password) {
		try {
			return webClient
					.post()
					.uri(authServiceUrl + "/login")
					.bodyValue(Map.of("email", username, "password", password))
					.retrieve()
					.bodyToMono(AuthResponseDTO.class)
					.block();
		} catch (WebClientResponseException e) {
			log.error("Error en login: " + e.getMessage());

			// SOLO si el usuario no existe (404) devolvemos null.
			// Si es 401 (Pass incorrecta), 403 (Bloqueado) o 429 (Rate Limit),
			// LANZAMOS la excepción para que el CustomAuthProvider pueda leer los headers.
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				return null;
			}

			// Envolvemos la excepción original
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException("Error de conexión", e);
		}
	}

	// ... (El resto de los métodos déjalos igual: getRolesPermisos, registrar, etc.) ...
	public RolesPermisosDTO getRolesPermisos(String accessToken) {
		try {
			return apiClientService.getWithAuth(authServiceUrl + "/user/roles-permisos", accessToken, RolesPermisosDTO.class);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new RuntimeException("Error al obtener roles y permisos", e);
		}
	}

	public void registrar(RegistroInputDTO dto) {
		try {
			Map<String, Object> requestBody = Map.of(
					"email", dto.getEmail(),
					"contrasenia", dto.getPassword(),
					"nombre", dto.getNombre(),
					"apellido", dto.getApellido(),
					"fechaDeNacimiento", dto.getFechaNacimiento().toString(),
					"admin", false
			);
			webClient.post().uri(authServiceUrl + "/register").bodyValue(requestBody).retrieve().bodyToMono(Void.class).block();
		} catch (WebClientResponseException e) {
			if (e.getStatusCode() == HttpStatus.CONFLICT) {
				throw new ValidationException("El email '" + dto.getEmail() + "' ya está registrado.");
			}
			throw new RuntimeException("Error en registro: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new RuntimeException("Error de conexión en registro", e);
		}
	}

	public AuthResponseDTO actualizarPerfil(RegistroInputDTO dto) {
		try {
			Map<String, Object> requestBody = Map.of(
					"nombre", dto.getNombre(),
					"apellido", dto.getApellido(),
					"fechaDeNacimiento", dto.getFechaNacimiento().toString(),
					"email", dto.getEmail()
			);
			return apiClientService.put(authServiceUrl + "/user/profile", requestBody, AuthResponseDTO.class);
		} catch (Exception e) {
			throw new RuntimeException("Error al actualizar perfil", e);
		}
	}
}