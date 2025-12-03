package ar.utn.da.dsi.frontend.client.dto.output;

import lombok.Data;

import java.util.List;

@Data
public class ProvinciaHechosPorColeccionListDTO {
  private String handleID; // ID de la colección consultada
  private List<ProvinciaHechosData> distribucion; // Lista de distribución de hechos por provincia
}