package com.minimarket.gestion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponse {
    private Long id;
    private String nombre;
    private String sku;
    private String imagenUrl;
    private BigDecimal precioUnitario;
    private BigDecimal precioCaja;
    private Integer unidadesPorCaja;
    private Integer stockMinimo;
    private Integer stockActual;
    private Long categoriaId;
    private String categoriaNombre;
    private Long proveedorId;
    private String proveedorNombre;
}
