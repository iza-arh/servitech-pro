package com.iza.servitech_pro.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.iza.servitech_pro.data.local.dao.CatalogoDao;
import com.iza.servitech_pro.data.local.dao.ClienteDao;
import com.iza.servitech_pro.data.local.dao.OrdenDao;
import com.iza.servitech_pro.data.local.dao.RegistroDao;
import com.iza.servitech_pro.data.local.dao.SyncQueueDao;
import com.iza.servitech_pro.data.local.entity.CatalogoEntity;
import com.iza.servitech_pro.data.local.entity.ClienteEntity;
import com.iza.servitech_pro.data.local.entity.OrdenEntity;
import com.iza.servitech_pro.data.local.entity.RegistroEntity;
import com.iza.servitech_pro.data.local.entity.SyncQueueItem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {
        ClienteEntity.class,
        CatalogoEntity.class,
        OrdenEntity.class,
        RegistroEntity.class,
        SyncQueueItem.class
}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ClienteDao clienteDao();
    public abstract CatalogoDao catalogoDao();
    public abstract OrdenDao ordenDao();
    public abstract RegistroDao registroDao();
    public abstract SyncQueueDao syncQueueDao();

    // Room no permite consultas en el hilo principal: todo se ejecuta aquí
    public static final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "servitech_pro.db")
                            .fallbackToDestructiveMigration() // la versión 1 solo tenía una cola vacía
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
