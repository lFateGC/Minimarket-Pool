package com.minimarket.gestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta detallada con información de cliente")
public class ClienteResponse {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "DNI")
    private String tipoDocumento;

    @Schema(example = "72345678")
    private String numeroDocumento;

    @Schema(example = "Carlos")
    private String nombres;

    @Schema(example = "Mendoza Soto")
    private String apellidos;

    @Schema(example = "Carlos Mendoza Soto")
    private String nombreCompleto;

    @Schema(example = "carlos.mendoza@gmail.com")
    private String email;

    @Schema(example = "987654321")
    private String telefono;

    @Schema(example = "Av. Los Sauces 450, Lima")
    private String direccion;

    @Schema(example = "true")
    private Boolean activo;

    private LocalDateTime fechaRegistro;

    @Schema(example = "5")
    private Long totalCompras;
}
