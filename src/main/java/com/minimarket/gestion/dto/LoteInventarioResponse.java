package com.minimarket.gestion.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteInventarioResponse {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String sku;
    private Integer cantidad;
    private LocalDate fechaIngreso;
    private LocalDate fechaVencimiento;
}
