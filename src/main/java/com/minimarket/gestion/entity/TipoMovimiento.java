package com.minimarket.gestion.entity;

public enum TipoMovimiento {
    ENTRADA, // ingreso de un lote nuevo (compra a proveedor)
    SALIDA,  // descuento por venta
    MERMA    // producto vencido o dañado que se da de baja manualmente
}
