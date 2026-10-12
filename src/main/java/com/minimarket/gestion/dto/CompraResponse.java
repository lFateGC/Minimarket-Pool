package com.minimarket.gestion.dto;

import com.minimarket.gestion.entity.EstadoCompra;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompraResponse {
    private Long id;
    private String numeroComprobante;
    private String tipoComprobante;
    private Long proveedorId;
    private String proveedorRazonSocial;
    private String proveedorRuc;
    private Long usuarioId;
    private String usuarioNombre;
    private LocalDateTime fecha;
    private BigDecimal total;
    private EstadoCompra estado;
    private String observaciones;
    private List<DetalleCompraResponse> detalles;
}
