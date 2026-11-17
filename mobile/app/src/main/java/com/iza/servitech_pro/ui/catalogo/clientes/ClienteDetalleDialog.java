package com.iza.servitech_pro.ui.catalogo.clientes; // ajusta a tu paquete

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.entity.ClienteEntity;

public class ClienteDetalleDialog extends BottomSheetDialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_NOMBRE = "nombre";
    private static final String ARG_TELEFONO = "telefono";
    private static final String ARG_CORREO = "correo";
    private static final String ARG_DIRECCION = "direccion";
    private static final String ARG_USUARIO_ID = "usuarioId";
    private static final String ARG_SYNCED = "synced";

    public static ClienteDetalleDialog newInstance(@NonNull ClienteEntity c) {
        ClienteDetalleDialog d = new ClienteDetalleDialog();
        Bundle b = new Bundle();
        b.putString(ARG_ID, c.id);
        b.putString(ARG_NOMBRE, c.nombre);
        b.putString(ARG_TELEFONO, c.telefono);
        b.putString(ARG_CORREO, c.correo);
        b.putString(ARG_DIRECCION, c.direccion);
        if (c.usuarioId != null) b.putLong(ARG_USUARIO_ID, c.usuarioId);
        b.putBoolean(ARG_SYNCED, c.isSynced);
        d.setArguments(b);
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_cliente_detalle, container, false);
        Bundle args = requireArguments();

        String nombre = args.getString(ARG_NOMBRE);
        String telefono = args.getString(ARG_TELEFONO);
        String correo = args.getString(ARG_CORREO);
        String direccion = args.getString(ARG_DIRECCION);

        ((TextView) view.findViewById(R.id.tvNombre)).setText(nombre);
        ((TextView) view.findViewById(R.id.tvEstadoSync)).setText(
                args.getBoolean(ARG_SYNCED) ? "Sincronizado" : "Pendiente de sincronizar");

        // Muestra cada fila solo si tiene dato
        mostrarFila(view, R.id.filaTelefono, R.id.tvTelefono, telefono);
        mostrarFila(view, R.id.filaCorreo, R.id.tvCorreo, correo);
        mostrarFila(view, R.id.filaDireccion, R.id.tvDireccion, direccion);

        // Atajos
        View btnLlamar = view.findViewById(R.id.btnLlamar);
        View btnCorreo = view.findViewById(R.id.btnCorreo);
        btnLlamar.setVisibility(TextUtils.isEmpty(telefono) ? View.GONE : View.VISIBLE);
        btnCorreo.setVisibility(TextUtils.isEmpty(correo) ? View.GONE : View.VISIBLE);

        btnLlamar.setOnClickListener(v ->
                abrir(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + telefono))));
        btnCorreo.setOnClickListener(v ->
                abrir(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + correo))));

        view.findViewById(R.id.btnCerrar).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.btnEditar).setOnClickListener(v -> {
            ClienteEntity c = new ClienteEntity();
            c.id = args.getString(ARG_ID);
            c.nombre = nombre;
            c.telefono = telefono;
            c.correo = correo;
            c.direccion = direccion;
            if (args.containsKey(ARG_USUARIO_ID)) c.usuarioId = args.getLong(ARG_USUARIO_ID);

            FragmentManager fm = getParentFragmentManager(); // guardarlo antes de cerrar
            dismiss();
            ClienteFormDialog.newInstance(c).show(fm, "cliente_form");
        });

        return view;
    }

    private void mostrarFila(View root, int idFila, int idTexto, String valor) {
        View fila = root.findViewById(idFila);
        if (TextUtils.isEmpty(valor)) {
            fila.setVisibility(View.GONE);
        } else {
            fila.setVisibility(View.VISIBLE);
            ((TextView) root.findViewById(idTexto)).setText(valor);
        }
    }

    private void abrir(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "No hay una app disponible", Toast.LENGTH_SHORT).show();
        }
    }
}
