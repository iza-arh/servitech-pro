package com.iza.servitech_pro.ui.catalogo;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.entity.CatalogoEntity;

public class CatalogoDetalleDialog extends BottomSheetDialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_TIPO = "tipo";
    private static final String ARG_NOMBRE = "nombre";
    private static final String ARG_CODIGO = "codigo";
    private static final String ARG_PRECIO = "precio";
    private static final String ARG_DESCRIPCION = "descripcion";
    private static final String ARG_SYNCED = "synced";

    public static CatalogoDetalleDialog newInstance(@NonNull CatalogoEntity c) {
        CatalogoDetalleDialog d = new CatalogoDetalleDialog();
        Bundle b = new Bundle();
        b.putString(ARG_ID, c.id);
        b.putString(ARG_TIPO, c.tipo);
        b.putString(ARG_NOMBRE, c.nombre);
        b.putString(ARG_CODIGO, c.codigo);
        b.putString(ARG_DESCRIPCION, c.descripcion);
        if (c.precio != null) b.putDouble(ARG_PRECIO, c.precio);              // ajustar si precio no es Double
        b.putBoolean(ARG_SYNCED, c.isSynced);
        d.setArguments(b);
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_catalogo_detalle, container, false);
        CatalogoViewModel vm = new ViewModelProvider(requireActivity()).get(CatalogoViewModel.class);
        Bundle args = requireArguments();

        String id = args.getString(ARG_ID);
        String tipo = args.getString(ARG_TIPO);
        String nombre = args.getString(ARG_NOMBRE);
        String codigo = args.getString(ARG_CODIGO);
        String descripcion = args.getString(ARG_DESCRIPCION);
        boolean tienePrecio = args.containsKey(ARG_PRECIO);

        ((TextView) view.findViewById(R.id.tvNombre)).setText(nombre);
        ((TextView) view.findViewById(R.id.tvTipo)).setText(etiquetaTipo(tipo));
        ((TextView) view.findViewById(R.id.tvEstadoSync)).setText(
                args.getBoolean(ARG_SYNCED) ? "Sincronizado" : "Pendiente de sincronizar");

        mostrarFila(view, R.id.filaCodigo, R.id.tvCodigo, codigo);
        mostrarFila(view, R.id.filaDescripcion, R.id.tvDescripcion, descripcion);
        mostrarFila(view, R.id.filaPrecio, R.id.tvPrecio,
                tienePrecio ? String.format(java.util.Locale.getDefault(), "%.2f", args.getDouble(ARG_PRECIO)) : null);

        view.findViewById(R.id.btnCerrar).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.btnEditar).setOnClickListener(v -> {
            CatalogoEntity c = new CatalogoEntity();
            c.id = id;
            c.tipo = tipo;
            c.nombre = nombre;
            c.codigo = codigo;
            c.descripcion = descripcion;
            if (tienePrecio) c.precio = args.getDouble(ARG_PRECIO);           // ajustar si precio no es Double

            FragmentManager fm = getParentFragmentManager(); // guardarlo antes de cerrar
            dismiss();
            CatalogoFormDialog.newInstance(tipo, c).show(fm, "catalogo_form");
        });

        view.findViewById(R.id.btnEliminar).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Eliminar")
                        .setMessage("¿Eliminar \"" + nombre + "\"?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Eliminar", (d, w) -> {
                            CatalogoEntity c = new CatalogoEntity();
                            c.id = id;
                            vm.eliminarItem(c);   // relee la fila por id y marca deletedAt
                            dismiss();
                        })
                        .show());

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

    private String etiquetaTipo(String tipo) {
        if ("TIPO_SERVICIO".equals(tipo)) return "Servicio";
        if ("CATEGORIA_EQUIPO".equals(tipo)) return "Equipo";
        return "Repuesto";
    }
}