package com.minimarket.gestion.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "productos")
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String sku;

    private String imagenUrl;

    @Column(nullable = false)
    private BigDecimal precioUnitario;

    private BigDecimal precioCaja;

    private Integer unidadesPorCaja;

    @Column(nullable = false)
    private Integer stockMinimo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaProducto categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Builder.Default
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<LoteInventario> lotes = new ArrayList<>();

    public Producto() {
        this.lotes = new ArrayList<>();
        this.stockMinimo = 5;
        this.unidadesPorCaja = 1;
    }

    public Producto(Long id, String nombre, String sku, String imagenUrl, BigDecimal precioUnitario,
                    BigDecimal precioCaja, Integer unidadesPorCaja, Integer stockMinimo,
                    CategoriaProducto categoria, Proveedor proveedor, List<LoteInventario> lotes) {
        this.id = id;
        this.nombre = nombre;
        this.sku = sku;
        this.imagenUrl = imagenUrl;
        this.precioUnitario = precioUnitario;
        this.precioCaja = precioCaja;
        this.unidadesPorCaja = unidadesPorCaja != null ? unidadesPorCaja : 1;
        this.stockMinimo = stockMinimo != null ? stockMinimo : 5;
        this.categoria = categoria;
        this.proveedor = proveedor;
        this.lotes = lotes != null ? lotes : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getPrecioCaja() {
        return precioCaja;
    }

    public void setPrecioCaja(BigDecimal precioCaja) {
        this.precioCaja = precioCaja;
    }

    public Integer getUnidadesPorCaja() {
        return unidadesPorCaja;
    }

    public void setUnidadesPorCaja(Integer unidadesPorCaja) {
        this.unidadesPorCaja = unidadesPorCaja;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public CategoriaProducto getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaProducto categoria) {
        this.categoria = categoria;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public List<LoteInventario> getLotes() {
        return lotes;
    }

    public void setLotes(List<LoteInventario> lotes) {
        this.lotes = lotes;
    }
}
