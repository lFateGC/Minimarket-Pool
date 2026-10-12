package com.minimarket.gestion.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopProductoVendidoResponse {
    private Long productoId;
    private String nombre;
    private String sku;
    private String categoriaNombre;
    private Integer unidadesVendidas;
    private BigDecimal totalRecaudado;
    private Double porcentajeVentas;
    private String sugerenciaEstrategica;
}
