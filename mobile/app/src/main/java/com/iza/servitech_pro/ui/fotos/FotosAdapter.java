package com.iza.servitech_pro.ui.fotos;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
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
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.entity.RegistroEntity;
import com.iza.servitech_pro.data.local.model.FotoItem;
import com.iza.servitech_pro.util.ImagenUtil;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FotosAdapter extends ListAdapter<FotoItem, FotosAdapter.VH> {

    public interface Listener {
        void alVer(FotoItem foto);

        void alEliminar(FotoItem foto);
    }

    // Miniaturas en memoria y carga en segundo plano (no se lee la imagen completa en la lista)
    private static final ExecutorService CARGADOR = Executors.newFixedThreadPool(2);
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 1024 / 8)) {
                @Override
                protected int sizeOf(String key, Bitmap b) {
                    return b.getByteCount() / 1024;
                }
            };

    private static final DiffUtil.ItemCallback<FotoItem> DIFF = new DiffUtil.ItemCallback<FotoItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull FotoItem a, @NonNull FotoItem b) {
            return a.id.equals(b.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull FotoItem a, @NonNull FotoItem b) {
            return Objects.equals(a.texto, b.texto)
                    && Objects.equals(a.updatedAt, b.updatedAt)
                    && a.isSynced == b.isSynced;
        }
    };

    private final Context ctx;
    private final Listener listener;

    private boolean editable = true;

    public void setEditable(boolean e) {
        if (editable != e) {
            editable = e;
            notifyDataSetChanged();
        }
    }

    public FotosAdapter(Context ctx, Listener listener) {
        super(DIFF);
        this.ctx = ctx.getApplicationContext();
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView img, btnEliminar, icPendiente;
        final TextView tvDescripcion;

        VH(View v) {
            super(v);
            img = v.findViewById(R.id.img);
            btnEliminar = v.findViewById(R.id.btnEliminar);
            icPendiente = v.findViewById(R.id.icPendiente);
            tvDescripcion = v.findViewById(R.id.tvDescripcion);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_foto, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        FotoItem f = getItem(position);

        boolean hayTexto = f.texto != null && !f.texto.trim().isEmpty();
        h.tvDescripcion.setVisibility(hayTexto ? View.VISIBLE : View.GONE);
        h.tvDescripcion.setText(f.texto);
        h.icPendiente.setVisibility(f.isSynced ? View.GONE : View.VISIBLE);

        h.img.setTag(f.id);
        Bitmap cacheada = CACHE.get(f.id);
        if (cacheada != null) {
            h.img.setImageBitmap(cacheada);
        } else {
            h.img.setImageDrawable(null);
            final String id = f.id;
            CARGADOR.execute(() -> {
                RegistroEntity r = AppDatabase.getInstance(ctx).registroDao().getById(id);
                Bitmap b = (r == null) ? null : ImagenUtil.decodificar(r.datos, 300);
                if (b != null) {
                    CACHE.put(id, b);
                    h.img.post(() -> {
                        if (id.equals(h.img.getTag())) {
                            h.img.setImageBitmap(b);
                        }
                    });
                }
            });
        }

        h.btnEliminar.setVisibility(editable ? View.VISIBLE : View.GONE);
        h.itemView.setOnClickListener(v -> listener.alVer(f));
        h.btnEliminar.setOnClickListener(v -> listener.alEliminar(f));
    }
}