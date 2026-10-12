package com.minimarket.gestion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntradaInventarioRequest {
    @NotNull
    private Long productoId;

    @NotNull @Positive
    private Integer cantidad;

    @NotNull
    private LocalDate fechaVencimiento;
}
