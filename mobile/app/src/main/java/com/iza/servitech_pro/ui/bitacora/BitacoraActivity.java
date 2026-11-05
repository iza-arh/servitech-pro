package com.iza.servitech_pro.ui.bitacora;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private TextView tvVacia;
    private EditText etNota;
    private int cantidadAnterior = 0;

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
        etNota = findViewById(R.id.etNota);
        TextView tvOrden = findViewById(R.id.tvOrden);

        rv = findViewById(R.id.rvBitacora);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BitacoraAdapter();
        rv.setAdapter(adapter);

        AppDatabase db = AppDatabase.getInstance(this);

        db.ordenDao().observarDetalle(ordenId).observe(this, d -> {
            if (d != null) {
                String numero = (d.placa != null && !d.placa.isEmpty())
                        ? d.placa : "#" + d.id.substring(0, 4).toUpperCase();
                tvOrden.setText(getString(R.string.bitacora_orden, numero));
            }
        });

        db.registroDao().observarBitacoras(ordenId).observe(this, lista -> {
            List<RegistroEntity> ordenada = lista == null ? new ArrayList<>() : new ArrayList<>(lista);
            // Cronológico: la más antigua arriba, la más nueva abajo
            Collections.sort(ordenada, (a, b) -> Long.compare(
                    FechaUtil.aMillis(a.updatedAt, a.localTs),
                    FechaUtil.aMillis(b.updatedAt, b.localTs)));
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
    }

    private void enviar() {
        String texto = etNota.getText().toString().trim();
        if (texto.isEmpty()) {
            Toast.makeText(this, R.string.bitacora_error_vacia, Toast.LENGTH_SHORT).show();
            return;
        }
        RegistroEntity r = new RegistroEntity();
        r.id = LocalStore.nuevoId();
        r.ordenId = ordenId;
        r.tipo = "BITACORA";
        r.texto = texto;
        etNota.setText("");

        AppDatabase.databaseWriteExecutor.execute(() -> {
            String error = store.guardarRegistro(r);
            runOnUiThread(() -> {
                if (error != null) {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                } else {
                    SyncManager.getInstance(this).sincronizar(); // si hay red, se envía enseguida
                }
            });
        });
    }
}