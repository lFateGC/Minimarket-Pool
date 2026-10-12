package com.minimarket.gestion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CierreCajaRequest {

    @NotNull(message = "El monto de cierre real (conteo físico en efectivo) es obligatorio")
    @DecimalMin(value = "0.00", message = "El monto de cierre no puede ser negativo")
    private BigDecimal montoCierreReal;

    private String observaciones;
}
