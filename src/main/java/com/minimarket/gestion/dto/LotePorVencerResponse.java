package com.minimarket.gestion.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotePorVencerResponse {
    private Long loteId;
    private Long productoId;
    private String productoNombre;
    private String sku;
    private Integer cantidad;
    private LocalDate fechaIngreso;
    private LocalDate fechaVencimiento;
    private long diasParaVencer;
    private String estadoVencimiento; // "VENCIDO", "ALERTA CRÍTICA (<= 3 días)", "PRÓXIMO A VENCER"
}
