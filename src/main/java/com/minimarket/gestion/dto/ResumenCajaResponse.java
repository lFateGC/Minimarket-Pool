package com.minimarket.gestion.dto;

import com.minimarket.gestion.entity.EstadoCaja;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenCajaResponse {
    private Long turnoId;
    private EstadoCaja estado;
    private Long usuarioId;
    private String usuarioNombre;
    private LocalDateTime fechaApertura;
    private BigDecimal montoApertura;
    private BigDecimal ventasEfectivo;
    private BigDecimal ventasYape;
    private BigDecimal ventasPlin;
    private BigDecimal ventasTarjetaDebito;
    private BigDecimal ventasTarjetaCredito;
    private BigDecimal ventasTransferencia;
    private BigDecimal totalVendido;
    private BigDecimal efectivoEsperadoEnCaja;
    private Long cantidadVentas;
}
