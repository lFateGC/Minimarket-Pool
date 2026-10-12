package com.minimarket.gestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para registro o actualización de un cliente")
public class ClienteRequest {

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "DNI|RUC|CE", message = "El tipo de documento debe ser DNI, RUC o CE")
    @Schema(example = "DNI", allowableValues = {"DNI", "RUC", "CE"})
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(min = 8, max = 20, message = "El número de documento debe tener entre 8 y 20 caracteres")
    @Schema(example = "72345678")
    private String numeroDocumento;

    @NotBlank(message = "El nombre o razón social es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    @Schema(example = "Carlos")
    private String nombres;

    @Size(max = 120, message = "Los apellidos no pueden superar los 120 caracteres")
    @Schema(example = "Mendoza Soto")
    private String apellidos;

    @Schema(example = "carlos.mendoza@gmail.com")
    private String email;

    @Size(max = 25, message = "El teléfono no puede superar los 25 caracteres")
    @Schema(example = "987654321")
    private String telefono;

    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    @Schema(example = "Av. Los Sauces 450, Lima")
    private String direccion;
}
