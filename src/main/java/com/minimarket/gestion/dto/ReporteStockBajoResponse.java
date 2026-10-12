package com.minimarket.gestion.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteStockBajoResponse {
    private Long productoId;
    private String nombre;
    private String sku;
    private Integer stockActual;
    private Integer stockMinimo;
    private Integer deficit;
    private String categoriaNombre;
    private String proveedorNombre;
}
