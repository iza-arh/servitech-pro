package com.iza.servitech_pro.ui.ordenes;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.model.OrdenDetalle;
import com.iza.servitech_pro.data.local.model.RepuestoUsado;
import com.iza.servitech_pro.ui.common.EstadoChip;
import com.iza.servitech_pro.ui.common.SyncBarView;

import java.util.List;
import android.content.Intent;
import com.iza.servitech_pro.ui.bitacora.BitacoraActivity;
import com.iza.servitech_pro.ui.fotos.FotosActivity;
public class OrdenDetalleActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "ordenId";

    private TextView tvTitulo, tvEstado, tvClienteNombre, tvClienteTelefono, tvClienteDireccion;
    private TextView tvVehiculo, tvPlaca, tvServicio, tvDiagnostico, tvSinRepuestos;
    private ChipGroup chipsRepuestos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orden_detalle);

        String ordenId = getIntent().getStringExtra(EXTRA_ID);
        if (ordenId == null) {
            finish();
            return;
        }

        SyncBarView barra = findViewById(R.id.syncBar);
        barra.enlazar(this);

        tvTitulo = findViewById(R.id.tvTitulo);
        tvEstado = findViewById(R.id.tvEstado);
        tvClienteNombre = findViewById(R.id.tvClienteNombre);
        tvClienteTelefono = findViewById(R.id.tvClienteTelefono);
        tvClienteDireccion = findViewById(R.id.tvClienteDireccion);
        tvVehiculo = findViewById(R.id.tvVehiculo);
        tvPlaca = findViewById(R.id.tvPlaca);
        tvServicio = findViewById(R.id.tvServicio);
        tvDiagnostico = findViewById(R.id.tvDiagnostico);
        tvSinRepuestos = findViewById(R.id.tvSinRepuestos);
        chipsRepuestos = findViewById(R.id.chipsRepuestos);

        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());

        // Estos botones se conectan en los siguientes pasos
        View.OnClickListener pronto = v ->
                Toast.makeText(this, R.string.proximamente, Toast.LENGTH_SHORT).show();
        findViewById(R.id.btnFoto).setOnClickListener(v -> {
            Intent i = new Intent(this, FotosActivity.class);
            i.putExtra(FotosActivity.EXTRA_ID, ordenId);
            startActivity(i);
        });
        findViewById(R.id.btnBitacora).setOnClickListener(v -> {
            Intent i = new Intent(this, BitacoraActivity.class);
            i.putExtra(BitacoraActivity.EXTRA_ID, ordenId);
            startActivity(i);
        });
        findViewById(R.id.btnFirma).setOnClickListener(pronto);
        findViewById(R.id.btnEditar).setOnClickListener(pronto);

        AppDatabase db = AppDatabase.getInstance(this);
        db.ordenDao().observarDetalle(ordenId).observe(this, this::mostrar);
        db.registroDao().observarRepuestos(ordenId).observe(this, this::mostrarRepuestos);
    }

    private String o(String valor) {
        return (valor == null || valor.trim().isEmpty()) ? getString(R.string.detalle_sin_dato) : valor;
    }

    private void mostrar(OrdenDetalle d) {
        if (d == null) {
            finish(); // la orden ya no existe
            return;
        }
        String numero = (d.placa != null && !d.placa.isEmpty())
                ? d.placa : "#" + d.id.substring(0, 4).toUpperCase();
        tvTitulo.setText(getString(R.string.detalle_orden_titulo, numero));
        EstadoChip.aplicar(tvEstado, d.estado);

        tvClienteNombre.setText(d.clienteNombre != null ? d.clienteNombre : getString(R.string.orden_sin_cliente));
        tvClienteTelefono.setText(o(d.clienteTelefono));
        tvClienteDireccion.setText(o(d.clienteDireccion));

        StringBuilder vehiculo = new StringBuilder();
        if (d.marca != null && !d.marca.isEmpty()) vehiculo.append(d.marca).append(' ');
        if (d.modelo != null && !d.modelo.isEmpty()) vehiculo.append(d.modelo).append(' ');
        if (d.anio != null) vehiculo.append(d.anio);
        tvVehiculo.setText(o(vehiculo.toString().trim()));
        tvPlaca.setText(getString(R.string.detalle_placa, o(d.placa)));

        String servicio = getString(R.string.detalle_servicio, o(d.servicioNombre));
        if (d.categoriaNombre != null && !d.categoriaNombre.isEmpty()) {
            servicio += " · " + getString(R.string.detalle_equipo, d.categoriaNombre);
        }
        tvServicio.setText(servicio);

        tvDiagnostico.setText(o(d.descripcion));
    }

    private void mostrarRepuestos(List<RepuestoUsado> lista) {
        chipsRepuestos.removeAllViews();
        boolean vacio = lista == null || lista.isEmpty();
        tvSinRepuestos.setVisibility(vacio ? View.VISIBLE : View.GONE);
        if (vacio) {
            return;
        }
        for (RepuestoUsado r : lista) {
            Chip chip = new Chip(this);
            String nombre = r.nombre != null ? r.nombre : getString(R.string.detalle_sin_dato);
            chip.setText(r.cantidad != null ? nombre + " ×" + r.cantidad : nombre);
            chip.setClickable(false);
            chip.setCheckable(false);
            chipsRepuestos.addView(chip);
        }
    }
}