package com.minimarket.gestion.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardKpiResponse {
    private long totalProductosRegistrados;
    private long totalCategorias;
    private long totalProveedores;
    private long totalProductosConStockBajo;
    private long totalLotesPorVencer;
    private long totalVentasRegistradas;
    private BigDecimal ingresosTotales;
}
