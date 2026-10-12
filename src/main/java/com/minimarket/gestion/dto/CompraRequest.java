package com.minimarket.gestion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompraRequest {

    @NotNull(message = "El ID del proveedor es obligatorio")
    private Long proveedorId;

    @NotBlank(message = "El número de comprobante es obligatorio")
    private String numeroComprobante;

    @NotBlank(message = "El tipo de comprobante es obligatorio (ej. FACTURA, BOLETA, GUIA)")
    private String tipoComprobante;

    private String observaciones;

    @NotEmpty(message = "La compra debe contener al menos un producto")
    @Valid
    private List<ItemCompraRequest> items;
}
