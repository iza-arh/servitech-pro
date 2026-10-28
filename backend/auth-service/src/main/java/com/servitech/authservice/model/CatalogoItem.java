package com.servitech.authservice.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalogo_items")
public class CatalogoItem {

    @Id
    @Column(length = 36)
    public String id; // UUID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TipoCatalogo tipo;

    @Column(nullable = false)
    public String nombre;

    public String descripcion;
    public String codigo; // solo repuestos (opcional)
    public BigDecimal precio; // solo repuestos (opcional)

    @Column(nullable = false)
    public Instant updatedAt; // lo asigna el servidor

    public Instant deletedAt; // borrado lógico
}