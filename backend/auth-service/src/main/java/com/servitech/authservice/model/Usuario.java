package com.servitech.authservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // los usuarios viven solo en el servidor, no se sincronizan

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash; // hash bcrypt, nunca texto plano

    @Enumerated(EnumType.STRING)
    private Rol rol;

    // getters y setters
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío o ser nulo.");
        }
        if (username.trim().length() < 3 || username.trim().length() > 50) {
            throw new IllegalArgumentException("El nombre de usuario debe tener entre 3 y 50 caracteres.");
        }
        this.username = username.trim();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("El hash de la contraseña no puede estar vacío.");
        }
        // Validación de formato BCrypt ($2a$, $2b$, o $2y$ seguido de 56 caracteres)
        if (!passwordHash.matches("^\\$2[ayb]\\$\\d\\d\\$[A-Za-z0-9./]{53}$")) {
            throw new IllegalArgumentException("La contraseña debe ser un hash BCrypt válido.");
        }
        this.passwordHash = passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        if (rol == null) {
            throw new IllegalArgumentException("El rol no puede ser nulo.");
        }
        this.rol = rol;
    }
}