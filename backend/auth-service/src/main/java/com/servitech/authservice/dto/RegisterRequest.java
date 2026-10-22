package com.servitech.authservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
public class RegisterRequest {
    private String username;
    private String password;
}
