package com.iza.servitech_pro.ui.catalogo.clientes; // ajusta a tu paquete

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.entity.ClienteEntity;
import com.iza.servitech_pro.ui.catalogo.CatalogoViewModel;

public class ClienteFormDialog extends BottomSheetDialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_NOMBRE = "nombre";
    private static final String ARG_TELEFONO = "telefono";
    private static final String ARG_CORREO = "correo";
    private static final String ARG_DIRECCION = "direccion";
    private static final String ARG_USUARIO_ID = "usuarioId";

    /** cliente = null para crear uno nuevo; con valor para editar. */
    public static ClienteFormDialog newInstance(@Nullable ClienteEntity cliente) {
        ClienteFormDialog d = new ClienteFormDialog();
        if (cliente != null) {
            Bundle b = new Bundle();
            b.putString(ARG_ID, cliente.id);
            b.putString(ARG_NOMBRE, cliente.nombre);
            b.putString(ARG_TELEFONO, cliente.telefono);
            b.putString(ARG_CORREO, cliente.correo);
            b.putString(ARG_DIRECCION, cliente.direccion);
            if (cliente.usuarioId != null) b.putLong(ARG_USUARIO_ID, cliente.usuarioId);
            d.setArguments(b);
        }
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_cliente_form, container, false);

        CatalogoViewModel vm = new ViewModelProvider(requireActivity()).get(CatalogoViewModel.class);

        TextView tvTitulo = view.findViewById(R.id.tvTitulo);
        TextInputLayout tilNombre = view.findViewById(R.id.tilNombre);
        TextInputLayout tilCorreo = view.findViewById(R.id.tilCorreo);
        TextInputEditText etNombre = view.findViewById(R.id.etNombre);
        TextInputEditText etTelefono = view.findViewById(R.id.etTelefono);
        TextInputEditText etCorreo = view.findViewById(R.id.etCorreo);
        TextInputEditText etDireccion = view.findViewById(R.id.etDireccion);

        Bundle args = getArguments();
        boolean editando = args != null && args.getString(ARG_ID) != null;

        tvTitulo.setText(editando ? "Editar cliente" : "Nuevo cliente");

        if (editando) {
            etNombre.setText(args.getString(ARG_NOMBRE));
            etTelefono.setText(args.getString(ARG_TELEFONO));
            etCorreo.setText(args.getString(ARG_CORREO));
            etDireccion.setText(args.getString(ARG_DIRECCION));
        }

        view.findViewById(R.id.btnCancelar).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.btnGuardar).setOnClickListener(v -> {
            String nombre = textoDe(etNombre);
            String correo = textoDe(etCorreo);

            // Validación
            tilNombre.setError(null);
            tilCorreo.setError(null);
            boolean ok = true;

            if (TextUtils.isEmpty(nombre)) {
                tilNombre.setError("El nombre es obligatorio");
                ok = false;
            }
            if (!TextUtils.isEmpty(correo)
                    && !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                tilCorreo.setError("Correo no válido");
                ok = false;
            }
            if (!ok) return;

            ClienteEntity c = new ClienteEntity();
            if (editando) {
                c.id = args.getString(ARG_ID);
                if (args.containsKey(ARG_USUARIO_ID)) c.usuarioId = args.getLong(ARG_USUARIO_ID);
            }
            // Si es nuevo, c.id queda "" y el ViewModel genera el UUID.
            // También deberías asignar c.usuarioId desde tu sesión si el servidor lo exige.

            c.nombre = nombre;
            c.telefono = textoDe(etTelefono);
            c.correo = correo;
            c.direccion = textoDe(etDireccion);

            vm.guardarCliente(c);
            dismiss();
        });

        return view;
    }

    private String textoDe(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
