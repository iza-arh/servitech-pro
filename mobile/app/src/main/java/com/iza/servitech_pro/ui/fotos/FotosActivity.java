package com.iza.servitech_pro.ui.fotos;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.entity.RegistroEntity;
import com.iza.servitech_pro.data.local.model.FotoItem;
import com.iza.servitech_pro.data.repository.LocalStore;
import com.iza.servitech_pro.data.sync.SyncManager;
import com.iza.servitech_pro.ui.common.SyncBarView;
import com.iza.servitech_pro.util.ImagenUtil;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.iza.servitech_pro.ui.common.Sesion;

public class FotosActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "ordenId";

    private String ordenId;
    private LocalStore store;
    private AppDatabase db;
    private FotosAdapter adapter;
    private TextView tvCantidad, tvVacia;
    private Uri uriCamara;
    private final ExecutorService procesador = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Uri> tomarFoto = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> {
                if (Boolean.TRUE.equals(ok) && uriCamara != null) {
                    procesar(uriCamara);
                }
            });

    private final ActivityResultLauncher<String> elegirGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    procesar(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fotos);

        ordenId = getIntent().getStringExtra(EXTRA_ID);
        if (ordenId == null) {
            finish();
            return;
        }
        store = new LocalStore(this);
        db = AppDatabase.getInstance(this);

        SyncBarView barra = findViewById(R.id.syncBar);
        barra.enlazar(this);
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        tvCantidad = findViewById(R.id.tvCantidad);
        tvVacia = findViewById(R.id.tvVacia);
        TextView tvOrden = findViewById(R.id.tvOrden);

        RecyclerView rv = findViewById(R.id.rvFotos);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new FotosAdapter(this, new FotosAdapter.Listener() {
            @Override
            public void alVer(FotoItem foto) {
                verFoto(foto);
            }

            @Override
            public void alEliminar(FotoItem foto) {
                confirmarEliminar(foto);
            }
        });
        rv.setAdapter(adapter);
        if (Sesion.esCliente(this)) {
            adapter.setEditable(false);
            findViewById(R.id.btnTomar).setVisibility(View.GONE);
            findViewById(R.id.btnGaleria).setVisibility(View.GONE);
        }

        db.ordenDao().observarDetalle(ordenId).observe(this, d -> {
            if (d != null) {
                String numero = (d.placa != null && !d.placa.isEmpty())
                        ? d.placa : "#" + d.id.substring(0, 4).toUpperCase();
                tvOrden.setText(getString(R.string.bitacora_orden, numero));
            }
        });

        db.registroDao().observarFotos(ordenId).observe(this, lista -> {
            int n = lista == null ? 0 : lista.size();
            adapter.submitList(lista);
            tvCantidad.setText(getResources().getQuantityString(R.plurals.fotos_cantidad, n, n));
            tvVacia.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        });

        findViewById(R.id.btnTomar).setOnClickListener(v -> abrirCamara());
        findViewById(R.id.btnGaleria).setOnClickListener(v -> elegirGaleria.launch("image/*"));
    }

    private void abrirCamara() {
        try {
            File carpeta = new File(getCacheDir(), "fotos");
            if (!carpeta.exists() && !carpeta.mkdirs()) {
                throw new IOException("No se pudo crear la carpeta");
            }
            File archivo = File.createTempFile("cam_", ".jpg", carpeta);
            uriCamara = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivo);
            tomarFoto.launch(uriCamara);
        } catch (IOException | IllegalArgumentException e) {
            Toast.makeText(this, R.string.fotos_error, Toast.LENGTH_LONG).show();
        }
    }

    // Reduce y comprime en segundo plano; luego pide la descripción
    private void procesar(Uri uri) {
        Toast.makeText(this, R.string.fotos_procesando, Toast.LENGTH_SHORT).show();
        procesador.execute(() -> {
            try {
                String base64 = ImagenUtil.comprimirABase64(this, uri);
                runOnUiThread(() -> pedirDescripcion(base64));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.fotos_error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void pedirDescripcion(String base64) {
        EditText et = new EditText(this);
        et.setHint(R.string.fotos_desc_hint);
        et.setSingleLine(true);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout contenedor = new FrameLayout(this);
        contenedor.setPadding(pad, pad / 2, pad, 0);
        contenedor.addView(et);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fotos_desc_titulo)
                .setView(contenedor)
                .setCancelable(false)
                .setPositiveButton(R.string.fotos_guardar, (d, w) -> guardar(base64, et.getText().toString().trim()))
                .setNegativeButton(R.string.fotos_omitir, (d, w) -> guardar(base64, null))
                .show();
    }

    private void guardar(String base64, String descripcion) {
        RegistroEntity r = new RegistroEntity();
        r.id = LocalStore.nuevoId();
        r.ordenId = ordenId;
        r.tipo = "FOTO";
        r.datos = base64;
        r.texto = (descripcion == null || descripcion.isEmpty()) ? null : descripcion;
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

    private void verFoto(FotoItem foto) {
        procesador.execute(() -> {
            RegistroEntity r = db.registroDao().getById(foto.id);
            Bitmap b = (r == null) ? null : ImagenUtil.decodificar(r.datos, 1600);
            if (b == null) {
                return;
            }
            runOnUiThread(() -> {
                ImageView iv = new ImageView(this);
                iv.setAdjustViewBounds(true);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iv.setImageBitmap(b);
                new MaterialAlertDialogBuilder(this)
                        .setView(iv)
                        .setPositiveButton(R.string.fotos_cerrar, null)
                        .show();
            });
        });
    }

    private void confirmarEliminar(FotoItem foto) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.fotos_eliminar_titulo)
                .setMessage(R.string.fotos_eliminar_msg)
                .setNegativeButton(R.string.fotos_cancelar, null)
                .setPositiveButton(R.string.fotos_eliminar, (d, w) ->
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            store.borrarRegistro(foto.id);
                            runOnUiThread(() -> SyncManager.getInstance(this).sincronizar());
                        }))
                .show();
    }
}