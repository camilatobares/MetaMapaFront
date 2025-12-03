package ar.utn.da.dsi.frontend.client.dto.output;


import lombok.Data;

@Data
// DTO para cada par {Provincia, Cantidad}
public class ProvinciaHechosData {
  private String provincia;
  private Long cantidadHechos;
}