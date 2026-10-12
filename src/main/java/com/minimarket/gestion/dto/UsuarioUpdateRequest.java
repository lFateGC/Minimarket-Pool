package com.minimarket.gestion.dto;

import com.minimarket.gestion.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioUpdateRequest {

    @NotBlank(message = "El nombre no puede estar vacío")
    private String nombre;

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El formato de email no es válido")
    private String email;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;

    // Contraseña opcional: si viene null o vacía, se mantiene la contraseña actual
    private String password;
}
