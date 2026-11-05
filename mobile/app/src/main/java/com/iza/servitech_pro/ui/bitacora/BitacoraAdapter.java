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

    private static final DiffUtil.ItemCallback<RegistroEntity> DIFF = new DiffUtil.ItemCallback<RegistroEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull RegistroEntity a, @NonNull RegistroEntity b) {
            return a.id.equals(b.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull RegistroEntity a, @NonNull RegistroEntity b) {
            return Objects.equals(a.texto, b.texto)
                    && Objects.equals(a.updatedAt, b.updatedAt)
                    && a.localTs == b.localTs
                    && a.isSynced == b.isSynced;
        }
    };

    public BitacoraAdapter() {
        super(DIFF);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvHora, tvTexto;
        final ImageView icPendiente;

        VH(View v) {
            super(v);
            tvHora = v.findViewById(R.id.tvHora);
            tvTexto = v.findViewById(R.id.tvTexto);
            icPendiente = v.findViewById(R.id.icPendiente);
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
        h.tvHora.setText(FechaUtil.horaYFecha(FechaUtil.aMillis(r.updatedAt, r.localTs)));
        h.tvTexto.setText(r.texto);
        h.icPendiente.setVisibility(r.isSynced ? View.GONE : View.VISIBLE);
    }
}