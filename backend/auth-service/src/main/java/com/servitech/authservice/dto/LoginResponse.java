package com.servitech.authservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
public class LoginResponse {
    private String token;
    private String nombre;
    private String rol;

    public LoginResponse(String token, String nombre, String rol) {
        this.token = token;
        this.nombre = nombre;
        this.rol = rol;
    }
}
