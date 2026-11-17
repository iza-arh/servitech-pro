package com.iza.servitech_pro.ui.catalogo.clientes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.entity.ClienteEntity;

public class ClientesAdapter extends ListAdapter<ClienteEntity, ClientesAdapter.ClienteViewHolder> {

    public interface OnClienteListener {

        void onVer(ClienteEntity cliente);
        void onEditar(ClienteEntity cliente);
        void onEliminar(ClienteEntity cliente);
    }

    private final OnClienteListener listener;

    public ClientesAdapter(OnClienteListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ClienteEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ClienteEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull ClienteEntity a, @NonNull ClienteEntity b) {
                    return a.id.equals(b.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull ClienteEntity a, @NonNull ClienteEntity b) {
                    return a.localTs == b.localTs
                            && safeEquals(a.nombre, b.nombre)
                            && safeEquals(a.telefono, b.telefono)
                            && safeEquals(a.correo, b.correo);
                }

                private boolean safeEquals(String x, String y) {
                    return x == null ? y == null : x.equals(y);
                }
            };

    @NonNull
    @Override
    public ClienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cliente, parent, false);
        return new ClienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClienteViewHolder holder, int position) {
        ClienteEntity c = getItem(position);

        holder.tvNombre.setText(c.nombre);
        holder.tvTelefono.setText(c.telefono);
        holder.tvCorreo.setText(c.correo);

        holder.itemView.setOnClickListener(v -> listener.onVer(c));
        holder.btnEditar.setOnClickListener(v -> listener.onEditar(c));
        holder.btnEliminar.setOnClickListener(v -> listener.onEliminar(c));
    }

    static class ClienteViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvTelefono, tvCorreo;
        ImageButton btnEditar, btnEliminar;

        ClienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvTelefono = itemView.findViewById(R.id.tvTelefono);
            tvCorreo = itemView.findViewById(R.id.tvCorreo);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
