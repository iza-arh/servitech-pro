package com.iza.servitech_pro.ui.catalogo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.entity.CatalogoEntity;

public class CatalogoItemsFragment extends Fragment {

    private static final String ARG_TIPO = "tipo";

    public static CatalogoItemsFragment newInstance(String tipo) {
        CatalogoItemsFragment f = new CatalogoItemsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TIPO, tipo);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista, container, false);
        RecyclerView rv = view.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        String tipo = requireArguments().getString(ARG_TIPO);
        CatalogoViewModel vm = new ViewModelProvider(requireActivity()).get(CatalogoViewModel.class);

        CatalogoItemsAdapter adapter = new CatalogoItemsAdapter(tipo, new CatalogoItemsAdapter.OnItemListener() {
            @Override
            public void onVer(CatalogoEntity item) {
                CatalogoDetalleDialog.newInstance(item)
                        .show(getParentFragmentManager(), "catalogo_detalle");
            }

            @Override
            public void onEditar(CatalogoEntity item) {
                CatalogoFormDialog.newInstance(tipo, item)
                        .show(getParentFragmentManager(), "catalogo_form");
            }

            @Override
            public void onEliminar(CatalogoEntity item) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Eliminar")
                        .setMessage("¿Eliminar \"" + item.nombre + "\"?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Eliminar", (d, w) -> vm.eliminarItem(item))
                        .show();
            }
        });
        rv.setAdapter(adapter);

        // Botón para crear (ver nota sobre el FAB)
        FloatingActionButton fab = view.findViewById(R.id.fabAgregar);
        if (fab != null) {
            fab.setOnClickListener(v -> CatalogoFormDialog.newInstance(tipo, null)
                    .show(getParentFragmentManager(), "catalogo_form"));
        }

        vm.observarCatalogo(tipo).observe(getViewLifecycleOwner(), adapter::submitList);
        return view;
    }
}
