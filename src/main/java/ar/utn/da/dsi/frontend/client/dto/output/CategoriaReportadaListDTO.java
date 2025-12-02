package ar.utn.da.dsi.frontend.client.dto.output;

import java.util.List;
import lombok.Data;

@Data
public class CategoriaReportadaListDTO {
  private List<CategoriaHechosData> distribucion; // Lista de distribución de hechos por categoría
}