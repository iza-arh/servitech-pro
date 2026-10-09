package com.iza.servitech_pro.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_queue")
public class SyncQueueItem {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String entityName;   // ej. "orden_trabajo"
    public long entityId;
    public String operation;    // CREATE, UPDATE, DELETE
    public String payloadJson;
    public long createdAt;
    public boolean isSynced;
}
