package com.iza.servitech_pro.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.entity.ClienteEntity;
import com.iza.servitech_pro.data.local.entity.OrdenEntity;
import com.iza.servitech_pro.data.remote.RetrofitClient;
import com.iza.servitech_pro.data.remote.dto.ClienteDto;
import com.iza.servitech_pro.data.remote.dto.OrdenDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class SyncRepository {

    public interface Resultado {
        void alTerminar(boolean ok, String mensaje);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private final Context app;
    private final AppDatabase db;

    public SyncRepository(Context context) {
        this.app = context.getApplicationContext();
        this.db = AppDatabase.getInstance(app);
    }

    // Descarga lo que este usuario puede ver y lo guarda en Room
    public void descargar(Resultado cb) {
    }

    private void responder(Resultado cb, boolean ok, String mensaje) {
        new Handler(Looper.getMainLooper()).post(() -> cb.alTerminar(ok, mensaje));
    }
}