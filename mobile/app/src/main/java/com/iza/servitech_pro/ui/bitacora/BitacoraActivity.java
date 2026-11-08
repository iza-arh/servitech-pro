package com.iza.servitech_pro.ui.bitacora;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.FechaUtil;
import com.iza.servitech_pro.data.local.entity.RegistroEntity;
import com.iza.servitech_pro.data.repository.LocalStore;
import com.iza.servitech_pro.data.sync.SyncManager;
import com.iza.servitech_pro.ui.common.SyncBarView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BitacoraActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "ordenId";

    private String ordenId;
    private LocalStore store;
    private BitacoraAdapter adapter;
    private RecyclerView rv;
    private TextView tvVacia, tvBloqueada;
    private EditText etNota;
    private View barraNota, barraEdicion;
    private int cantidadAnterior = 0;
    private boolean bloqueada = false;
    private RegistroEntity editando = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitacora);

        ordenId = getIntent().getStringExtra(EXTRA_ID);
        if (ordenId == null) {
            finish();
            return;
        }
        store = new LocalStore(this);

        SyncBarView barra = findViewById(R.id.syncBar);
        barra.enlazar(this);

        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        tvVacia = findViewById(R.id.tvVacia);
        tvBloqueada = findViewById(R.id.tvBloqueada);
        etNota = findViewById(R.id.etNota);
        barraNota = findViewById(R.id.barraNota);
        barraEdicion = findViewById(R.id.barraEdicion);
        TextView tvOrden = findViewById(R.id.tvOrden);

        rv = findViewById(R.id.rvBitacora);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BitacoraAdapter();
        adapter.setListener(new BitacoraAdapter.Listener() {
            @Override
            public void alEditar(RegistroEntity nota) {
                iniciarEdicion(nota);
            }

            @Override
            public void alEliminar(RegistroEntity nota) {
                confirmarEliminar(nota);
            }
        });
        rv.setAdapter(adapter);

        AppDatabase db = AppDatabase.getInstance(this);

        db.ordenDao().observarDetalle(ordenId).observe(this, d -> {
            if (d != null) {
                String numero = (d.placa != null && !d.placa.isEmpty())
                        ? d.placa : "#" + d.id.substring(0, 4).toUpperCase();
                tvOrden.setText(getString(R.string.bitacora_orden, numero));
                aplicarBloqueo("COMPLETADA".equals(d.estado));
            }
        });

        db.registroDao().observarBitacoras(ordenId).observe(this, lista -> {
            List<RegistroEntity> ordenada = lista == null ? new ArrayList<>() : new ArrayList<>(lista);
            // Cronológico por fecha de creación: editar una nota no cambia su lugar
            Collections.sort(ordenada, (a, b) -> Long.compare(momento(a), momento(b)));
            boolean crecio = ordenada.size() > cantidadAnterior;
            cantidadAnterior = ordenada.size();
            adapter.submitList(ordenada, () -> {
                if (crecio && !ordenada.isEmpty()) {
                    rv.scrollToPosition(ordenada.size() - 1);
                }
            });
            tvVacia.setVisibility(ordenada.isEmpty() ? View.VISIBLE : View.GONE);
        });

        findViewById(R.id.btnEnviar).setOnClickListener(v -> enviar());
        findViewById(R.id.btnCancelarEdicion).setOnClickListener(v -> cancelarEdicion());
    }

    private static long momento(RegistroEntity r) {
        return FechaUtil.aMillis(r.creadoEn != null ? r.creadoEn : r.updatedAt, r.localTs);
    }

    // Orden completada: la bitácora queda solo de lectura
    private void aplicarBloqueo(boolean b) {
        bloqueada = b;
        adapter.setEditable(!b);
        barraNota.setVisibility(b ? View.GONE : View.VISIBLE);
        tvBloqueada.setVisibility(b ? View.VISIBLE : View.GONE);
        if (b) {
            cancelarEdicion();
        }
    }

    private void iniciarEdicion(RegistroEntity nota) {
        if (bloqueada) {
            return;
        }
        editando = nota;
        barraEdicion.setVisibility(View.VISIBLE);
        etNota.setText(nota.texto);
        etNota.setSelection(etNota.getText().length());
        etNota.requestFocus();
    }

    private void cancelarEdicion() {
        editando = null;
        barraEdicion.setVisibility(View.GONE);
        etNota.setText("");
    }

    private void enviar() {
        if (bloqueada) {
            return;
        }
        String texto = etNota.getText().toString().trim();
        if (texto.isEmpty()) {
            Toast.makeText(this, R.string.bitacora_error_vacia, Toast.LENGTH_SHORT).show();
            return;
        }

        RegistroEntity r;
        if (editando != null) {
            if (texto.equals(editando.texto)) {   // sin cambios
                cancelarEdicion();
                return;
            }
            r = copiar(editando);
            r.texto = texto;
        } else {
            r = new RegistroEntity();
            r.id = LocalStore.nuevoId();
            r.ordenId = ordenId;
            r.tipo = "BITACORA";
            r.texto = texto;
        }
        editando = null;
        barraEdicion.setVisibility(View.GONE);
        etNota.setText("");
        guardar(r);
    }

    private void guardar(RegistroEntity r) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            String error = store.guardarRegistro(r);
            runOnUiThread(() -> {
                if (error != null) {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                } else {
                    SyncManager.getInstance(this).sincronizar();
                }
            });
        });
    }

    private void confirmarEliminar(RegistroEntity nota) {
        if (bloqueada) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.bitacora_eliminar)
                .setMessage(R.string.bitacora_eliminar_msg)
                .setNegativeButton(R.string.bitacora_cancelar, null)
                .setPositiveButton(R.string.fotos_eliminar, (d, w) -> {
                    if (editando != null && editando.id.equals(nota.id)) {
                        cancelarEdicion();
                    }
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        store.borrarRegistro(nota.id);
                        runOnUiThread(() -> SyncManager.getInstance(this).sincronizar());
                    });
                })
                .show();
    }

    // Copia para no modificar el objeto que usa la lista
    private static RegistroEntity copiar(RegistroEntity o) {
        RegistroEntity c = new RegistroEntity();
        c.id = o.id;
        c.ordenId = o.ordenId;
        c.tipo = o.tipo;
        c.texto = o.texto;
        c.catalogoItemId = o.catalogoItemId;
        c.cantidad = o.cantidad;
        c.datos = o.datos;
        c.updatedAt = o.updatedAt;
        c.deletedAt = o.deletedAt;
        c.creadoEn = o.creadoEn;
        c.isSynced = o.isSynced;
        c.localTs = o.localTs;
        return c;
    }
}