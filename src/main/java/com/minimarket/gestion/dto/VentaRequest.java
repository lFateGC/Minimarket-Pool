package com.minimarket.gestion.dto;

import com.minimarket.gestion.entity.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaRequest {

    private Long clienteId;

    private MetodoPago metodoPago;

    @NotEmpty(message = "La venta debe contener al menos un producto")
    @Valid
    private List<ItemVentaRequest> items;
}
