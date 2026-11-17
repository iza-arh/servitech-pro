package com.iza.servitech_pro.ui.catalogo;

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
import com.iza.servitech_pro.data.local.entity.CatalogoEntity;

import java.util.Locale;
import java.util.Objects;

public class CatalogoItemsAdapter
        extends ListAdapter<CatalogoEntity, CatalogoItemsAdapter.ItemViewHolder> {

    public interface OnItemListener {
        void onVer(CatalogoEntity item);
        void onEditar(CatalogoEntity item);
        void onEliminar(CatalogoEntity item);
    }

    private final String tipo;
    private final OnItemListener listener; // puede ser null

    public CatalogoItemsAdapter(String tipo, OnItemListener listener) {
        super(DIFF_CALLBACK);
        this.tipo = tipo;
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<CatalogoEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CatalogoEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull CatalogoEntity a, @NonNull CatalogoEntity b) {
                    return a.id.equals(b.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull CatalogoEntity a, @NonNull CatalogoEntity b) {
                    return a.localTs == b.localTs
                            && Objects.equals(a.nombre, b.nombre)
                            && Objects.equals(a.descripcion, b.descripcion)
                            && Objects.equals(a.codigo, b.codigo)
                            && Objects.equals(a.precio, b.precio);
                }
            };

    private int layoutParaTipo() {
        if ("TIPO_SERVICIO".equals(tipo)) return R.layout.item_servicio;
        if ("CATEGORIA_EQUIPO".equals(tipo)) return R.layout.item_equipo;
        return R.layout.item_repuesto;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutParaTipo(), parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder h, int position) {
        CatalogoEntity c = getItem(position);

        h.tvNombre.setText(c.nombre);

        if (h.tvDescripcion != null) h.tvDescripcion.setText(c.descripcion);

        if (h.tvCodigo != null) {
            h.tvCodigo.setText(c.codigo != null ? "Código: " + c.codigo : "");
        }

        if (h.tvPrecio != null) {
            h.tvPrecio.setText(c.precio != null
                    ? String.format(Locale.US, "$%.2f", c.precio) : "");
        }

        h.itemView.setOnClickListener(v -> {
           if (listener != null) listener.onVer(c);
        });

        if (h.btnEditar != null) {
            h.btnEditar.setOnClickListener(v -> {
                if (listener != null) listener.onEditar(c);
            });
        }
        if (h.btnEliminar != null) {
            h.btnEliminar.setOnClickListener(v -> {
                if (listener != null) listener.onEliminar(c);
            });
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDescripcion, tvCodigo, tvPrecio;
        ImageButton btnEditar, btnEliminar;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            // Solo existen en algunos layouts; en los demás quedan en null
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvCodigo = itemView.findViewById(R.id.tvCodigo);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
