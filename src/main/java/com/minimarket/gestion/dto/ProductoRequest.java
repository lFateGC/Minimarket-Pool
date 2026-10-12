package com.minimarket.gestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

// Lo que el frontend manda para crear/editar un producto.
// imagenUrl llega ya lista porque el frontend subio la imagen directo a Supabase Storage.
@Data
public class ProductoRequest {
    @NotBlank
    private String nombre;

    @NotBlank
    private String sku;

    private String imagenUrl;

    @NotNull
    @Positive
    private BigDecimal precioUnitario;

    @Positive
    private BigDecimal precioCaja;

    @Positive
    private Integer unidadesPorCaja;

    @NotNull
    @PositiveOrZero
    private Integer stockMinimo;

    @NotNull
    private Long categoriaId;

    @NotNull
    private Long proveedorId;
}
