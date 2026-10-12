package com.minimarket.gestion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MermaInventarioRequest {

    @NotNull(message = "El ID del lote es obligatorio")
    private Long loteId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @NotBlank(message = "El motivo de la merma es obligatorio (ej: Producto vencido, Empaque roto, etc.)")
    private String motivo;
}
