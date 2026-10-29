package com.iza.servitech_pro.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.iza.servitech_pro.data.local.entity.CatalogoEntity;

import java.util.List;

@Dao
public interface CatalogoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CatalogoEntity c);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<CatalogoEntity> lista);

    @Query("SELECT * FROM catalogo_items WHERE id = :id")
    CatalogoEntity getById(String id);

    @Query("SELECT * FROM catalogo_items WHERE deletedAt IS NULL AND tipo = :tipo ORDER BY nombre COLLATE NOCASE")
    LiveData<List<CatalogoEntity>> observarPorTipo(String tipo);

    // Versión sin LiveData, útil para validar duplicados
    @Query("SELECT * FROM catalogo_items WHERE deletedAt IS NULL AND tipo = :tipo")
    List<CatalogoEntity> getActivosPorTipo(String tipo);

    @Query("SELECT * FROM catalogo_items WHERE isSynced = 0")
    List<CatalogoEntity> getPendientes();

    @Query("SELECT id FROM catalogo_items WHERE isSynced = 0")
    List<String> idsPendientes();

    @Query("UPDATE catalogo_items SET isSynced = 1 WHERE id = :id AND localTs = :localTs")
    int marcarSincronizado(String id, long localTs);

    @Query("SELECT MAX(updatedAt) FROM catalogo_items")
    String maxUpdatedAt();
}