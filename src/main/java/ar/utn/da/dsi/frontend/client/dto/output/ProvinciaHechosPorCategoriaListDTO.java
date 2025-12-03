package ar.utn.da.dsi.frontend.client.dto.output;

import lombok.Data;

import java.util.List;
@Data
public class ProvinciaHechosPorCategoriaListDTO {
  private String categoria; // Categoría consultada
  private List<ProvinciaHechosData> distribucion; // Lista de distribución de hechos por provincia para esa categoría
}
