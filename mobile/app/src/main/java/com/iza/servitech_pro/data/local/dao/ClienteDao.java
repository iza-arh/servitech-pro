package com.iza.servitech_pro.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.iza.servitech_pro.data.local.entity.ClienteEntity;

import java.util.List;

@Dao
public interface ClienteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ClienteEntity c);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<ClienteEntity> lista);

    @Query("SELECT * FROM clientes WHERE id = :id")
    ClienteEntity getById(String id);

    @Query("SELECT * FROM clientes WHERE deletedAt IS NULL ORDER BY nombre COLLATE NOCASE")
    LiveData<List<ClienteEntity>> observarActivos();

    @Query("SELECT * FROM clientes WHERE isSynced = 0")
    List<ClienteEntity> getPendientes();

    @Query("SELECT id FROM clientes WHERE isSynced = 0")
    List<String> idsPendientes();

    @Query("UPDATE clientes SET isSynced = 1 WHERE id = :id AND localTs = :localTs")
    int marcarSincronizado(String id, long localTs);

    @Query("SELECT MAX(updatedAt) FROM clientes")
    String maxUpdatedAt();
}