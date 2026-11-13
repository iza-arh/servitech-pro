package com.iza.servitech_pro.ui.ordenes;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.FechaUtil;
import com.iza.servitech_pro.data.local.model.OrdenResumen;

import java.util.Objects;
import com.iza.servitech_pro.ui.common.Sesion;
import com.iza.servitech_pro.ui.common.TecnicosCache;

public class OrdenesAdapter extends ListAdapter<OrdenResumen, OrdenesAdapter.VH> {

    private static final DiffUtil.ItemCallback<OrdenResumen> DIFF = new DiffUtil.ItemCallback<OrdenResumen>() {
        @Override
        public boolean areItemsTheSame(@NonNull OrdenResumen a, @NonNull OrdenResumen b) {
            return a.id.equals(b.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull OrdenResumen a, @NonNull OrdenResumen b) {
            return Objects.equals(a.estado, b.estado)
                    && Objects.equals(a.placa, b.placa)
                    && Objects.equals(a.modelo, b.modelo)
                    && Objects.equals(a.descripcion, b.descripcion)
                    && Objects.equals(a.updatedAt, b.updatedAt)
                    && Objects.equals(a.clienteNombre, b.clienteNombre)
                    && Objects.equals(a.servicioNombre, b.servicioNombre)
                    && a.localTs == b.localTs
                    && a.isSynced == b.isSynced
                    && Objects.equals(a.tecnicoId, b.tecnicoId);
        }
    };

    public interface OnOrdenClick {
        void alTocar(OrdenResumen orden);
    }

    private OnOrdenClick listener;

    public void setOnOrdenClick(OnOrdenClick l) {
        this.listener = l;
    }

    public OrdenesAdapter() {
        super(DIFF);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvCliente, tvServicio, tvFecha, tvEstado;
        final ImageView icPendiente;
        final TextView tvTecnico;

        VH(View v) {
            super(v);
            tvCliente = v.findViewById(R.id.tvCliente);
            tvServicio = v.findViewById(R.id.tvServicio);
            tvFecha = v.findViewById(R.id.tvFecha);
            tvEstado = v.findViewById(R.id.tvEstado);
            icPendiente = v.findViewById(R.id.icPendienteSync);
            tvTecnico = v.findViewById(R.id.tvTecnico);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_orden, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        OrdenResumen o = getItem(position);
        Context c = h.itemView.getContext();

        h.tvCliente.setText(o.clienteNombre != null ? o.clienteNombre : c.getString(R.string.orden_sin_cliente));

        String servicio = o.servicioNombre != null ? o.servicioNombre
                : (o.descripcion != null ? o.descripcion : "");
        String vehiculo = (o.modelo != null && !o.modelo.isEmpty()) ? o.modelo
                : (o.placa != null ? o.placa : "");
        String linea = servicio.isEmpty() ? vehiculo
                : (vehiculo.isEmpty() ? servicio : servicio + " / " + vehiculo);
        h.tvServicio.setText(linea);

        h.tvFecha.setText(FechaUtil.mostrar(o.updatedAt, o.localTs));

        int fondo, texto, etiqueta;
        switch (o.estado == null ? "" : o.estado) {
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
        h.tvEstado.setText(etiqueta);
        h.tvEstado.setTextColor(ContextCompat.getColor(c, texto));
        h.tvEstado.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, fondo)));

        h.icPendiente.setVisibility(o.isSynced ? View.GONE : View.VISIBLE);

        if (Sesion.esAdmin(c)) {
            String tec = TecnicosCache.nombre(c, o.tecnicoId);
            if (o.tecnicoId == null) {
                h.tvTecnico.setText(R.string.card_sin_tecnico);
            } else {
                h.tvTecnico.setText(c.getString(R.string.card_tecnico, tec != null ? tec : "#" + o.tecnicoId));
            }
            h.tvTecnico.setVisibility(View.VISIBLE);
        } else {
            h.tvTecnico.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.alTocar(o);
            }
        });
    }
}