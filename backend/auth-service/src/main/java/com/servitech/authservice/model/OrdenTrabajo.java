package com.servitech.authservice.model;

import java.time.Instant;
import java.time.Year;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ordenes_trabajo")
public class OrdenTrabajo {

    @Id
    @Column(length = 36)
    private String id; // UUID generado en el dispositivo

    @Column(nullable = false, length = 36)
    private String clienteId;

    private Long tecnicoId; // null = sin asignar

    @Column(length = 36)
    private String tipoServicioId; // CatalogoItem TIPO_SERVICIO (opcional)

    @Column(length = 36)
    private String categoriaEquipoId; // CatalogoItem CATEGORIA_EQUIPO (opcional)

    // Datos del vehículo, dentro de la orden
    @Column(nullable = false)
    private String placa;
    private String marca;
    private String modelo;
    private Integer anio;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrden estado;

    @Column(nullable = false)
    private Instant updatedAt; // lo asigna el servidor

    private Instant deletedAt; // borrado lógico

    // getters y setters de todos los campos
    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID no puede estar vacío.");
        }
        if (id.trim().length() > 36) {
            throw new IllegalArgumentException("El ID no puede exceder los 36 caracteres.");
        }
        this.id = id.trim();
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        if (clienteId == null || clienteId.trim().isEmpty()) {
            throw new IllegalArgumentException("El clienteId es obligatorio.");
        }
        if (clienteId.trim().length() > 36) {
            throw new IllegalArgumentException("El clienteId no puede exceder los 36 caracteres.");
        }
        this.clienteId = clienteId.trim();
    }

    public Long getTecnicoId() {
        return tecnicoId;
    }

    public void setTecnicoId(Long tecnicoId) {
        if (tecnicoId != null && tecnicoId <= 0) {
            throw new IllegalArgumentException("El ID del técnico debe ser mayor a 0.");
        }
        this.tecnicoId = tecnicoId;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        if (placa == null || placa.trim().isEmpty()) {
            throw new IllegalArgumentException("La placa es obligatoria.");
        }
        this.placa = placa.trim().toUpperCase();
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = (marca != null) ? marca.trim() : null;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = (modelo != null) ? modelo.trim() : null;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        if (anio != null) {
            int maxAnioPermitido = Year.now().getValue() + 1;
            if (anio < 1900 || anio > maxAnioPermitido) {
                throw new IllegalArgumentException("El año debe estar entre 1900 y " + maxAnioPermitido + ".");
            }
        }
        this.anio = anio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción es obligatoria.");
        }
        if (descripcion.length() > 1000) {
            throw new IllegalArgumentException("La descripción no puede exceder los 1000 caracteres.");
        }
        this.descripcion = descripcion.trim();
    }

    public EstadoOrden getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrden estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo.");
        }
        this.estado = estado;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("La fecha updatedAt no puede ser nula.");
        }
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getTipoServicioId() {
        return tipoServicioId;
    }

    public void setTipoServicioId(String tipoServicioId) {
        this.tipoServicioId = tipoServicioId;
    }

    public String getCategoriaEquipoId() {
        return categoriaEquipoId;
    }

    public void setCategoriaEquipoId(String categoriaEquipoId) {
        this.categoriaEquipoId = categoriaEquipoId;
    }
}