package com.minimarket.gestion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AperturaCajaRequest {

    @NotNull(message = "El monto inicial de apertura es obligatorio")
    @DecimalMin(value = "0.00", message = "El monto de apertura no puede ser negativo")
    private BigDecimal montoApertura;

    private String observaciones;
}
