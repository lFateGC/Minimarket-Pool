package com.minimarket.gestion.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompraResponse {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String productoSku;
    private Integer cantidad;
    private BigDecimal costoUnitario;
    private BigDecimal subtotal;
    private LocalDate fechaVencimiento;
    private Long loteId;
}
