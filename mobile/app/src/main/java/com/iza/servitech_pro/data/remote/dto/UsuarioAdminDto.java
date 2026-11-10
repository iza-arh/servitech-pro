package com.iza.servitech_pro.data.remote.dto;

public class UsuarioAdminDto {

    public static class Usuario {
        public Long id;
        public String username;
        public String nombre;
        public String rol;
    }

    public static class Crear {
        public String username;
        public String password;
        public String nombre;
        public String rol;
    }

    public static class Password {
        public String password;
    }
}