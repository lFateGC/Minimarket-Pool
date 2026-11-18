package com.minimarket.gestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("cajaAbierta")
    public boolean isCajaAbierta() {
        return EstadoCaja.ABIERTA.equals(this.estado);
    }

    @JsonProperty("sesionId")
    public Long getSesionId() {
        return this.id;
    }

    @JsonProperty("saldoCalculado")
    public BigDecimal getSaldoCalculado() {
        if (this.montoCierreEsperado != null) {
            return this.montoCierreEsperado;
        }
        BigDecimal apertura = this.montoApertura != null ? this.montoApertura : BigDecimal.ZERO;
        BigDecimal efectivo = this.totalVentasEfectivo != null ? this.totalVentasEfectivo : BigDecimal.ZERO;
        return apertura.add(efectivo);
    }
}
