package com.iza.servitech_pro.ui.usuarios;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.remote.dto.UsuarioAdminDto;

import java.util.Objects;

public class UsuariosAdapter extends ListAdapter<UsuarioAdminDto.Usuario, UsuariosAdapter.VH> {

    public interface OnUsuarioClick {
        void alTocar(UsuarioAdminDto.Usuario usuario);
    }

    private static final DiffUtil.ItemCallback<UsuarioAdminDto.Usuario> DIFF =
            new DiffUtil.ItemCallback<UsuarioAdminDto.Usuario>() {
                @Override
                public boolean areItemsTheSame(@NonNull UsuarioAdminDto.Usuario a, @NonNull UsuarioAdminDto.Usuario b) {
                    return Objects.equals(a.id, b.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull UsuarioAdminDto.Usuario a, @NonNull UsuarioAdminDto.Usuario b) {
                    return Objects.equals(a.username, b.username)
                            && Objects.equals(a.nombre, b.nombre)
                            && Objects.equals(a.rol, b.rol);
                }
            };

    private final OnUsuarioClick listener;

    public UsuariosAdapter(OnUsuarioClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvNombre, tvUsuario, tvRol;

        VH(View v) {
            super(v);
            tvNombre = v.findViewById(R.id.tvNombre);
            tvUsuario = v.findViewById(R.id.tvUsuario);
            tvRol = v.findViewById(R.id.tvRol);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usuario, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        UsuarioAdminDto.Usuario u = getItem(position);
        Context c = h.itemView.getContext();

        boolean tieneNombre = u.nombre != null && !u.nombre.trim().isEmpty();
        h.tvNombre.setText(tieneNombre ? u.nombre : u.username);
        h.tvUsuario.setText("@" + u.username);

        int fondo, texto, etiqueta;
        switch (u.rol == null ? "" : u.rol) {
            case "ADMIN":
                fondo = R.color.sv_primary;
                texto = R.color.sv_on_primary;
                etiqueta = R.string.usuarios_rol_admin;
                break;
            case "TECNICO":
                fondo = R.color.sv_secondary;
                texto = R.color.sv_on_primary;
                etiqueta = R.string.usuarios_rol_tecnico;
                break;
            default:
                fondo = R.color.sv_border;
                texto = R.color.sv_text_primary;
                etiqueta = R.string.usuarios_rol_cliente;
                break;
        }
        h.tvRol.setText(etiqueta);
        h.tvRol.setTextColor(ContextCompat.getColor(c, texto));
        h.tvRol.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, fondo)));

        h.itemView.setOnClickListener(v -> listener.alTocar(u));
    }
}