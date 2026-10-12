package com.minimarket.gestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para el auto-registro de un nuevo cliente desde la tienda virtual")
public class RegistroClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    @Schema(example = "Juan")
    private String nombres;

    @Size(max = 120, message = "Los apellidos no pueden superar los 120 caracteres")
    @Schema(example = "Pérez Quispe")
    private String apellidos;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo no es válido")
    @Schema(example = "juan.perez@gmail.com")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @Schema(example = "123456")
    private String password;

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "DNI|RUC|CE", message = "El tipo de documento debe ser DNI, RUC o CE")
    @Schema(example = "DNI", allowableValues = {"DNI", "RUC", "CE"})
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(min = 8, max = 20, message = "El documento debe tener entre 8 y 20 caracteres")
    @Schema(example = "71234567")
    private String numeroDocumento;

    @Size(max = 25, message = "El teléfono no puede superar los 25 caracteres")
    @Schema(example = "987123456")
    private String telefono;

    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    @Schema(example = "Calle Los Pinos 123, Urb. Primavera")
    private String direccion;
}
