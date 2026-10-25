package com.servitech.authservice.model;

import java.time.Instant;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes")
public class Cliente {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$");

    private static final Pattern TELEFONO_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-()]{7,20}$");

    // --- Atributos ---

    @Id
    @Column(length = 36)
    private String id; // UUID

    @Column(nullable = false)
    private String nombre;

    private String telefono;
    private String correo;
    private String direccion;

    @Column(unique = true)
    private Long usuarioId; // cuenta de la app de este cliente

    @Column(nullable = false)
    private Instant updatedAt; // lo asigna el servidor

    private Instant deletedAt;

    // --- Getters y Setters con Validaciones ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id != null && id.trim().length() > 36) {
            throw new IllegalArgumentException("El ID no puede exceder los 36 caracteres.");
        }
        this.id = id != null ? id.trim() : null;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio y no puede estar vacío.");
        }
        this.nombre = nombre.trim();
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        if (telefono != null && !telefono.trim().isEmpty()) {
            String temp = telefono.trim();
            if (!TELEFONO_PATTERN.matcher(temp).matches()) {
                throw new IllegalArgumentException("El formato del teléfono no es válido.");
            }
            this.telefono = temp;
        } else {
            this.telefono = null;
        }
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        if (correo != null && !correo.trim().isEmpty()) {
            String temp = correo.trim().toLowerCase();
            if (!EMAIL_PATTERN.matcher(temp).matches()) {
                throw new IllegalArgumentException("El formato del correo electrónico no es válido.");
            }
            this.correo = temp;
        } else {
            this.correo = null;
        }
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = (direccion != null) ? direccion.trim() : null;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        if (usuarioId != null && usuarioId <= 0) {
            throw new IllegalArgumentException("El usuarioId debe ser un número entero positivo.");
        }
        this.usuarioId = usuarioId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("El campo updatedAt no puede ser nulo.");
        }
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        if (deletedAt != null && updatedAt != null && deletedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException("La fecha de eliminación no puede ser anterior a la de actualización.");
        }
        this.deletedAt = deletedAt;
    }
}