package com.iza.servitech_pro.ui.bitacora;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.FechaUtil;
import com.iza.servitech_pro.data.local.entity.RegistroEntity;

import java.util.Objects;

public class BitacoraAdapter extends ListAdapter<RegistroEntity, BitacoraAdapter.VH> {

    public interface Listener {
        void alEditar(RegistroEntity nota);

        void alEliminar(RegistroEntity nota);
    }

    private static final DiffUtil.ItemCallback<RegistroEntity> DIFF = new DiffUtil.ItemCallback<RegistroEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull RegistroEntity a, @NonNull RegistroEntity b) {
            return a.id.equals(b.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull RegistroEntity a, @NonNull RegistroEntity b) {
            return Objects.equals(a.texto, b.texto)
                    && Objects.equals(a.updatedAt, b.updatedAt)
                    && Objects.equals(a.creadoEn, b.creadoEn)
                    && a.localTs == b.localTs
                    && a.isSynced == b.isSynced;
        }
    };

    private Listener listener;
    private boolean editable = true;

    public BitacoraAdapter() {
        super(DIFF);
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    // false = orden completada: se ocultan editar y eliminar
    public void setEditable(boolean e) {
        if (editable != e) {
            editable = e;
            notifyDataSetChanged();
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvHora, tvTexto;
        final ImageView icPendiente, btnEditar, btnEliminar;

        VH(View v) {
            super(v);
            tvHora = v.findViewById(R.id.tvHora);
            tvTexto = v.findViewById(R.id.tvTexto);
            icPendiente = v.findViewById(R.id.icPendiente);
            btnEditar = v.findViewById(R.id.btnEditar);
            btnEliminar = v.findViewById(R.id.btnEliminar);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bitacora, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        RegistroEntity r = getItem(position);
        String base = r.creadoEn != null ? r.creadoEn : r.updatedAt;
        h.tvHora.setText(FechaUtil.horaYFecha(FechaUtil.aMillis(base, r.localTs)));
        h.tvTexto.setText(r.texto);
        h.icPendiente.setVisibility(r.isSynced ? View.GONE : View.VISIBLE);

        int acciones = editable ? View.VISIBLE : View.GONE;
        h.btnEditar.setVisibility(acciones);
        h.btnEliminar.setVisibility(acciones);
        h.btnEditar.setOnClickListener(v -> {
            if (listener != null) listener.alEditar(r);
        });
        h.btnEliminar.setOnClickListener(v -> {
            if (listener != null) listener.alEliminar(r);
        });
    }
}