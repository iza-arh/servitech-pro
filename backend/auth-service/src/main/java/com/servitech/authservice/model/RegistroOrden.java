package com.servitech.authservice.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "registros_orden")
public class RegistroOrden {

    @Id
    @Column(length = 36)
    public String id; // UUID generado en el dispositivo

    @Column(nullable = false, length = 36)
    public String ordenId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TipoRegistro tipo;

    @Column(length = 2000)
    public String texto; // BITACORA: avance. FOTO: descripción (opcional)

    @Column(length = 36)
    public String catalogoItemId; // REPUESTO: id del repuesto del catálogo

    public Integer cantidad; // REPUESTO

    @Column(columnDefinition = "LONGTEXT")
    public String datos; // FOTO / FIRMA: imagen en base64

    @Column(nullable = false)
    public Instant updatedAt; // lo asigna el servidor

    public Instant deletedAt; // borrado lógico
}