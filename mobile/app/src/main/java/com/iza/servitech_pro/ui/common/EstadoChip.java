package com.iza.servitech_pro.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.iza.servitech_pro.R;

// Pinta un TextView como "chip" de estado (color según el estado de la orden)
public class EstadoChip {

    public static void aplicar(TextView tv, String estado) {
        Context c = tv.getContext();
        int fondo, texto, etiqueta;
        switch (estado == null ? "" : estado) {
            case "EN_PROCESO":
                fondo = R.color.sv_status_progress;
                texto = R.color.sv_on_chip_dark;
                etiqueta = R.string.estado_en_proceso;
                break;
            case "COMPLETADA":
                fondo = R.color.sv_status_completed;
                texto = R.color.sv_on_chip_dark;
                etiqueta = R.string.estado_completada;
                break;
            default:
                fondo = R.color.sv_status_pending;
                texto = R.color.sv_on_primary;
                etiqueta = R.string.estado_pendiente;
                break;
        }
        tv.setText(etiqueta);
        tv.setTextColor(ContextCompat.getColor(c, texto));
        tv.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, fondo)));
    }
}