package ar.utn.da.dsi.frontend.config;

import ar.utn.da.dsi.frontend.providers.CustomAuthProvider;
import org.springframework.beans.factory.annotation.Autowired; // (AÑADIR)
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler; // (AÑADIR)

@EnableMethodSecurity(prePostEnabled = true)
@Configuration
public class SecurityConfig {

	@Autowired
	private AuthenticationSuccessHandler successHandler;

	@Bean
	public AuthenticationManager authManager(HttpSecurity http, CustomAuthProvider provider) throws Exception {
		return http.getSharedObject(AuthenticationManagerBuilder.class)
				.authenticationProvider(provider)
				.build();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth

						//RUTAS PÚBLICAS (Permitir a TODOS)
						.requestMatchers(
								"/", "/facts",                   // Páginas de visualización
								"/login", "/registro",          // Login y Registro
                "/login-success",								// Login Sucess
								"/hechos/nuevo", "/hechos/crear",      // Enviar Hecho (Anónimo)
								"/solicitudes/nueva", "/solicitudes/crear", // Enviar Solicitud (Anónimo)
								"/css/**", "/js/**", "/assets/**", // Recursos estáticos
								"/error/403",
								"/error"
						).permitAll()

						.requestMatchers("/admin/api/solicitudes/**").hasAnyRole("ADMIN", "CONTRIBUTOR")

						//RUTAS DE CONTRIBUYENTE (Requieren estar registrados)
						.requestMatchers(
								"/contributor/**",              // Panel de Contribuyente
								"/hechos/{id}/editar"       		// Formulario de Edición
						).hasAnyRole("ADMIN", "CONTRIBUTOR") // Spring añade "ROLE_" automáticamente => agregue admin para tener panel como contribuyente

						.requestMatchers("/profile")
						.hasAnyRole("ADMIN", "CONTRIBUTOR")

						//RUTAS DE ADMIN (Requieren ser ADMIN)
						.requestMatchers("/admin/**").hasRole("ADMIN")

						.requestMatchers("/actuator/**").permitAll()

						// OTRAS RUTAS
						.anyRequest().authenticated() // Bloquea todo lo demás
				)
				.formLogin(form -> form
						.loginPage("/login")
						.usernameParameter("username")
						.passwordParameter("password")
						.permitAll()
						.successHandler(successHandler) // Redirección por rol
				)
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/")
						.permitAll()
				)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) ->
								response.sendRedirect("/login?unauthorized")
						)
						.accessDeniedHandler((request, response, accessDeniedException) ->
								response.sendRedirect("/error/403")
						)
				);

		return http.build();
	}
}