package com.iza.servitech_pro.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "ordenes", indices = {@Index("estado"), @Index("clienteId")})
public class OrdenEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    public String clienteId;
    public Long tecnicoId;
    public String tipoServicioId;
    public String categoriaEquipoId;
    public String placa;
    public String marca;
    public String modelo;
    public Integer anio;
    public String descripcion;
    public String estado;      // PENDIENTE, EN_PROCESO, COMPLETADA

    public String updatedAt;
    public String deletedAt;

    public boolean isSynced;
    public long localTs;
}