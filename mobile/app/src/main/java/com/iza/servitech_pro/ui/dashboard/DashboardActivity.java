package com.iza.servitech_pro.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.repository.SecurePrefs;
import com.iza.servitech_pro.data.sync.SyncManager;
import com.iza.servitech_pro.ui.catalogo.Catalogo;
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

        android.content.SharedPreferences estado = getSharedPreferences("app_estado", MODE_PRIVATE);
        String usuario = estado.getString("ultimo_usuario", "");
        String nombre = estado.getString("nombre_usuario", usuario); // si no hay nombre, usa el usuario
        if (nombre == null || nombre.trim().isEmpty()) nombre = getString(R.string.panel_usuario_defecto);

        TextView tvRol = findViewById(R.id.tvRol);
        TextView tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        tvRol.setText(textoRol(SecurePrefs.leerRol(this)));
        tvNombreUsuario.setText(getString(R.string.panel_bienvenida, nombre));

        AppDatabase db = AppDatabase.getInstance(this);
        TextView tvPendientes = findViewById(R.id.tvPendientes);
        TextView tvEnProceso = findViewById(R.id.tvEnProceso);
        TextView tvCompletadas = findViewById(R.id.tvCompletadas);
        MaterialButton btnThema = findViewById(R.id.btnThema);

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

        //Para cambiar thema (claro y obscuro).
        int modoActual = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean esModoOscuro = (modoActual == android.content.res.Configuration.UI_MODE_NIGHT_YES);

        if (esModoOscuro) {
            btnThema.setIconResource(R.drawable.ic_light);
        } else {
            btnThema.setIconResource(R.drawable.ic_dark);
        }

        btnThema.setOnClickListener(v -> {
            if (esModoOscuro) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        });

        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, v);
            menu.getMenu().add(0, 1, 0, R.string.menu_ubicacion);
            if ("ADMIN".equals(SecurePrefs.leerRol(this))) {
                menu.getMenu().add(0, 2, 1, R.string.menu_usuarios);
            }
            if ("ADMIN".equals(SecurePrefs.leerRol(this))) {
                menu.getMenu().add(0, 3, 8, R.string.menu_catalogo);
            }
            menu.getMenu().add(0, 4, 9, R.string.menu_cerrar_sesion);
            menu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1:
                        startActivity(new Intent(this, TallerActivity.class));
                        return true;
                    case 2:
                        startActivity(new Intent(this, UsuariosActivity.class));
                        return true;
                    case 3:
                        startActivity(new Intent(this, Catalogo.class));
                        return true;
                    case 4:
                        cerrarSesion();
                        return true;
                    default:
                        return false;
                }
            });
            menu.show();
        });
    }

    private String textoRol(String rol) {
        if (rol == null) return "";
        switch (rol) {
            case "ADMIN":   return getString(R.string.rol_admin);
            case "TECNICO": return getString(R.string.rol_tecnico);
            case "CLIENTE": return getString(R.string.rol_cliente);
            default:        return rol;
        }
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