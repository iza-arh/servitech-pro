package com.iza.servitech_pro.ui.catalogo.clientes;

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
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.entity.ClienteEntity;
import com.iza.servitech_pro.ui.catalogo.CatalogoViewModel;

public class ClientesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_lista, container, false);
        RecyclerView rv = view.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // ViewModel compartido con la Activity
        CatalogoViewModel vm = new ViewModelProvider(requireActivity())
                .get(CatalogoViewModel.class);

        ClientesAdapter adapter = new ClientesAdapter(new ClientesAdapter.OnClienteListener() {

            @Override
            public void onVer(ClienteEntity cliente) {
                ClienteDetalleDialog.newInstance(cliente)
                        .show(getParentFragmentManager(), "cliente_detalle");
            }

            @Override
            public void onEditar(ClienteEntity cliente) {
                ClienteFormDialog.newInstance(cliente)
                        .show(getParentFragmentManager(), "cliente_form");
            }

            @Override
            public void onEliminar(ClienteEntity cliente) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Eliminar cliente")
                        .setMessage("¿Eliminar a " + cliente.nombre + "?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Eliminar", (d, w) -> vm.eliminarCliente(cliente))
                        .show();
            }
        });

        rv.setAdapter(adapter);

        // Cuando Room cambia (o cambia la búsqueda), la lista se actualiza sola
        vm.clientes.observe(getViewLifecycleOwner(), adapter::submitList);

        return view;
    }
}
