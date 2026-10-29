package com.iza.servitech_pro.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_queue", indices = {@Index(value = {"entidad", "entidadId"})})
public class SyncQueueItem {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String entidad;      // CLIENTE, CATALOGO, ORDEN, REGISTRO (igual que el servidor)
    public String entidadId;    // UUID del registro
    public String operacion;    // UPSERT, DELETE
    public String estado;       // PENDIENTE, RECHAZADO
    public int intentos;
    public String ultimoError;
    public long createdAt;
}
