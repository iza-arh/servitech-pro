package com.iza.servitech_pro.ui.firma;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.entity.OrdenEntity;
import com.iza.servitech_pro.data.local.entity.RegistroEntity;
import com.iza.servitech_pro.data.local.entity.SyncQueueItem;
import com.iza.servitech_pro.data.local.model.OrdenDetalle;
import com.iza.servitech_pro.data.sync.SyncManager;
import com.iza.servitech_pro.ui.common.SyncBarView;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executors;

import es.dmoral.toasty.Toasty;

public class Firma extends AppCompatActivity {

    public static final String EXTRA_ID = "ordenId";

    // Debe coincidir con el valor que usa tu app/backend para "finalizado"
    private static final String ESTADO_FINALIZADO = "COMPLETADA";

    private static final int ANCHO_MAX = 600;
    private static final int LIMITE_BYTES = 500 * 1024;

    private String ordenId;

    private TextView tvCliente, tvServicio, tvEquipo, tvDetail;
    private SignatureView signatureView;
    private ImageView signatureImage;
    private View hint;
    private MaterialButton btnClear, btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firma);

        SyncManager.getInstance(this).sincronizar();
        SyncBarView barra = findViewById(R.id.syncBar);
        barra.enlazar(this);

        ordenId = getIntent().getStringExtra(EXTRA_ID);
        if (ordenId == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvCliente = findViewById(R.id.text_client_name);
        tvServicio = findViewById(R.id.text_service_detail);
        tvEquipo = findViewById(R.id.text_equipo_detail);
        tvDetail = findViewById(R.id.text_detail);

        AppDatabase.getInstance(this).ordenDao()
                .observarDetalle(ordenId).observe(this, d -> {
                    if (d == null) return;
                    tvCliente.setText(d.clienteNombre != null ? d.clienteNombre : getString(R.string.orden_sin_cliente));
                    tvServicio.setText(d.servicioNombre != null ? d.servicioNombre : getString(R.string.detalle_sin_dato));
                    tvEquipo.setText(d.categoriaNombre != null ? d.categoriaNombre : getString(R.string.detalle_sin_dato));
                    tvDetail.setText(vehiculoTexto(d));
                });

        signatureView = findViewById(R.id.signature_view);
        signatureImage = findViewById(R.id.signature_image);
        hint = findViewById(R.id.text_hint);
        btnClear = findViewById(R.id.btn_clear);
        btnNext = findViewById(R.id.btn_next);

        // Habilita "Confirmar" solo si hay firma, y oculta la marca de agua
        signatureView.setOnSignatureChangedListener(hasSignature -> {
            btnNext.setEnabled(hasSignature);
            hint.setVisibility(hasSignature ? View.GONE : View.VISIBLE);
        });

        btnClear.setOnClickListener(v -> signatureView.clear());
        btnNext.setOnClickListener(v -> guardarFirma());

        cargarFirmaExistente();
    }

    private String vehiculoTexto(OrdenDetalle d) {
        StringBuilder sb = new StringBuilder();
        if (d.marca != null && !d.marca.isEmpty()) sb.append(d.marca).append(' ');
        if (d.modelo != null && !d.modelo.isEmpty()) sb.append(d.modelo).append(' ');
        if (d.anio != null) sb.append(d.anio);
        String texto = sb.toString().trim();
        return texto.isEmpty() ? getString(R.string.detalle_sin_dato) : texto;
    }

    // Si la orden ya tiene firma, se muestra y se bloquea la pantalla
    private void cargarFirmaExistente() {
        Executors.newSingleThreadExecutor().execute(() -> {
            RegistroEntity f = AppDatabase.getInstance(this).registroDao().getFirmaActiva(ordenId);
            if (f == null) return;

            Bitmap bmp = null;
            if (f.datos != null && !f.datos.isEmpty()) {
                try {
                    byte[] bytes = Base64.decode(f.datos, Base64.DEFAULT);
                    bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                } catch (IllegalArgumentException ignorado) {
                    // base64 inválido: se bloquea igual, sin imagen
                }
            }
            final Bitmap imagen = bmp;
            runOnUiThread(() -> mostrarFirmaBloqueada(imagen));
        });
    }

    private void mostrarFirmaBloqueada(Bitmap imagen) {
        signatureView.setVisibility(View.GONE);
        hint.setVisibility(View.GONE);
        signatureImage.setVisibility(View.VISIBLE);
        if (imagen != null) {
            signatureImage.setImageBitmap(imagen);
        }
        btnClear.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);
    }

    private void guardarFirma() {
        Bitmap bmp = signatureView.getSignatureBitmap();
        if (bmp == null) {
            Toasty.warning(this, "Debe firmar primero", Toast.LENGTH_SHORT, true).show();
            return;
        }
        btnNext.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String base64 = aBase64(bmp);
                AppDatabase db = AppDatabase.getInstance(this);

                RegistroEntity r = new RegistroEntity();
                r.id = UUID.randomUUID().toString();
                r.ordenId = ordenId;
                r.tipo = "FIRMA";
                r.datos = base64;
                r.creadoEn = ahoraIso();
                r.isSynced = false;
                r.localTs = System.currentTimeMillis();

                final boolean[] yaFirmada = {false};

                db.runInTransaction(() -> {
                    // Solo se firma una vez por orden
                    if (db.registroDao().getFirmaActiva(ordenId) != null) {
                        yaFirmada[0] = true;
                        return;
                    }

                    // 1) Firma
                    db.registroDao().upsert(r);
                    encolar(db, "REGISTRO", r.id);

                    // 2) Orden -> finalizada
                    OrdenEntity o = db.ordenDao().getById(ordenId);
                    if (o != null && !ESTADO_FINALIZADO.equals(o.estado)) {
                        o.estado = ESTADO_FINALIZADO;
                        o.isSynced = false;
                        o.localTs = System.currentTimeMillis();
                        db.ordenDao().upsertAll(Collections.singletonList(o));
                        encolar(db, "ORDEN", o.id);
                    }
                });

                if (yaFirmada[0]) {
                    runOnUiThread(() -> {
                        Toasty.info(this, "Esta orden ya fue firmada", Toast.LENGTH_SHORT, true).show();
                        finish();
                    });
                    return;
                }

                SyncManager.getInstance(this).sincronizar();

                runOnUiThread(() -> {
                    Toasty.success(this, "Firma guardada", Toast.LENGTH_SHORT, true).show();
                    finish();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                runOnUiThread(() -> {
                    btnNext.setEnabled(true);
                    Toasty.error(this, "No se pudo guardar la firma", Toast.LENGTH_SHORT, true).show();
                });
            }
        });
    }

    // Reduce el tamaño hasta quedar bajo ~500 KB y devuelve base64 sin saltos de línea
    private String aBase64(Bitmap original) {
        Bitmap bmp = original;
        if (bmp.getWidth() > ANCHO_MAX) {
            int alto = Math.round(bmp.getHeight() * (ANCHO_MAX / (float) bmp.getWidth()));
            bmp = Bitmap.createScaledBitmap(bmp, ANCHO_MAX, alto, true);
        }
        byte[] bytes;
        while (true) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            bytes = out.toByteArray();
            if (bytes.length <= LIMITE_BYTES || bmp.getWidth() < 200) break;
            bmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth() * 3 / 4, bmp.getHeight() * 3 / 4, true);
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private void encolar(AppDatabase db, String entidad, String id) {
        db.syncQueueDao().eliminarPorEntidad(entidad, id);

        SyncQueueItem q = new SyncQueueItem();
        q.entidad = entidad;
        q.operacion = "UPSERT";
        q.entidadId = id;
        q.estado = "PENDIENTE";
        db.syncQueueDao().insert(q);
    }

    private String ahoraIso() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }
}