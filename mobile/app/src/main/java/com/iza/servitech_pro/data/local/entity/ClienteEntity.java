package com.iza.servitech_pro.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "clientes")
public class ClienteEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    public String nombre;
    public String telefono;
    public String correo;
    public String direccion;
    public Long usuarioId;

    public String updatedAt;  // lo asigna el servidor
    public String deletedAt;

    public boolean isSynced;  // false = tiene cambios sin enviar
    public long localTs;      // momento del último cambio local
}