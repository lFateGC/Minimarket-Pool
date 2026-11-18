package com.minimarket.gestion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtResponse {
    private String token;
    private String email;
    private String nombre;
    private String rol;

    public JwtResponse(String token, String nombre, String rol) {
        this.token = token;
        this.nombre = nombre;
        this.rol = rol;
    }
}
