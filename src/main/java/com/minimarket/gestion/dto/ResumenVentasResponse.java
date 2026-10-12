package com.minimarket.gestion.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenVentasResponse {
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private long totalVentas;
    private BigDecimal totalRecaudado;
    private BigDecimal ticketPromedio;
}
