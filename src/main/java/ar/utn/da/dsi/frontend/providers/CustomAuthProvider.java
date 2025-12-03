package ar.utn.da.dsi.frontend.providers;

import ar.utn.da.dsi.frontend.client.dto.AuthResponseDTO;
import ar.utn.da.dsi.frontend.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomAuthProvider implements AuthenticationProvider {

	private static final Logger log = LoggerFactory.getLogger(CustomAuthProvider.class);
	private final AuthService authService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	public CustomAuthProvider(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = authentication.getName();
		String password = authentication.getCredentials().toString();

		try {
			AuthResponseDTO authResponse = authService.login(username, password);

			if (authResponse == null) {
				throw new BadCredentialsException("El usuario no existe.");
			}

			// --- LOGICA DE ÉXITO ---
			String accessToken = authResponse.getToken();
			String rolBackend = authResponse.getRolesPermisos().getNombreRol();
			String feRole = rolBackend.equalsIgnoreCase("ADMIN") ? "admin" : "contributor";

			String userJson = String.format(
					"{\"id\":%d,\"name\":\"%s\",\"lastName\":\"%s\",\"role\":\"%s\",\"email\":\"%s\",\"birthDate\":\"%s\"}",
					authResponse.getId(),
					authResponse.getNombre().replace("\"", "\\\""),
					authResponse.getApellido().replace("\"", "\\\""),
					feRole,
					authResponse.getEmail(),
					authResponse.getFechaDeNacimiento().toString()
			);

			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
			HttpSession session = attributes.getRequest().getSession(true);
			session.setAttribute("accessToken", accessToken);
			session.setAttribute("userJson", userJson);
			session.setAttribute("userRole", feRole);

			List<GrantedAuthority> authorities = new ArrayList<>();
			if (authResponse.getRolesPermisos().getPermisos() != null) {
				authResponse.getRolesPermisos().getPermisos().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
			}
			authorities.add(new SimpleGrantedAuthority("ROLE_" + feRole.toUpperCase()));

			return new UsernamePasswordAuthenticationToken(authResponse, accessToken, authorities);

		} catch (RuntimeException e) {
			// --- AQUÍ ES DONDE SUCEDE LA MAGIA ---
			Throwable cause = e.getCause(); // Desempaquetamos el error

			// 1. RATE LIMIT (429) -> BANEO
			if (cause instanceof WebClientResponseException.TooManyRequests ex) {
        try {
					// Intentamos leer el JSON {"retryAfter": 60}
					String responseBody = ex.getResponseBodyAsString();
					long seconds = objectMapper.readTree(responseBody).get("retryAfter").asLong();
					throw new AuthenticationServiceException("BAN:" + seconds);
				} catch (Exception jsonEx) {
					throw new AuthenticationServiceException("Demasiados intentos. Intente más tarde.");
				}
			}

			// 2. ERROR DE CREDENCIALES (401 o 403) -> INTENTOS RESTANTES
			else if (cause instanceof WebClientResponseException.Unauthorized ||
					cause instanceof WebClientResponseException.Forbidden) {

				WebClientResponseException ex = (WebClientResponseException) cause;
				String remaining = ex.getHeaders().getFirst("X-Rate-Limit-Remaining");

				String msg = "Contraseña incorrecta.";
				if (remaining != null) {
					msg += " Te quedan " + remaining + " intentos.";
				}
				// Esto muestra el mensaje limpio en rojo
				throw new BadCredentialsException(msg);
			}

			// 3. OTROS ERRORES
			// Mensaje genérico para no mostrar el stacktrace al usuario
			throw new AuthenticationServiceException("Error de conexión o credenciales.");
		}
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}
}