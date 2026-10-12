package com.minimarket.gestion.dto;

import com.minimarket.gestion.entity.MetodoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponse {
    private Long id;
    private String vendedor;
    private Long clienteId;
    private String clienteNombre;
    private String clienteDocumento;
    private LocalDateTime fecha;
    private BigDecimal total;
    private MetodoPago metodoPago;
    private Long turnoCajaId;
    private List<DetalleVentaResponse> detalles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleVentaResponse {
        private Long productoId;
        private String sku;
        private String productoNombre;
        private String imagenUrl;
        private String categoriaNombre;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
    }
}
