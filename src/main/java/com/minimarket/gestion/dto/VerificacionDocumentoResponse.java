package com.minimarket.gestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resultado de la verificación de un documento de identidad para auto-registro o compra web")
public class VerificacionDocumentoResponse {

    @Schema(description = "Indica si el cliente ya existe registrado en la tienda física (por caja/vendedor)", example = "true")
    private boolean registradoEnTienda;

    @Schema(description = "Indica si el cliente ya cuenta con un usuario y contraseña web activos", example = "false")
    private boolean tieneCuentaWeb;

    @Schema(example = "DNI")
    private String tipoDocumento;

    @Schema(example = "71837969")
    private String numeroDocumento;

    @Schema(example = "Juan Carlos")
    private String nombres;

    @Schema(example = "Pérez Soto")
    private String apellidos;

    @Schema(example = "juan.perez@gmail.com")
    private String email;

    @Schema(example = "987654321")
    private String telefono;

    @Schema(example = "Av. Los Sauces 450, Lima")
    private String direccion;

    @Schema(description = "Mensaje amigable contextual para el usuario", example = "¡Hola Juan Carlos! Te encontramos en nuestra tienda...")
    private String mensaje;
}
