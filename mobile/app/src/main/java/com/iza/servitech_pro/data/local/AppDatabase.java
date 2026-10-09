package com.iza.servitech_pro.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.iza.servitech_pro.data.local.dao.SyncQueueDao;
import com.iza.servitech_pro.data.local.entity.SyncQueueItem;

@Database(entities = {SyncQueueItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SyncQueueDao syncQueueDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        // Patrón singleton: una sola instancia de la BD para toda la app
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "servitech_pro.db").build();
                }
            }
        }
        return INSTANCE;
    }
}
