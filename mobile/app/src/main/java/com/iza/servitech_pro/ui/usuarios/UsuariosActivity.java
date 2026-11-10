package com.iza.servitech_pro.ui.usuarios;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.remote.RetrofitClient;
import com.iza.servitech_pro.data.remote.api.UsuarioAdminApiService;
import com.iza.servitech_pro.data.remote.dto.UsuarioAdminDto;
import com.iza.servitech_pro.data.repository.SecurePrefs;
import com.iza.servitech_pro.data.sync.SyncManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsuariosActivity extends AppCompatActivity {

    private static final String[] ROLES = {"TECNICO", "ADMIN"};

    private UsuarioAdminApiService api;
    private UsuariosAdapter adapter;
    private TextView tvEstado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!"ADMIN".equals(SecurePrefs.leerRol(this))) {
            finish(); // pantalla solo para administradores
            return;
        }
        setContentView(R.layout.activity_usuarios);
        api = RetrofitClient.getAdminApi(this);

        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        tvEstado = findViewById(R.id.tvEstado);
        tvEstado.setOnClickListener(v -> cargar());

        RecyclerView rv = findViewById(R.id.rvUsuarios);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsuariosAdapter(this::dialogoPassword);
        rv.setAdapter(adapter);

        findViewById(R.id.fabNuevo).setOnClickListener(v -> dialogoNuevo());
        cargar();
    }

    private boolean activa() {
        return !isFinishing() && !isDestroyed();
    }

    private static String detalle(Response<?> r, String porDefecto) {
        try {
            if (r.errorBody() != null) {
                String s = r.errorBody().string();
                if (!s.isEmpty()) {
                    return s;
                }
            }
        } catch (IOException ignorada) {
        }
        return porDefecto;
    }

    // Si el servidor dice 401, la sincronización lo detecta y manda al login
    private void sesionVencida() {
        SyncManager.getInstance(this).sincronizar();
    }

    private void cargar() {
        tvEstado.setText(R.string.usuarios_cargando);
        tvEstado.setVisibility(View.VISIBLE);
        api.listar(null).enqueue(new Callback<List<UsuarioAdminDto.Usuario>>() {
            @Override
            public void onResponse(Call<List<UsuarioAdminDto.Usuario>> call,
                                   Response<List<UsuarioAdminDto.Usuario>> r) {
                if (!activa()) return;
                if (r.isSuccessful() && r.body() != null) {
                    adapter.submitList(r.body());
                    if (r.body().isEmpty()) {
                        tvEstado.setText(R.string.usuarios_vacio);
                    } else {
                        tvEstado.setVisibility(View.GONE);
                    }
                } else {
                    tvEstado.setText(R.string.usuarios_error);
                    if (r.code() == 401) sesionVencida();
                }
            }

            @Override
            public void onFailure(Call<List<UsuarioAdminDto.Usuario>> call, Throwable t) {
                if (!activa()) return;
                tvEstado.setText(R.string.usuarios_error);
            }
        });
    }

    // ---------- Nuevo usuario ----------

    private void dialogoNuevo() {
        View v = getLayoutInflater().inflate(R.layout.dialog_usuario, null);
        TextInputLayout tilNombre = v.findViewById(R.id.tilNombre);
        TextInputLayout tilUsuario = v.findViewById(R.id.tilUsuario);
        TextInputLayout tilPassword = v.findViewById(R.id.tilPassword);
        EditText etNombre = v.findViewById(R.id.etNombre);
        EditText etUsuario = v.findViewById(R.id.etUsuario);
        EditText etPassword = v.findViewById(R.id.etPassword);
        AutoCompleteTextView acRol = v.findViewById(R.id.acRol);

        List<String> etiquetas = Arrays.asList(
                getString(R.string.usuarios_rol_tecnico), getString(R.string.usuarios_rol_admin));
        acRol.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, etiquetas));
        acRol.setText(etiquetas.get(0), false);
        final int[] rol = {0};
        acRol.setOnItemClickListener((p, view, i, id) -> rol[0] = i);

        AlertDialog d = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.usuarios_nuevo)
                .setView(v)
                .setNegativeButton(R.string.form_cancelar, null)
                .setPositiveButton(R.string.usuarios_crear, null)
                .create();
        d.setOnShowListener(x -> {
            Button crear = d.getButton(AlertDialog.BUTTON_POSITIVE);
            crear.setOnClickListener(b -> {
                String nombre = etNombre.getText().toString().trim();
                String usuario = etUsuario.getText().toString().trim();
                String pass = etPassword.getText().toString();

                tilNombre.setError(null);
                tilUsuario.setError(null);
                tilPassword.setError(null);
                boolean ok = true;
                if (nombre.length() < 2 || nombre.length() > 100) {
                    tilNombre.setError(getString(R.string.usuarios_err_nombre));
                    ok = false;
                }
                if (!usuario.matches("[A-Za-z0-9._-]{3,30}")) {
                    tilUsuario.setError(getString(R.string.usuarios_err_usuario));
                    ok = false;
                }
                if (pass.length() < 8 || pass.length() > 72) {
                    tilPassword.setError(getString(R.string.usuarios_err_password));
                    ok = false;
                }
                if (!ok) return;

                UsuarioAdminDto.Crear req = new UsuarioAdminDto.Crear();
                req.nombre = nombre;
                req.username = usuario;
                req.password = pass;
                req.rol = ROLES[rol[0]];

                crear.setEnabled(false);
                api.crear(req).enqueue(new Callback<UsuarioAdminDto.Usuario>() {
                    @Override
                    public void onResponse(Call<UsuarioAdminDto.Usuario> call,
                                           Response<UsuarioAdminDto.Usuario> r) {
                        if (!activa()) return;
                        crear.setEnabled(true);
                        if (r.isSuccessful()) {
                            d.dismiss();
                            Toast.makeText(UsuariosActivity.this, R.string.usuarios_creado, Toast.LENGTH_SHORT).show();
                            cargar();
                        } else if (r.code() == 409) {
                            tilUsuario.setError(getString(R.string.usuarios_existe));
                        } else if (r.code() == 401) {
                            d.dismiss();
                            sesionVencida();
                        } else {
                            Toast.makeText(UsuariosActivity.this,
                                    detalle(r, "Error del servidor (" + r.code() + ")"), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UsuarioAdminDto.Usuario> call, Throwable t) {
                        if (!activa()) return;
                        crear.setEnabled(true);
                        Toast.makeText(UsuariosActivity.this, R.string.usuarios_error_red, Toast.LENGTH_LONG).show();
                    }
                });
            });
        });
        d.show();
    }

    // ---------- Restablecer contraseña ----------

    private void dialogoPassword(UsuarioAdminDto.Usuario u) {
        View v = getLayoutInflater().inflate(R.layout.dialog_usuario, null);
        v.findViewById(R.id.tilNombre).setVisibility(View.GONE);
        v.findViewById(R.id.tilUsuario).setVisibility(View.GONE);
        v.findViewById(R.id.tilRol).setVisibility(View.GONE);
        TextInputLayout tilPassword = v.findViewById(R.id.tilPassword);
        EditText etPassword = v.findViewById(R.id.etPassword);

        AlertDialog d = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.usuarios_pass_titulo, u.username))
                .setView(v)
                .setNegativeButton(R.string.form_cancelar, null)
                .setPositiveButton(R.string.usuarios_guardar, null)
                .create();
        d.setOnShowListener(x -> {
            Button guardar = d.getButton(AlertDialog.BUTTON_POSITIVE);
            guardar.setOnClickListener(b -> {
                String pass = etPassword.getText().toString();
                tilPassword.setError(null);
                if (pass.length() < 8 || pass.length() > 72) {
                    tilPassword.setError(getString(R.string.usuarios_err_password));
                    return;
                }
                UsuarioAdminDto.Password req = new UsuarioAdminDto.Password();
                req.password = pass;

                guardar.setEnabled(false);
                api.cambiarPassword(u.id, req).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> r) {
                        if (!activa()) return;
                        guardar.setEnabled(true);
                        if (r.isSuccessful()) {
                            d.dismiss();
                            Toast.makeText(UsuariosActivity.this, R.string.usuarios_pass_ok, Toast.LENGTH_SHORT).show();
                        } else if (r.code() == 401) {
                            d.dismiss();
                            sesionVencida();
                        } else {
                            Toast.makeText(UsuariosActivity.this,
                                    detalle(r, "Error del servidor (" + r.code() + ")"), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (!activa()) return;
                        guardar.setEnabled(true);
                        Toast.makeText(UsuariosActivity.this, R.string.usuarios_error_red, Toast.LENGTH_LONG).show();
                    }
                });
            });
        });
        d.show();
    }
}