package com.iza.servitech_pro.ui.ordenes;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.dao.OrdenTrabajoDao;
import com.iza.servitech_pro.data.local.entity.OrdenTrabajo;
import com.iza.servitech_pro.data.repository.OrdenRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListadoOrdenesFragment extends Fragment {

    private RecyclerView rvOrdenes;
    private TextView tvSinOrdenes;
    private TextView tvEstadoSync;
    private MaterialButton btnOrdenSync;

    private OrdenTrabajoAdapter adapter;
    private OrdenTrabajoDao ordenDao;
    private OrdenRepository repository;

    private MaterialButton btnNuevaOrden;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public ListadoOrdenesFragment() {
        // Constructor requerido para Fragment.
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_listado_ordenes,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvOrdenes = view.findViewById(R.id.rvOrdenes);
        tvSinOrdenes = view.findViewById(R.id.tvSinOrdenes);
        tvEstadoSync = view.findViewById(R.id.tvEstadoSync);
        btnOrdenSync = view.findViewById(R.id.btnOrdenSync);
        btnNuevaOrden = view.findViewById(R.id.btnNuevaOrden);

        rvOrdenes.setLayoutManager(
                new LinearLayoutManager(requireContext()));

        adapter = new OrdenTrabajoAdapter(
                orden -> ((OrdenesActivity) requireActivity())
                        .mostrarDetalle(orden.id)
        );

        rvOrdenes.setAdapter(adapter);

        ordenDao = AppDatabase.getInstance(requireContext())
                .ordenTrabajoDao();

        repository = new OrdenRepository(ordenDao);

        btnOrdenSync.setOnClickListener(v -> sincronizar());

        btnNuevaOrden.setOnClickListener(v -> {
            ((OrdenesActivity) requireActivity())
                    .mostrarFormulario(null);
        });

        cargarOrdenes();
        mostrarEstado(
                "Pulsa para comprobar la sincronización",
                Color.DKGRAY,
                "Sincronizar",
                Color.rgb(37, 99, 235),
                true
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        cargarOrdenes();

        // Recuperar datos remotos y enviar pendientes al volver al listado.
        if (repository != null) {
            sincronizar();
        }
    }

    private void sincronizar() {
        if (repository == null || !isAdded() || getView() == null) {
            return;
        }

        mostrarEstado(
                "Sincronizando órdenes...",
                Color.rgb(180, 83, 9),
                "Sincronizando...",
                Color.rgb(180, 83, 9),
                false
        );

        repository.sincronizarPendientes((correcto, pendientes) -> {
            if (!isAdded() || getView() == null) {
                return;
            }

            cargarOrdenes();

            if (!correcto) {
                mostrarEstado(
                        "Error de sincronización. Revisa la conexión e inténtalo de nuevo.",
                        Color.rgb(185, 28, 28),
                        "Reintentar",
                        Color.rgb(185, 28, 28),
                        true
                );
            } else if (pendientes > 0) {
                mostrarEstado(
                        pendientes + " orden(es) pendiente(s) de sincronizar",
                        Color.rgb(180, 83, 9),
                        "Reintentar",
                        Color.rgb(180, 83, 9),
                        true
                );
            } else {
                mostrarEstado(
                        "Órdenes sincronizadas con Firebase",
                        Color.rgb(21, 128, 61),
                        "Sincronizado",
                        Color.rgb(21, 128, 61),
                        true
                );
            }
        });
    }

    private void mostrarEstado(
            String mensaje,
            int colorTexto,
            String textoBoton,
            int colorBoton,
            boolean habilitado) {

        if (tvEstadoSync == null || btnOrdenSync == null) {
            return;
        }

        tvEstadoSync.setText(mensaje);
        tvEstadoSync.setTextColor(colorTexto);

        btnOrdenSync.setText(textoBoton);
        btnOrdenSync.setBackgroundTintList(
                ColorStateList.valueOf(colorBoton));
        btnOrdenSync.setEnabled(habilitado);
    }

    private void cargarOrdenes() {
        if (ordenDao == null || !isAdded() || getView() == null) {
            return;
        }

        executor.execute(() -> {
            List<OrdenTrabajo> ordenes = ordenDao.obtenerTodas();

            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || getView() == null || adapter == null) {
                    return;
                }

                adapter.setOrdenes(ordenes);

                boolean vacio = ordenes.isEmpty();

                tvSinOrdenes.setVisibility(
                        vacio ? View.VISIBLE : View.GONE);
                rvOrdenes.setVisibility(
                        vacio ? View.GONE : View.VISIBLE);
            });
        });
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}

