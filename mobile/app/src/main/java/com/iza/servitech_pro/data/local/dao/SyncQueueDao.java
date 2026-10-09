package com.iza.servitech_pro.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.iza.servitech_pro.data.local.entity.SyncQueueItem;

import java.util.List;

@Dao
public interface SyncQueueDao {
    @Insert
    long insert(SyncQueueItem item);

    @Query("SELECT * FROM sync_queue WHERE isSynced = 0")
    List<SyncQueueItem> getPendientes();

    @Query("SELECT * FROM sync_queue")
    List<SyncQueueItem> getAllItems();

    @Update
    void update(SyncQueueItem item);
}
