package com.iza.servitech_pro.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.iza.servitech_pro.data.local.entity.RegistroEntity;

import java.util.List;
import com.iza.servitech_pro.data.local.model.RepuestoUsado;
@Dao
public interface RegistroDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(RegistroEntity r);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<RegistroEntity> lista);

    @Query("SELECT * FROM registros_orden WHERE id = :id")
    RegistroEntity getById(String id);

    @Query("SELECT * FROM registros_orden WHERE deletedAt IS NULL AND ordenId = :ordenId ORDER BY localTs, updatedAt")
    LiveData<List<RegistroEntity>> observarPorOrden(String ordenId);

    @Query("SELECT * FROM registros_orden WHERE deletedAt IS NULL AND ordenId = :ordenId AND tipo = 'FIRMA' LIMIT 1")
    RegistroEntity getFirmaActiva(String ordenId);

    @Query("SELECT * FROM registros_orden WHERE isSynced = 0")
    List<RegistroEntity> getPendientes();

    @Query("SELECT id FROM registros_orden WHERE isSynced = 0")
    List<String> idsPendientes();

    @Query("UPDATE registros_orden SET isSynced = 1 WHERE id = :id AND localTs = :localTs")
    int marcarSincronizado(String id, long localTs);

    @Query("SELECT MAX(updatedAt) FROM registros_orden")
    String maxUpdatedAt();

    @Query("SELECT r.id AS id, i.nombre AS nombre, r.cantidad AS cantidad "
            + "FROM registros_orden r "
            + "LEFT JOIN catalogo_items i ON i.id = r.catalogoItemId "
            + "WHERE r.ordenId = :ordenId AND r.tipo = 'REPUESTO' AND r.deletedAt IS NULL "
            + "ORDER BY r.localTs, r.updatedAt")
    LiveData<List<RepuestoUsado>> observarRepuestos(String ordenId);
}