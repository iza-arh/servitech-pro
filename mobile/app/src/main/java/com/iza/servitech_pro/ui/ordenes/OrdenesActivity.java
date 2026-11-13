package com.iza.servitech_pro.ui.ordenes;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.model.OrdenResumen;
import com.iza.servitech_pro.ui.common.SyncBarView;

import java.util.List;
import android.content.Intent;

import com.iza.servitech_pro.ui.common.Sesion;

import com.iza.servitech_pro.ui.common.TecnicosCache;

public class OrdenesActivity extends AppCompatActivity {

    public static final String EXTRA_ESTADO = "estado";

    private AppDatabase db;
    private OrdenesAdapter adapter;
    private TextView tvVacio;
    private LiveData<List<OrdenResumen>> fuente;
    private String estado = "";
    private String texto = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ordenes);
        db = AppDatabase.getInstance(this);

        SyncBarView barra = findViewById(R.id.syncBar);
        barra.enlazar(this);

        tvVacio = findViewById(R.id.tvVacio);
        RecyclerView rv = findViewById(R.id.rvOrdenes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrdenesAdapter();
        rv.setAdapter(adapter);
        adapter.setOnOrdenClick(orden -> {
            Intent i = new Intent(this, OrdenDetalleActivity.class);
            i.putExtra(OrdenDetalleActivity.EXTRA_ID, orden.id);
            startActivity(i);
        });

        // Filtro inicial que viene del Panel
        String inicial = getIntent().getStringExtra(EXTRA_ESTADO);
        if (inicial != null) {
            estado = inicial;
        }
        ChipGroup grupo = findViewById(R.id.chipGroup);
        switch (estado) {
            case "PENDIENTE": grupo.check(R.id.chipPendientes); break;
            case "EN_PROCESO": grupo.check(R.id.chipEnProceso); break;
            case "COMPLETADA": grupo.check(R.id.chipCompletadas); break;
            default: grupo.check(R.id.chipTodas); break;
        }
        grupo.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) {
                return;
            }
            int id = ids.get(0);
            if (id == R.id.chipPendientes) estado = "PENDIENTE";
            else if (id == R.id.chipEnProceso) estado = "EN_PROCESO";
            else if (id == R.id.chipCompletadas) estado = "COMPLETADA";
            else estado = "";
            cargar();
        });

        EditText etBuscar = findViewById(R.id.etBuscar);
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                texto = s.toString().trim();
                cargar();
            }
        });

        cargar();

        findViewById(R.id.fabNueva).setOnClickListener(v ->
                startActivity(new Intent(this, OrdenFormActivity.class)));

        if (Sesion.esCliente(this)) {
            findViewById(R.id.fabNueva).setVisibility(android.view.View.GONE);
        }

        TecnicosCache.actualizar(this, () -> adapter.notifyDataSetChanged());
    }

    // Cambia la consulta cuando cambia el filtro o el texto de búsqueda
    private void cargar() {
        if (fuente != null) {
            fuente.removeObservers(this);
        }
        fuente = db.ordenDao().observarResumen(estado, texto);
        fuente.observe(this, lista -> {
            adapter.submitList(lista);
            tvVacio.setVisibility(lista == null || lista.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        });
    }
}