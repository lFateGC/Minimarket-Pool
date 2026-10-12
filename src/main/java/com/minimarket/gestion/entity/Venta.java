package com.minimarket.gestion.entity;

import jakarta.persistence.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario; // quien registro la venta

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente; // cliente que realiza la compra

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private BigDecimal total;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 25)
    private MetodoPago metodoPago = MetodoPago.EFECTIVO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_caja_id")
    private TurnoCaja turnoCaja;

    @Builder.Default
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    public Venta() {
        this.detalles = new ArrayList<>();
        this.metodoPago = MetodoPago.EFECTIVO;
    }

    public Venta(Long id, Usuario usuario, Cliente cliente, LocalDateTime fecha, BigDecimal total,
                 MetodoPago metodoPago, TurnoCaja turnoCaja, List<DetalleVenta> detalles) {
        this.id = id;
        this.usuario = usuario;
        this.cliente = cliente;
        this.fecha = fecha != null ? fecha : LocalDateTime.now();
        this.total = total != null ? total : BigDecimal.ZERO;
        this.metodoPago = metodoPago != null ? metodoPago : MetodoPago.EFECTIVO;
        this.turnoCaja = turnoCaja;
        this.detalles = detalles != null ? detalles : new ArrayList<>();
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

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public TurnoCaja getTurnoCaja() {
        return turnoCaja;
    }

    public void setTurnoCaja(TurnoCaja turnoCaja) {
        this.turnoCaja = turnoCaja;
    }
}
