package com.servitech.authservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
public class LoginResponse {
    private String token;
    private String rol;

    public LoginResponse(String token, String rol) {
        this.token = token;
        this.rol = rol;
    }
}
