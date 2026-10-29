package com.iza.servitech_pro.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.iza.servitech_pro.data.local.entity.OrdenEntity;

import java.util.List;

@Dao
public interface OrdenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(OrdenEntity o);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<OrdenEntity> lista);

    @Query("SELECT * FROM ordenes WHERE id = :id")
    OrdenEntity getById(String id);

    @Query("SELECT * FROM ordenes WHERE id = :id")
    LiveData<OrdenEntity> observarPorId(String id);

    @Query("SELECT * FROM ordenes WHERE deletedAt IS NULL ORDER BY localTs DESC, updatedAt DESC")
    LiveData<List<OrdenEntity>> observarActivas();

    @Query("SELECT * FROM ordenes WHERE deletedAt IS NULL AND estado = :estado ORDER BY localTs DESC, updatedAt DESC")
    LiveData<List<OrdenEntity>> observarPorEstado(String estado);

    @Query("SELECT * FROM ordenes WHERE deletedAt IS NULL AND (placa LIKE '%' || :q || '%' OR descripcion LIKE '%' || :q || '%') ORDER BY localTs DESC, updatedAt DESC")
    LiveData<List<OrdenEntity>> observarBuscar(String q);

    // Para las tarjetas del dashboard
    @Query("SELECT COUNT(*) FROM ordenes WHERE deletedAt IS NULL AND estado = :estado")
    LiveData<Integer> contarPorEstado(String estado);

    @Query("SELECT * FROM ordenes WHERE isSynced = 0")
    List<OrdenEntity> getPendientes();

    @Query("SELECT id FROM ordenes WHERE isSynced = 0")
    List<String> idsPendientes();

    @Query("UPDATE ordenes SET isSynced = 1 WHERE id = :id AND localTs = :localTs")
    int marcarSincronizado(String id, long localTs);

    @Query("SELECT MAX(updatedAt) FROM ordenes")
    String maxUpdatedAt();
}