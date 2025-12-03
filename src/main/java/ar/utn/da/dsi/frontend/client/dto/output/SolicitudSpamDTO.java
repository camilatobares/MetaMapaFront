package ar.utn.da.dsi.frontend.client.dto.output;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolicitudSpamDTO {

  // El backend envía "cantidadSpam", debemos capturarlo tal cual
  @JsonProperty("cantidadSpam")
  private Long cantidadSpam;

  // El backend envía "totalSolicitudes", corregimos el mapeo
  @JsonProperty("totalSolicitudes")
  private Long totalSolicitudes;

  /**
   * Este método calcula el porcentaje automáticamente.
   * Al usar Thymeleaf u otro motor de plantillas, puedes acceder a él como ${spamRatio.porcentaje}
   */
  public Double getPorcentaje() {
    if (totalSolicitudes == null || totalSolicitudes == 0) {
      return 0.0;
    }
    // Calculamos el porcentaje: (Spam / Total) * 100
    return (double) cantidadSpam / totalSolicitudes * 100;
  }
}