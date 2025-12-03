package ar.utn.da.dsi.frontend.client.dto.output;

import lombok.Data;

import java.util.List;

@Data
public class HoraHechosPorCategoriaListDTO {
  private String categoria; // Categoría consultada
  private List<HoraHechosData> distribucion; // Distribución de conteos para cada hora del día
}