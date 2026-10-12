package com.minimarket.gestion.dto;

import com.minimarket.gestion.entity.TipoMovimiento;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventarioResponse {
    private Long id;
    private Long loteId;
    private Long productoId;
    private String productoNombre;
    private TipoMovimiento tipo;
    private Integer cantidad;
    private LocalDateTime fecha;
    private String motivo;
}
