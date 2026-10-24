package com.iza.servitech_pro.ui.ordenes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.dao.OrdenTrabajoDao;
import com.iza.servitech_pro.data.local.entity.OrdenTrabajo;
import com.iza.servitech_pro.data.repository.OrdenRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetalleOrdenFragment extends Fragment {

    private static final String ARG_ORDEN_ID = "orden_id";

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private String ordenId;
    private OrdenTrabajoDao ordenDao;
    private OrdenRepository repository;

    public DetalleOrdenFragment() {
        // Constructor requerido.
    }

    public static DetalleOrdenFragment newInstance(String ordenId) {
        DetalleOrdenFragment fragment = new DetalleOrdenFragment();

        Bundle args = new Bundle();
        args.putString(ARG_ORDEN_ID, ordenId);
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_detalle_orden,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();

        if (args == null || args.getString(ARG_ORDEN_ID) == null) {
            Toast.makeText(
                    requireContext(),
                    "No se especificó la orden",
                    Toast.LENGTH_SHORT
            ).show();

            getParentFragmentManager().popBackStack();
            return;
        }

        ordenId = args.getString(ARG_ORDEN_ID);

        ordenDao = AppDatabase.getInstance(requireContext())
                .ordenTrabajoDao();

        repository = new OrdenRepository(ordenDao);

        Button btnEditar = view.findViewById(R.id.btnEditarOrden);
        Button btnVolver = view.findViewById(R.id.btnVolverDetalle);
        Button btnEliminar = view.findViewById(R.id.btnEliminarOrden);

        btnEditar.setOnClickListener(v ->
                ((OrdenesActivity) requireActivity())
                        .mostrarFormulario(ordenId)
        );

        btnVolver.setOnClickListener(v ->
                getParentFragmentManager().popBackStack()
        );

        btnEliminar.setOnClickListener(v -> confirmarEliminacion());

        cargarDetalle(view);
    }

    private void cargarDetalle(View view) {
        executor.execute(() -> {
            OrdenTrabajo orden = ordenDao.obtenerPorId(ordenId);

            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }

                if (orden == null || orden.eliminado) {
                    Toast.makeText(
                            requireContext(),
                            "No se encontró la orden",
                            Toast.LENGTH_SHORT
                    ).show();

                    getParentFragmentManager().popBackStack();
                    return;
                }

                ((TextView) view.findViewById(R.id.tvDetalleVehiculo))
                        .setText(orden.marca + " " + orden.modelo);

                ((TextView) view.findViewById(R.id.tvDetallePlaca))
                        .setText("Placa: " + orden.placa);

                ((TextView) view.findViewById(R.id.tvDetalleDescripcion))
                        .setText("Descripción: " + orden.descripcion);

                ((TextView) view.findViewById(R.id.tvDetalleEstado))
                        .setText("Estado: " + orden.estado);
            });
        });
    }

    private void confirmarEliminacion() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar orden")
                .setMessage(
                        "¿Deseas eliminar esta orden? " +
                                "La eliminación se sincronizará con Firebase " +
                                "cuando haya conexión."
                )
                .setNegativeButton("Cancelar", (dialog, which) ->
                        dialog.dismiss()
                )
                .setPositiveButton("Eliminar", (dialog, which) ->
                        eliminarOrden()
                )
                .show();
    }

    private void eliminarOrden() {
        executor.execute(() -> {
            OrdenTrabajo orden = ordenDao.obtenerPorId(ordenId);

            if (orden == null || orden.eliminado) {
                mostrarMensajeYVolver("La orden ya no existe");
                return;
            }

            // Registrar primero la eliminación en Room.
            orden.eliminado = true;
            orden.pendienteSync = true;
            ordenDao.actualizar(orden);

            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(
                        requireContext(),
                        "Orden eliminada localmente. Se sincronizará cuando sea posible.",
                        Toast.LENGTH_LONG
                ).show();

                getParentFragmentManager().popBackStack();

                // Enviar la eliminación pendiente a Firebase.
                repository.sincronizarPendientes();
            });
        });
    }

    private void mostrarMensajeYVolver(String mensaje) {
        if (!isAdded()) {
            return;
        }

        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) {
                return;
            }

            Toast.makeText(
                    requireContext(),
                    mensaje,
                    Toast.LENGTH_SHORT
            ).show();

            getParentFragmentManager().popBackStack();
        });
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}


