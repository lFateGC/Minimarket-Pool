package com.minimarket.gestion.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias({"montoFinalReal", "monto_cierre_real", "monto_final_real"})
    private BigDecimal montoCierreReal;

    private String observaciones;
}
