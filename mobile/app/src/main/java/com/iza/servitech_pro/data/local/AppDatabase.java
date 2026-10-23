package com.iza.servitech_pro.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.iza.servitech_pro.data.local.dao.OrdenTrabajoDao;
import com.iza.servitech_pro.data.local.dao.SyncQueueDao;
import com.iza.servitech_pro.data.local.dao.UsuarioLocalDao;
import com.iza.servitech_pro.data.local.entity.OrdenTrabajo;
import com.iza.servitech_pro.data.local.entity.SyncQueueItem;
import com.iza.servitech_pro.data.local.entity.UsuarioLocal;

@Database(
        entities = {
                SyncQueueItem.class,
                UsuarioLocal.class,
                OrdenTrabajo.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract SyncQueueDao syncQueueDao();

    public abstract UsuarioLocalDao usuarioLocalDao();

    public abstract OrdenTrabajoDao ordenTrabajoDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "servitech_pro.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}