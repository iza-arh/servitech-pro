package com.iza.servitech_pro.ui.dashboard;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.sync.SyncManager;
import com.iza.servitech_pro.ui.common.SyncBarView;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        SyncManager.getInstance(this).iniciar();

        SyncBarView barra = findViewById(R.id.syncBar);
        barra.enlazar(this);

        AppDatabase db = AppDatabase.getInstance(this);
        TextView tvPendientes = findViewById(R.id.tvPendientes);
        TextView tvEnProceso = findViewById(R.id.tvEnProceso);
        TextView tvCompletadas = findViewById(R.id.tvCompletadas);

        // Los números se actualizan solos cuando cambian las órdenes locales
        db.ordenDao().contarPorEstado("PENDIENTE").observe(this,
                n -> tvPendientes.setText(String.valueOf(n == null ? 0 : n)));
        db.ordenDao().contarPorEstado("EN_PROCESO").observe(this,
                n -> tvEnProceso.setText(String.valueOf(n == null ? 0 : n)));
        db.ordenDao().contarPorEstado("COMPLETADA").observe(this,
                n -> tvCompletadas.setText(String.valueOf(n == null ? 0 : n)));
    }
}