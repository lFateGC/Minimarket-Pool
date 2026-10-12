package com.minimarket.gestion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemVentaRequest {
    @NotNull
    private Long productoId;

    @NotNull @Positive
    private Integer cantidad;
}
