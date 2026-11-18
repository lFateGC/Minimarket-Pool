package com.minimarket.gestion.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.minimarket.gestion.entity.MetodoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaRequest {

    private Long clienteId;

    private MetodoPago metodoPago;

    private String tipoComprobante;

    private BigDecimal montoEfectivo;

    private BigDecimal vuelto;

    private String direccionEnvio;

    @NotEmpty(message = "La venta debe contener al menos un producto")
    @Valid
    @JsonAlias({"detalles", "items", "productos"})
    private List<ItemVentaRequest> items;
}
