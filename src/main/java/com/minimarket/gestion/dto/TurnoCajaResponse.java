package com.minimarket.gestion.dto;

import com.minimarket.gestion.entity.EstadoCaja;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoCajaResponse {
    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal montoApertura;
    private BigDecimal totalVentasEfectivo;
    private BigDecimal totalVentasDigital;
    private BigDecimal totalVentasTarjeta;
    private BigDecimal totalVendido;
    private BigDecimal montoCierreEsperado;
    private BigDecimal montoCierreReal;
    private BigDecimal diferencia;
    private EstadoCaja estado;
    private String observaciones;
    private Long cantidadVentas;
}
