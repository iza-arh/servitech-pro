package com.iza.servitech_pro.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.iza.servitech_pro.data.local.entity.SyncQueueItem;

import java.util.List;

@Dao
public interface SyncQueueDao {
    @Insert
    long insert(SyncQueueItem item);

    @Query("SELECT * FROM sync_queue")
    List<SyncQueueItem> getAllItems();

    @Query("SELECT * FROM sync_queue WHERE estado = 'PENDIENTE' ORDER BY id")
    List<SyncQueueItem> getPendientes();

    @Query("SELECT COUNT(*) FROM sync_queue WHERE estado = 'PENDIENTE'")
    LiveData<Integer> observarCantidadPendientes();

    @Query("SELECT * FROM sync_queue WHERE estado = 'RECHAZADO' ORDER BY id DESC")
    LiveData<List<SyncQueueItem>> observarRechazados();

    // Un cambio nuevo reemplaza al anterior de la misma fila
    @Query("DELETE FROM sync_queue WHERE entidad = :entidad AND entidadId = :entidadId")
    void eliminarPorEntidad(String entidad, String entidadId);

    @Query("DELETE FROM sync_queue WHERE id = :id")
    void eliminar(long id);

    @Query("UPDATE sync_queue SET estado = 'RECHAZADO', ultimoError = :error, intentos = intentos + 1 WHERE id = :id")
    void marcarRechazado(long id, String error);

    @Query("UPDATE sync_queue SET intentos = intentos + 1, ultimoError = :error WHERE id = :id")
    void registrarIntentoFallido(long id, String error);
}
