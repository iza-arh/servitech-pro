package com.iza.servitech_pro.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "catalogo_items", indices = {@Index("tipo")})
public class CatalogoEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    public String tipo;        // TIPO_SERVICIO, CATEGORIA_EQUIPO, REPUESTO
    public String nombre;
    public String descripcion;
    public String codigo;
    public Double precio;

    public String updatedAt;
    public String deletedAt;

    public boolean isSynced;
    public long localTs;
}
