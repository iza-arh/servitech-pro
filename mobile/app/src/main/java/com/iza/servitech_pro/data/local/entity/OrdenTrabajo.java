package com.iza.servitech_pro.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ordenes_trabajo")
public class OrdenTrabajo {

    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String placa;

    @NonNull
    public String marca;

    @NonNull
    public String modelo;

    @NonNull
    public String descripcion;

    @NonNull
    public String estado;

    @NonNull
    public String uidCreador;

    public boolean pendienteSync;

    public boolean eliminado;

    public OrdenTrabajo(@NonNull String id, @NonNull String placa, @NonNull String marca, @NonNull String modelo, @NonNull String descripcion,
            @NonNull String estado, @NonNull String uidCreador, boolean pendienteSync, boolean eliminado) {
        this.id = id;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.descripcion = descripcion;
        this.estado = estado;
        this.uidCreador = uidCreador;
        this.pendienteSync = pendienteSync;
        this.eliminado = eliminado;
    }
}
