package ar.utn.da.dsi.frontend.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HechoDTO {
	@JsonProperty("id")
	private Long id;

	@JsonProperty("userId")
	private Long userId;

	@JsonProperty("collectionHandle")
	private String collectionHandle;

	@JsonProperty("titulo")
	private String titulo;

	@JsonProperty("descripcion")
	private String descripcion;

	@JsonProperty("categoria")
	private String categoria;

	@JsonProperty("fechaAcontecimiento")
	private LocalDateTime fechaAcontecimiento;

	@JsonProperty("fechaCarga")
	private LocalDate fechaCarga;

	@JsonProperty("latitud")
	private Double latitud;

	@JsonProperty("longitud")
	private Double longitud;

	@JsonProperty("contenidoMultimedia")
	private String contenidoMultimedia;

	@JsonProperty("consensuado")
	private boolean consensuado;

	@JsonProperty("estado")
	private String estado;

	@JsonProperty("sugerenciaAdmin")
	private String sugerenciaAdmin;

	@JsonProperty("nombreOrigen")
	private String nombreOrigen;

	@JsonProperty("tieneEdicionPendiente")
	private boolean tieneEdicionPendiente;
}