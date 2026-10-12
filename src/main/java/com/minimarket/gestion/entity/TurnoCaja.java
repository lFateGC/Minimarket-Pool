package com.minimarket.gestion.entity;

import jakarta.persistence.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "turnos_caja")
@Builder
public class TurnoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    private LocalDateTime fechaCierre;

    @Column(nullable = false)
    private BigDecimal montoApertura;

    private BigDecimal totalVentasEfectivo;

    private BigDecimal totalVentasDigital;

    private BigDecimal totalVentasTarjeta;

    private BigDecimal totalVendido;

    private BigDecimal montoCierreEsperado;

    private BigDecimal montoCierreReal;

    private BigDecimal diferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCaja estado;

    private String observaciones;

    public TurnoCaja() {
        this.estado = EstadoCaja.ABIERTA;
        this.fechaApertura = LocalDateTime.now();
        this.montoApertura = BigDecimal.ZERO;
        this.totalVentasEfectivo = BigDecimal.ZERO;
        this.totalVentasDigital = BigDecimal.ZERO;
        this.totalVentasTarjeta = BigDecimal.ZERO;
        this.totalVendido = BigDecimal.ZERO;
    }

    public TurnoCaja(Long id, Usuario usuario, LocalDateTime fechaApertura, LocalDateTime fechaCierre,
                     BigDecimal montoApertura, BigDecimal totalVentasEfectivo, BigDecimal totalVentasDigital,
                     BigDecimal totalVentasTarjeta, BigDecimal totalVendido, BigDecimal montoCierreEsperado,
                     BigDecimal montoCierreReal, BigDecimal diferencia, EstadoCaja estado, String observaciones) {
        this.id = id;
        this.usuario = usuario;
        this.fechaApertura = fechaApertura != null ? fechaApertura : LocalDateTime.now();
        this.fechaCierre = fechaCierre;
        this.montoApertura = montoApertura != null ? montoApertura : BigDecimal.ZERO;
        this.totalVentasEfectivo = totalVentasEfectivo != null ? totalVentasEfectivo : BigDecimal.ZERO;
        this.totalVentasDigital = totalVentasDigital != null ? totalVentasDigital : BigDecimal.ZERO;
        this.totalVentasTarjeta = totalVentasTarjeta != null ? totalVentasTarjeta : BigDecimal.ZERO;
        this.totalVendido = totalVendido != null ? totalVendido : BigDecimal.ZERO;
        this.montoCierreEsperado = montoCierreEsperado;
        this.montoCierreReal = montoCierreReal;
        this.diferencia = diferencia;
        this.estado = estado != null ? estado : EstadoCaja.ABIERTA;
        this.observaciones = observaciones;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public BigDecimal getMontoApertura() {
        return montoApertura;
    }

    public void setMontoApertura(BigDecimal montoApertura) {
        this.montoApertura = montoApertura;
    }

    public BigDecimal getTotalVentasEfectivo() {
        return totalVentasEfectivo;
    }

    public void setTotalVentasEfectivo(BigDecimal totalVentasEfectivo) {
        this.totalVentasEfectivo = totalVentasEfectivo;
    }

    public BigDecimal getTotalVentasDigital() {
        return totalVentasDigital;
    }

    public void setTotalVentasDigital(BigDecimal totalVentasDigital) {
        this.totalVentasDigital = totalVentasDigital;
    }

    public BigDecimal getTotalVentasTarjeta() {
        return totalVentasTarjeta;
    }

    public void setTotalVentasTarjeta(BigDecimal totalVentasTarjeta) {
        this.totalVentasTarjeta = totalVentasTarjeta;
    }

    public BigDecimal getTotalVendido() {
        return totalVendido;
    }

    public void setTotalVendido(BigDecimal totalVendido) {
        this.totalVendido = totalVendido;
    }

    public BigDecimal getMontoCierreEsperado() {
        return montoCierreEsperado;
    }

    public void setMontoCierreEsperado(BigDecimal montoCierreEsperado) {
        this.montoCierreEsperado = montoCierreEsperado;
    }

    public BigDecimal getMontoCierreReal() {
        return montoCierreReal;
    }

    public void setMontoCierreReal(BigDecimal montoCierreReal) {
        this.montoCierreReal = montoCierreReal;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(BigDecimal diferencia) {
        this.diferencia = diferencia;
    }

    public EstadoCaja getEstado() {
        return estado;
    }

    public void setEstado(EstadoCaja estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
