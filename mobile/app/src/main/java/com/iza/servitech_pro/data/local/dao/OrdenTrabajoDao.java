package com.iza.servitech_pro.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.iza.servitech_pro.data.local.entity.OrdenTrabajo;

import java.util.List;

@Dao
public interface OrdenTrabajoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(OrdenTrabajo orden);

    @Update
    void actualizar(OrdenTrabajo orden);

    @Query("SELECT * FROM ordenes_trabajo WHERE id = :id LIMIT 1")
    OrdenTrabajo obtenerPorId(String id);

    @Query("SELECT * FROM ordenes_trabajo WHERE eliminado = 0 ORDER BY placa ASC")
    List<OrdenTrabajo> obtenerTodas();

    @Query("SELECT * FROM ordenes_trabajo WHERE pendienteSync = 1")
    List<OrdenTrabajo> obtenerPendientes();

    @Query("SELECT * FROM ordenes_trabajo WHERE uidCreador = :uid AND eliminado = 0")
    List<OrdenTrabajo> obtenerPorCreador(String uid);

    @Query("DELETE FROM ordenes_trabajo WHERE id = :id")
    void eliminarFisicamente(String id);
}
