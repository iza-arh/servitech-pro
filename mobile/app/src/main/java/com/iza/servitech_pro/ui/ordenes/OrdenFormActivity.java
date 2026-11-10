package com.iza.servitech_pro.ui.ordenes;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.entity.CatalogoEntity;
import com.iza.servitech_pro.data.local.entity.ClienteEntity;
import com.iza.servitech_pro.data.local.entity.OrdenEntity;
import com.iza.servitech_pro.data.local.entity.RegistroEntity;
import com.iza.servitech_pro.data.repository.LocalStore;
import com.iza.servitech_pro.data.sync.SyncManager;
import com.iza.servitech_pro.ui.common.Sesion;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import com.iza.servitech_pro.ui.common.Sesion;
public class OrdenFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "ordenId";
    private static final String[] ESTADOS = {"PENDIENTE", "EN_PROCESO", "COMPLETADA"};

    // Un repuesto de la orden mientras se edita (no se guarda hasta pulsar "Guardar orden")
    private static class Parte {
        RegistroEntity original;   // null = nuevo
        String catalogoId;
        String nombre;
        int cantidad;
        boolean modificado;
    }

    private interface Elegir {
        void en(int posicion);
    }

    private abstract static class AlEscribir implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int a, int b, int c) { }

        @Override
        public void onTextChanged(CharSequence s, int a, int b, int c) { }
    }

    private AppDatabase db;
    private LocalStore store;
    private boolean esNueva;
    private String ordenId;

    private List<ClienteEntity> clientes = new ArrayList<>();
    private List<CatalogoEntity> servicios = new ArrayList<>();
    private List<CatalogoEntity> equipos = new ArrayList<>();
    private List<CatalogoEntity> repuestos = new ArrayList<>();
    private final List<Parte> partes = new ArrayList<>();
    private final List<String> quitados = new ArrayList<>();

    private String clienteSel, servicioSel, equipoSel, estadoSel = "PENDIENTE";
    private String snapshotInicial = "";

    private TextView tvTitulo, tvSinRepuestos;
    private TextInputLayout tilCliente, tilPlaca, tilAnio, tilMarca, tilModelo, tilDiagnostico;
    private AutoCompleteTextView acCliente, acServicio, acEquipo, acEstado;
    private EditText etPlaca, etAnio, etMarca, etModelo, etDiagnostico;
    private ChipGroup chipsRepuestos;
    private View btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Sesion.esCliente(this)) {
            finish();
            return;
        }

        setContentView(R.layout.activity_orden_form);

        db = AppDatabase.getInstance(this);
        store = new LocalStore(this);
        String id = getIntent().getStringExtra(EXTRA_ID);
        esNueva = (id == null);
        ordenId = esNueva ? LocalStore.nuevoId() : id;

        tvTitulo = findViewById(R.id.tvTitulo);
        tvSinRepuestos = findViewById(R.id.tvSinRepuestos);
        tilCliente = findViewById(R.id.tilCliente);
        tilPlaca = findViewById(R.id.tilPlaca);
        tilAnio = findViewById(R.id.tilAnio);
        tilMarca = findViewById(R.id.tilMarca);
        tilModelo = findViewById(R.id.tilModelo);
        tilDiagnostico = findViewById(R.id.tilDiagnostico);
        acCliente = findViewById(R.id.acCliente);
        acServicio = findViewById(R.id.acServicio);
        acEquipo = findViewById(R.id.acEquipo);
        acEstado = findViewById(R.id.acEstado);
        etPlaca = findViewById(R.id.etPlaca);
        etAnio = findViewById(R.id.etAnio);
        etMarca = findViewById(R.id.etMarca);
        etModelo = findViewById(R.id.etModelo);
        etDiagnostico = findViewById(R.id.etDiagnostico);
        chipsRepuestos = findViewById(R.id.chipsRepuestos);
        btnGuardar = findViewById(R.id.btnGuardar);

        tvTitulo.setText(esNueva ? getString(R.string.form_nueva) : "");

        limpiarAlEscribir(tilPlaca, etPlaca);
        limpiarAlEscribir(tilAnio, etAnio);
        limpiarAlEscribir(tilMarca, etMarca);
        limpiarAlEscribir(tilModelo, etModelo);
        limpiarAlEscribir(tilDiagnostico, etDiagnostico);

        findViewById(R.id.btnAtras).setOnClickListener(v -> intentarSalir());
        findViewById(R.id.btnCancelar).setOnClickListener(v -> intentarSalir());
        btnGuardar.setOnClickListener(v -> guardar());
        findViewById(R.id.btnAgregarRepuesto).setOnClickListener(v -> dialogoRepuesto());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                intentarSalir();
            }
        });

        cargarDatos();
    }

    private void limpiarAlEscribir(TextInputLayout til, EditText et) {
        et.addTextChangedListener(new AlEscribir() {
            @Override
            public void afterTextChanged(Editable s) {
                til.setError(null);
            }
        });
    }

    // ---------- Carga ----------

    private static List<CatalogoEntity> ordenadas(List<CatalogoEntity> lista) {
        List<CatalogoEntity> copia = new ArrayList<>(lista);
        Collections.sort(copia, (a, b) -> String.valueOf(a.nombre).compareToIgnoreCase(String.valueOf(b.nombre)));
        return copia;
    }

    private void cargarDatos() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ClienteEntity> cl = db.clienteDao().getActivos();
            List<CatalogoEntity> sv = ordenadas(db.catalogoDao().getActivosPorTipo("TIPO_SERVICIO"));
            List<CatalogoEntity> eq = ordenadas(db.catalogoDao().getActivosPorTipo("CATEGORIA_EQUIPO"));
            List<CatalogoEntity> rp = ordenadas(db.catalogoDao().getActivosPorTipo("REPUESTO"));
            OrdenEntity ex = esNueva ? null : db.ordenDao().getById(ordenId);
            List<RegistroEntity> reps = esNueva ? new ArrayList<>() : db.registroDao().getRepuestosActivos(ordenId);
            runOnUiThread(() -> poblar(cl, sv, eq, rp, ex, reps));
        });
    }

    private void poblar(List<ClienteEntity> cl, List<CatalogoEntity> sv, List<CatalogoEntity> eq,
                        List<CatalogoEntity> rp, OrdenEntity ex, List<RegistroEntity> reps) {
        if (!esNueva && ex == null) {
            finish(); // la orden ya no existe
            return;
        }
        clientes = cl;
        servicios = sv;
        equipos = eq;
        repuestos = rp;

        // Dropdowns
        List<String> nombresClientes = new ArrayList<>();
        for (ClienteEntity c : clientes) nombresClientes.add(c.nombre);
        dropdown(acCliente, nombresClientes, pos -> {
            clienteSel = clientes.get(pos).id;
            tilCliente.setError(null);
        });
        dropdown(acServicio, conNinguno(servicios), pos -> servicioSel = pos == 0 ? null : servicios.get(pos - 1).id);
        dropdown(acEquipo, conNinguno(equipos), pos -> equipoSel = pos == 0 ? null : equipos.get(pos - 1).id);
        dropdown(acEstado, etiquetasEstado(), pos -> estadoSel = ESTADOS[pos]);

        if (ex != null) {
            String numero = (ex.placa != null && !ex.placa.isEmpty()) ? ex.placa : "#" + ex.id.substring(0, 4).toUpperCase();
            tvTitulo.setText(getString(R.string.form_editar, numero));
            etPlaca.setText(ex.placa);
            etMarca.setText(ex.marca);
            etModelo.setText(ex.modelo);
            etAnio.setText(ex.anio != null ? String.valueOf(ex.anio) : "");
            etDiagnostico.setText(ex.descripcion);
            clienteSel = ex.clienteId;
            acCliente.setText(nombreCliente(ex.clienteId), false);
            servicioSel = ex.tipoServicioId;
            acServicio.setText(nombreCatalogo(servicios, ex.tipoServicioId), false);
            equipoSel = ex.categoriaEquipoId;
            acEquipo.setText(nombreCatalogo(equipos, ex.categoriaEquipoId), false);
            estadoSel = ex.estado != null ? ex.estado : "PENDIENTE";

            for (RegistroEntity r : reps) {
                Parte p = new Parte();
                p.original = r;
                p.catalogoId = r.catalogoItemId;
                String n = nombreCatalogo(repuestos, r.catalogoItemId);
                p.nombre = n.equals(getString(R.string.form_ninguno)) ? getString(R.string.form_rep_repuesto) : n;
                p.cantidad = r.cantidad != null ? r.cantidad : 1;
                partes.add(p);
            }
        } else {
            acServicio.setText(getString(R.string.form_ninguno), false);
            acEquipo.setText(getString(R.string.form_ninguno), false);
        }
        acEstado.setText(etiquetasEstado().get(indiceEstado(estadoSel)), false);
        pintarPartes();
        snapshotInicial = snapshot();
    }

    private void dropdown(AutoCompleteTextView ac, List<String> etiquetas, Elegir al) {
        ac.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, etiquetas));
        ac.setOnItemClickListener((p, v, pos, id) -> al.en(pos));
    }

    private List<String> conNinguno(List<CatalogoEntity> lista) {
        List<String> r = new ArrayList<>();
        r.add(getString(R.string.form_ninguno));
        for (CatalogoEntity c : lista) r.add(c.nombre);
        return r;
    }

    private List<String> etiquetasEstado() {
        List<String> r = new ArrayList<>();
        r.add(getString(R.string.estado_pendiente));
        r.add(getString(R.string.estado_en_proceso));
        r.add(getString(R.string.estado_completada));
        return r;
    }

    private int indiceEstado(String estado) {
        for (int i = 0; i < ESTADOS.length; i++) {
            if (ESTADOS[i].equals(estado)) return i;
        }
        return 0;
    }

    private String nombreCliente(String id) {
        for (ClienteEntity c : clientes) {
            if (c.id.equals(id)) return c.nombre;
        }
        return "";
    }

    private String nombreCatalogo(List<CatalogoEntity> lista, String id) {
        if (id != null) {
            for (CatalogoEntity c : lista) {
                if (c.id.equals(id)) return c.nombre;
            }
        }
        return getString(R.string.form_ninguno);
    }

    // ---------- Repuestos ----------

    private void pintarPartes() {
        chipsRepuestos.removeAllViews();
        tvSinRepuestos.setVisibility(partes.isEmpty() ? View.VISIBLE : View.GONE);
        for (Parte p : partes) {
            Chip chip = new Chip(this);
            chip.setText(p.nombre + " ×" + p.cantidad);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                partes.remove(p);
                if (p.original != null) {
                    quitados.add(p.original.id);
                }
                pintarPartes();
            });
            chipsRepuestos.addView(chip);
        }
    }

    private void dialogoRepuesto() {
        if (repuestos.isEmpty()) {
            Toast.makeText(this, R.string.form_rep_sin_catalogo, Toast.LENGTH_SHORT).show();
            return;
        }
        View v = getLayoutInflater().inflate(R.layout.dialog_repuesto, null);
        TextInputLayout tilRep = v.findViewById(R.id.tilRepuesto);
        TextInputLayout tilCant = v.findViewById(R.id.tilCantidad);
        AutoCompleteTextView ac = v.findViewById(R.id.acRepuesto);
        EditText etCant = v.findViewById(R.id.etCantidad);

        List<String> nombres = new ArrayList<>();
        for (CatalogoEntity r : repuestos) nombres.add(r.nombre);
        final int[] elegido = {-1};
        ac.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombres));
        ac.setOnItemClickListener((p, view, i, id) -> {
            elegido[0] = i;
            tilRep.setError(null);
        });

        AlertDialog d = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.form_rep_titulo)
                .setView(v)
                .setNegativeButton(R.string.form_cancelar, null)
                .setPositiveButton(R.string.form_rep_agregar, null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            boolean ok = true;
            if (elegido[0] < 0) {
                tilRep.setError(getString(R.string.form_rep_err_repuesto));
                ok = false;
            }
            int cantidad = 0;
            try {
                cantidad = Integer.parseInt(etCant.getText().toString().trim());
            } catch (NumberFormatException ignorada) {
            }
            if (cantidad < 1 || cantidad > 999) {
                tilCant.setError(getString(R.string.form_rep_err_cantidad));
                ok = false;
            } else {
                tilCant.setError(null);
            }
            if (ok) {
                agregarParte(repuestos.get(elegido[0]), cantidad);
                d.dismiss();
            }
        }));
        d.show();
    }

    private void agregarParte(CatalogoEntity item, int cantidad) {
        for (Parte p : partes) {          // si ya está en la lista, se suma la cantidad
            if (item.id.equals(p.catalogoId)) {
                p.cantidad = Math.min(999, p.cantidad + cantidad);
                if (p.original != null) p.modificado = true;
                pintarPartes();
                return;
            }
        }
        Parte p = new Parte();
        p.catalogoId = item.id;
        p.nombre = item.nombre;
        p.cantidad = cantidad;
        partes.add(p);
        pintarPartes();
    }

    // ---------- Salir / cambios ----------

    private String snapshot() {
        StringBuilder sb = new StringBuilder();
        sb.append(clienteSel).append('|').append(servicioSel).append('|').append(equipoSel).append('|')
                .append(estadoSel).append('|').append(etPlaca.getText()).append('|')
                .append(etMarca.getText()).append('|').append(etModelo.getText()).append('|')
                .append(etAnio.getText()).append('|').append(etDiagnostico.getText()).append('|');
        for (Parte p : partes) sb.append(p.catalogoId).append(':').append(p.cantidad).append(',');
        sb.append('|').append(quitados.size());
        return sb.toString();
    }

    private void intentarSalir() {
        if (snapshot().equals(snapshotInicial)) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.form_descartar_titulo)
                .setMessage(R.string.form_descartar_msg)
                .setNegativeButton(R.string.form_seguir, null)
                .setPositiveButton(R.string.form_descartar, (d, w) -> finish())
                .show();
    }

    // ---------- Validar y guardar ----------

    private boolean validar() {
        boolean ok = true;
        View primero = null;

        tilCliente.setError(null);
        if (clienteSel == null) {
            tilCliente.setError(getString(R.string.form_err_cliente));
            primero = tilCliente;
            ok = false;
        }

        String placa = etPlaca.getText().toString().trim();
        if (placa.isEmpty()) {
            tilPlaca.setError(getString(R.string.form_err_placa));
        } else if (placa.length() < 2) {
            tilPlaca.setError(getString(R.string.form_err_placa_corta));
        } else if (placa.length() > 15) {
            tilPlaca.setError(getString(R.string.form_err_placa_larga));
        } else {
            tilPlaca.setError(null);
        }
        if (tilPlaca.getError() != null) {
            if (primero == null) primero = tilPlaca;
            ok = false;
        }

        if (etMarca.getText().toString().trim().length() > 40) {
            tilMarca.setError(getString(R.string.form_err_texto_largo));
            if (primero == null) primero = tilMarca;
            ok = false;
        }
        if (etModelo.getText().toString().trim().length() > 40) {
            tilModelo.setError(getString(R.string.form_err_texto_largo));
            if (primero == null) primero = tilModelo;
            ok = false;
        }

        String anio = etAnio.getText().toString().trim();
        if (!anio.isEmpty()) {
            int maximo = Calendar.getInstance().get(Calendar.YEAR) + 1;
            int valor = 0;
            try {
                valor = Integer.parseInt(anio);
            } catch (NumberFormatException ignorada) {
            }
            if (valor < 1950 || valor > maximo) {
                tilAnio.setError(getString(R.string.form_err_anio, maximo));
                if (primero == null) primero = tilAnio;
                ok = false;
            }
        }

        if (etDiagnostico.getText().toString().trim().isEmpty()) {
            tilDiagnostico.setError(getString(R.string.form_err_diagnostico));
            if (primero == null) primero = tilDiagnostico;
            ok = false;
        }

        if (primero != null) {
            primero.requestFocus();
        }
        return ok;
    }

    private static String vacioANulo(String s) {
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

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

    private void guardar() {
        if (!validar()) {
            return;
        }
        btnGuardar.setEnabled(false); // evita guardar dos veces

        final String placa = etPlaca.getText().toString().trim();
        final String marca = vacioANulo(etMarca.getText().toString());
        final String modelo = vacioANulo(etModelo.getText().toString());
        final String anioTxt = etAnio.getText().toString().trim();
        final Integer anio = anioTxt.isEmpty() ? null : Integer.valueOf(anioTxt);
        final String diagnostico = etDiagnostico.getText().toString().trim();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            OrdenEntity o = esNueva ? new OrdenEntity() : db.ordenDao().getById(ordenId);
            if (o == null) {
                runOnUiThread(this::finish);
                return;
            }
            if (esNueva) {
                o.id = ordenId;
            }
            o.clienteId = clienteSel;
            o.tipoServicioId = servicioSel;
            o.categoriaEquipoId = equipoSel;
            o.placa = placa;
            o.marca = marca;
            o.modelo = modelo;
            o.anio = anio;
            o.descripcion = diagnostico;
            o.estado = estadoSel;
            store.guardarOrden(o);

            for (Parte p : partes) {
                if (p.original == null) {
                    RegistroEntity r = new RegistroEntity();
                    r.id = LocalStore.nuevoId();
                    r.ordenId = ordenId;
                    r.tipo = "REPUESTO";
                    r.catalogoItemId = p.catalogoId;
                    r.cantidad = p.cantidad;
                    store.guardarRegistro(r);
                } else if (p.modificado) {
                    RegistroEntity r = copiar(p.original);
                    r.cantidad = p.cantidad;
                    store.guardarRegistro(r);
                }
            }
            for (String rid : quitados) {
                store.borrarRegistro(rid);
            }

            runOnUiThread(() -> {
                SyncManager.getInstance(this).sincronizar();
                Toast.makeText(this, R.string.form_guardado, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}