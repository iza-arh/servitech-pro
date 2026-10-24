
package com.iza.servitech_pro.ui.ordenes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.dao.OrdenTrabajoDao;
import com.iza.servitech_pro.data.local.entity.OrdenTrabajo;
import com.iza.servitech_pro.data.repository.OrdenRepository;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FormularioOrdenFragment extends Fragment {

    private static final String ARG_ORDEN_ID = "orden_id";

    private EditText etPlaca;
    private EditText etMarca;
    private EditText etModelo;
    private EditText etDescripcion;
    private AutoCompleteTextView spEstado;
    private TextView tvTitulo;

    private OrdenTrabajoDao ordenDao;
    private String ordenId;
    private OrdenTrabajo ordenExistente;
    private boolean guardando = false;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public FormularioOrdenFragment() {
        // Constructor requerido.
    }

    public static FormularioOrdenFragment newInstance(
            @Nullable String ordenId) {

        FormularioOrdenFragment fragment =
                new FormularioOrdenFragment();

        Bundle args = new Bundle();

        if (ordenId != null) {
            args.putString(ARG_ORDEN_ID, ordenId);
        }

        fragment.setArguments(args);
        return fragment;
    }

    private static final String[] ESTADOS = {
            "Pendiente",
            "En proceso",
            "Completada",
            "Cancelada"
    };

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.activity_formulario_orden,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPlaca = view.findViewById(R.id.etPlaca);
        etMarca = view.findViewById(R.id.etMarca);
        etModelo = view.findViewById(R.id.etModelo);
        etDescripcion = view.findViewById(R.id.etDescripcion);
        spEstado = view.findViewById(R.id.spEstado);
        tvTitulo = view.findViewById(R.id.tvTituloFormulario);

        Button btnGuardar = view.findViewById(R.id.btnGuardarOrden);
        Button btnCancelar = view.findViewById(R.id.btnCancelarOrden);

        ArrayAdapter<String> estadoAdapter = new ArrayAdapter<>(
          requireContext(),
          android.R.layout.simple_dropdown_item_1line,
          ESTADOS
        );

        spEstado.setAdapter(estadoAdapter);
        spEstado.setAdapter(estadoAdapter);

        ordenDao = AppDatabase.getInstance(requireContext())
                .ordenTrabajoDao();

        Bundle args = getArguments();

        if (args != null) {
            ordenId = args.getString(ARG_ORDEN_ID);
        }

        if (ordenId != null) {
            tvTitulo.setText("Editar orden");
            cargarOrden();
        } else {
            tvTitulo.setText("Nueva orden");
        }

        btnGuardar.setOnClickListener(v -> guardarOrden());

        btnCancelar.setOnClickListener(v ->
                getParentFragmentManager().popBackStack()
        );
    }

    private void cargarOrden() {
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

                ordenExistente = orden;

                etPlaca.setText(orden.placa);
                etMarca.setText(orden.marca);
                etModelo.setText(orden.modelo);
                etDescripcion.setText(orden.descripcion);
                spEstado.setText(orden.estado, false);
            });
        });
    }

    private void guardarOrden() {
        if (guardando) {
            return;
        }

        String placa = etPlaca.getText().toString().trim();
        String marca = etMarca.getText().toString().trim();
        String modelo = etModelo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String estado = spEstado.getText().toString().trim();

        if (placa.isEmpty() || marca.isEmpty()
                || modelo.isEmpty() || descripcion.isEmpty() || estado.isEmpty()) {

            Toast.makeText(
                    requireContext(),
                    "Completa todos los campos",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            Toast.makeText(
                    requireContext(),
                    "No hay un usuario autenticado. Inicia sesión nuevamente.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String uid = usuario.getUid();

        // Evita cambiar el creador de una orden existente.
        if (ordenExistente != null
                && !ordenExistente.uidCreador.equals(uid)) {
            Toast.makeText(
                    requireContext(),
                    "No puedes editar una orden de otro usuario",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        guardando = true;

        OrdenTrabajo orden;

        if (ordenExistente == null) {
            orden = new OrdenTrabajo(
                    UUID.randomUUID().toString(),
                    placa,
                    marca,
                    modelo,
                    descripcion,
                    estado,
                    uid,
                    true,
                    false
            );
        } else {
            orden = new OrdenTrabajo(
                    ordenExistente.id,
                    placa,
                    marca,
                    modelo,
                    descripcion,
                    estado,
                    ordenExistente.uidCreador,
                    true,
                    false
            );
        }

        executor.execute(() -> {
            try {
                if (ordenExistente == null) {
                    ordenDao.insertar(orden);
                } else {
                    ordenDao.actualizar(orden);
                }

                //Sincronizar las órdenes pendientes con Firestore.
                OrdenRepository repository = new OrdenRepository(ordenDao);
                repository.sincronizarPendientes();

                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    Toast.makeText(
                            requireContext(),
                            "Orden guardada localmente",
                            Toast.LENGTH_SHORT
                    ).show();

                    getParentFragmentManager().popBackStack();
                });

            } catch (Exception e) {
                android.util.Log.e(
                        "ORDEN_GUARDAR",
                        "Error al guardar la orden en Room",
                        e
                );

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (!isAdded()) {
                            return;
                        }

                        guardando = false;

                        Toast.makeText(
                                requireContext(),
                                "No se pudo guardar la orden",
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}

