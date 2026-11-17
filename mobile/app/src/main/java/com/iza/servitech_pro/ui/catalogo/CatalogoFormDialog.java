package com.iza.servitech_pro.ui.catalogo;

import android.os.Bundle;
import android.text.TextUtils;
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
import com.iza.servitech_pro.data.local.entity.CatalogoEntity;

public class CatalogoFormDialog extends BottomSheetDialogFragment {

    private static final String ARG_TIPO = "tipo";
    private static final String ARG_ID = "id";
    private static final String ARG_NOMBRE = "nombre";
    private static final String ARG_CODIGO = "codigo";
    private static final String ARG_PRECIO = "precio";
    private static final String ARG_DESCRIPCION = "descripcion";

    /** item = null para crear; con valor para editar. */
    public static CatalogoFormDialog newInstance(@NonNull String tipo, @Nullable CatalogoEntity item) {
        CatalogoFormDialog d = new CatalogoFormDialog();
        Bundle b = new Bundle();
        b.putString(ARG_TIPO, tipo);
        if (item != null) {
            b.putString(ARG_ID, item.id);
            b.putString(ARG_NOMBRE, item.nombre);
            b.putString(ARG_CODIGO, item.codigo);
            b.putString(ARG_DESCRIPCION, item.descripcion);
            if (item.precio != null) b.putDouble(ARG_PRECIO, item.precio);
        }
        d.setArguments(b);
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_catalogo_form, container, false);
        CatalogoViewModel vm = new ViewModelProvider(requireActivity()).get(CatalogoViewModel.class);

        Bundle args = requireArguments();
        String tipo = args.getString(ARG_TIPO);
        boolean editando = args.getString(ARG_ID) != null;

        TextView tvTitulo = view.findViewById(R.id.tvTitulo);
        TextInputLayout tilNombre = view.findViewById(R.id.tilNombre);
        TextInputLayout tilPrecio = view.findViewById(R.id.tilPrecio);
        TextInputEditText etNombre = view.findViewById(R.id.etNombre);
        TextInputEditText etCodigo = view.findViewById(R.id.etCodigo);
        TextInputEditText etPrecio = view.findViewById(R.id.etPrecio);
        TextInputEditText etDescripcion = view.findViewById(R.id.etDescripcion);

        tvTitulo.setText((editando ? "Editar " : "Nuevo ") + nombreTipo(tipo));

        // Campos que aplican según el tipo (ajusta a tus reglas)
        boolean usaPrecio = !"CATEGORIA_EQUIPO".equals(tipo);
        view.findViewById(R.id.tilPrecio).setVisibility(usaPrecio ? View.VISIBLE : View.GONE);

        if (editando) {
            etNombre.setText(args.getString(ARG_NOMBRE));
            etCodigo.setText(args.getString(ARG_CODIGO));
            etDescripcion.setText(args.getString(ARG_DESCRIPCION));
            if (args.containsKey(ARG_PRECIO)) etPrecio.setText(String.valueOf(args.getDouble(ARG_PRECIO)));
        }

        view.findViewById(R.id.btnCancelar).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.btnGuardar).setOnClickListener(v -> {
            String nombre = textoDe(etNombre);
            String precioTxt = textoDe(etPrecio);

            tilNombre.setError(null);
            tilPrecio.setError(null);
            boolean ok = true;

            if (TextUtils.isEmpty(nombre)) {
                tilNombre.setError("El nombre es obligatorio");
                ok = false;
            }
            Double precio = null;
            if (usaPrecio && !TextUtils.isEmpty(precioTxt)) {
                try {
                    precio = Double.parseDouble(precioTxt);
                    if (precio < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    tilPrecio.setError("Precio no válido");
                    ok = false;
                }
            }
            if (!ok) return;

            CatalogoEntity c = new CatalogoEntity();
            if (editando) c.id = args.getString(ARG_ID);
            // Si es nuevo, c.id queda "" y el ViewModel genera el UUID
            c.tipo = tipo;
            c.nombre = nombre;
            c.codigo = textoDe(etCodigo);
            c.descripcion = textoDe(etDescripcion);
            c.precio = precio;

            vm.guardarItem(c);
            dismiss();
        });

        return view;
    }

    private String nombreTipo(String tipo) {
        if (tipo == null) return "ítem";
        switch (tipo) {
            case "TIPO_SERVICIO": return "servicio";
            case "CATEGORIA_EQUIPO": return "equipo";
            case "REPUESTO": return "repuesto";
            default: return "ítem";
        }
    }

    private String textoDe(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
