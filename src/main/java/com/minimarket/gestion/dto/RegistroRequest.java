package com.minimarket.gestion.dto;

import com.minimarket.gestion.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroRequest {
    @NotBlank
    private String nombre;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private Rol rol; // Se ignora en el registro público; solo lo administra un administrador.
}
