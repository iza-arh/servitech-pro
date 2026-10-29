package com.iza.servitech_pro.ui.dashboard;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.repository.SyncRepository;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        // Prueba temporal: abre la BD para que aparezca en el Database Inspector
        new Thread(() -> AppDatabase.getInstance(this).clienteDao().observarActivos()).start();
        new SyncRepository(this).descargar((ok, mensaje) ->
                Log.d("SYNC", "ok=" + ok + " " + mensaje));
    }
}