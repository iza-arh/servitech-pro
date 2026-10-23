package com.iza.servitech_pro.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.iza.servitech_pro.data.local.entity.UsuarioLocal;

import java.util.List;

@Dao
public interface UsuarioLocalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(UsuarioLocal usuario);

    @Update
    void actualizar(UsuarioLocal usuario);

    @Query("SELECT * FROM usuarios_local WHERE uid = :uid LIMIT 1")
    UsuarioLocal obtenerPorUid(String uid);

    @Query("SELECT * FROM usuarios_local WHERE pendienteSync = 1")
    List<UsuarioLocal> obtenerPendientes();

    @Query("DELETE FROM usuarios_local WHERE uid = :uid")
    void eliminarPorUid(String uid);
}