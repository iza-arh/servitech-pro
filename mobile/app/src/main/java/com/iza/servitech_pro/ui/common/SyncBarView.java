package com.iza.servitech_pro.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.sync.SyncEstado;
import com.iza.servitech_pro.data.sync.SyncManager;

// Barra superior: estado de sincronización (izquierda) y de conexión (derecha)
public class SyncBarView extends LinearLayout {

    private final ImageView icSync;
    private final TextView tvSync;
    private final TextView tvRed;
    private final View dotRed;

    private SyncEstado estado = SyncEstado.EN_LINEA;
    private int pendientes = 0;
    private boolean girando = false;

    public SyncBarView(Context context) {
        this(context, null);
    }

    public SyncBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_sync_bar, this, true);
        icSync = findViewById(R.id.icSync);
        tvSync = findViewById(R.id.tvSync);
        tvRed = findViewById(R.id.tvRed);
        dotRed = findViewById(R.id.dotRed);
        actualizar();
    }

    // Conecta la barra con el motor de sincronización y la cola local
    public void enlazar(LifecycleOwner owner) {
        SyncManager.getInstance(getContext()).estado().observe(owner, e -> {
            if (e != null) {
                estado = e;
                actualizar();
            }
        });
        AppDatabase.getInstance(getContext()).syncQueueDao().observarCantidadPendientes()
                .observe(owner, n -> {
                    pendientes = n == null ? 0 : n;
                    actualizar();
                });
    }

    private int color(int res) {
        return ContextCompat.getColor(getContext(), res);
    }

    private void actualizar() {
        // Izquierda: sincronización
        String texto;
        int colorTexto = color(R.color.sv_text_secondary);
        switch (estado) {
            case SINCRONIZANDO:
                texto = getContext().getString(R.string.sync_sincronizando);
                break;
            case SESION_EXPIRADA:
                texto = getContext().getString(R.string.sync_sesion_expirada);
                colorTexto = color(R.color.sv_status_pending);
                break;
            case ERROR:
                texto = getContext().getString(R.string.sync_error);
                colorTexto = color(R.color.sv_status_pending);
                break;
            default:
                texto = pendientes > 0
                        ? getContext().getString(R.string.sync_pendientes, pendientes)
                        : getContext().getString(R.string.sync_sincronizado);
                break;
        }
        tvSync.setText(texto);
        tvSync.setTextColor(colorTexto);
        girar(estado == SyncEstado.SINCRONIZANDO);

        // Derecha: conexión
        boolean sinRed = estado == SyncEstado.SIN_CONEXION;
        int colorRed = color(sinRed ? R.color.sv_status_pending : R.color.sv_success);
        dotRed.setBackgroundTintList(ColorStateList.valueOf(colorRed));
        tvRed.setText(sinRed ? R.string.red_sin_conexion : R.string.red_en_linea);
    }

    private void girar(boolean activo) {
        if (activo == girando) {
            return;
        }
        girando = activo;
        if (activo) {
            RotateAnimation giro = new RotateAnimation(0, 360,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            giro.setDuration(1000);
            giro.setRepeatCount(Animation.INFINITE);
            giro.setInterpolator(new LinearInterpolator());
            icSync.startAnimation(giro);
        } else {
            icSync.clearAnimation();
        }
    }
}