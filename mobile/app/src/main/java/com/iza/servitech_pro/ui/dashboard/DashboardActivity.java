package com.iza.servitech_pro.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.repository.SecurePrefs;
import com.iza.servitech_pro.data.sync.SyncManager;
import com.iza.servitech_pro.ui.common.SyncBarView;
import com.iza.servitech_pro.ui.login.LoginActivity;
import com.iza.servitech_pro.ui.ordenes.OrdenesActivity;
import com.iza.servitech_pro.ui.taller.TallerActivity;
import com.iza.servitech_pro.ui.usuarios.UsuariosActivity;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        SyncManager.getInstance(this).sincronizar();
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

        findViewById(R.id.cardPendientes).setOnClickListener(v -> abrirOrdenes("PENDIENTE"));
        findViewById(R.id.cardEnProceso).setOnClickListener(v -> abrirOrdenes("EN_PROCESO"));
        findViewById(R.id.cardCompletadas).setOnClickListener(v -> abrirOrdenes("COMPLETADA"));
        findViewById(R.id.btnVerTodas).setOnClickListener(v -> abrirOrdenes(""));

        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, v);
            menu.getMenu().add(0, 1, 0, R.string.menu_ubicacion);
            if ("ADMIN".equals(SecurePrefs.leerRol(this))) {
                menu.getMenu().add(0, 2, 1, R.string.menu_usuarios);
            }
            menu.getMenu().add(0, 3, 9, R.string.menu_cerrar_sesion);
            menu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1:
                        startActivity(new Intent(this, TallerActivity.class));
                        return true;
                    case 2:
                        startActivity(new Intent(this, UsuariosActivity.class));
                        return true;
                    case 3:
                        cerrarSesion();
                        return true;
                    default:
                        return false;
                }
            });
            menu.show();
        });
    }

    private void abrirOrdenes(String estado) {
        Intent i = new Intent(this, OrdenesActivity.class);
        i.putExtra(OrdenesActivity.EXTRA_ESTADO, estado);
        startActivity(i);
    }

    // Avisa si hay cambios sin enviar, porque al salir se borran los datos locales
    private void cerrarSesion() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int pendientes = AppDatabase.getInstance(this).syncQueueDao().getPendientes().size();
            runOnUiThread(() -> confirmarCierre(pendientes));
        });
    }

    private void confirmarCierre(int pendientes) {
        String msg = pendientes > 0
                ? getString(R.string.logout_msg_pendientes, pendientes)
                : getString(R.string.logout_msg);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_cerrar_sesion)
                .setMessage(msg)
                .setNegativeButton(R.string.form_cancelar, null)
                .setPositiveButton(R.string.menu_cerrar_sesion, (d, w) -> ejecutarCierre())
                .show();
    }

    private void ejecutarCierre() {
        SecurePrefs.cerrarSesion(this);
        getSharedPreferences("app_estado", MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("tecnicos_cache", MODE_PRIVATE).edit().clear().apply();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getInstance(this).clearAllTables();
            runOnUiThread(() -> {
                Intent i = new Intent(this, LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            });
        });
    }
}