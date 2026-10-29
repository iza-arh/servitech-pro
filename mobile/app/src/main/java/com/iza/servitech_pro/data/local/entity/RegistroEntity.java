package com.iza.servitech_pro.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "registros_orden", indices = {@Index("ordenId")})
public class RegistroEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    public String ordenId;
    public String tipo;            // BITACORA, REPUESTO, FOTO, FIRMA
    public String texto;
    public String catalogoItemId;
    public Integer cantidad;
    public String datos;           // imagen en base64 (comprimida, idealmente < 500 KB)

    public String updatedAt;
    public String deletedAt;

    public boolean isSynced;
    public long localTs;
}