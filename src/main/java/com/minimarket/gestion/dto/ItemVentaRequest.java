package com.minimarket.gestion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemVentaRequest {
    @NotNull
    private Long productoId;

    @NotNull @Positive
    private Integer cantidad;

    private BigDecimal precioUnitario;

    public ItemVentaRequest(Long productoId, Integer cantidad) {
        this.productoId = productoId;
        this.cantidad = cantidad;
    }
}
