package com.iza.servitech_pro.ui.perfil;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.local.AppDatabase;
import com.iza.servitech_pro.data.local.dao.UsuarioLocalDao;
import com.iza.servitech_pro.data.local.entity.UsuarioLocal;
import com.iza.servitech_pro.data.repository.SecurePrefs;
import com.iza.servitech_pro.util.Validador;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PerfilActivity extends AppCompatActivity {

    private TextView tvCorreo, tvPendiente;
    private TextInputLayout tilNombre, tilActual, tilNueva, tilConfirmar;
    private TextInputEditText etNombre, etActual, etNueva, etConfirmar;
    private Button btnGuardar, btnReintentar;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private FirebaseUser usuario;
    private String nombreActual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Perfil");
        }

        usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            finish();
            return;
        }

        tvCorreo = findViewById(R.id.tvCorreo);
        tvPendiente = findViewById(R.id.tvPendiente);
        tilNombre = findViewById(R.id.tilNombre);
        tilActual = findViewById(R.id.tilActual);
        tilNueva = findViewById(R.id.tilNueva);
        tilConfirmar = findViewById(R.id.tilConfirmar);
        etNombre = findViewById(R.id.etNombre);
        etActual = findViewById(R.id.etActual);
        etNueva = findViewById(R.id.etNueva);
        etConfirmar = findViewById(R.id.etConfirmar);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnReintentar = findViewById(R.id.btnReintentar);

        tvCorreo.setText(usuario.getEmail());
        btnGuardar.setOnClickListener(v -> pedirConfirmacion());
        btnReintentar.setOnClickListener(v -> reintentarSincronizacion(true));

        cargarPerfil();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        io.shutdown();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // ---------- Carga ----------

    private UsuarioLocalDao dao() {
        return AppDatabase.getInstance(getApplicationContext()).usuarioLocalDao();
    }

    private void cargarPerfil() {
        io.execute(() -> {
            UsuarioLocal local = dao().obtenerPorUid(usuario.getUid());
            runOnUiThread(() -> {
                if (local != null) {
                    nombreActual = local.nombre;
                    etNombre.setText(local.nombre);
                    actualizarBanner(local.pendienteSync);
                    if (local.pendienteSync) {
                        reintentarSincronizacion(false); // reintento automático al abrir
                    }
                } else {
                    cargarDesdeNube();
                }
            });
        });
    }

    // Si no hay datos locales (ej. instalación nueva) intenta traerlos de Firestore
    private void cargarDesdeNube() {
        if (!hayInternet()) return;
        FirebaseFirestore.getInstance().collection("usuarios").document(usuario.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String nombre = doc.exists() && doc.getString("nombre") != null
                            ? doc.getString("nombre") : "";
                    nombreActual = nombre;
                    etNombre.setText(nombre);
                    if (!nombre.isEmpty()) {
                        io.execute(() -> dao().insertar(new UsuarioLocal(
                                usuario.getUid(), usuario.getEmail(), nombre, null, false)));
                    }
                });
    }

    // ---------- Validación y confirmación ----------

    private String texto(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString();
    }

    private void pedirConfirmacion() {
        tilNombre.setError(null);
        tilActual.setError(null);
        tilNueva.setError(null);
        tilConfirmar.setError(null);

        String nombre = texto(etNombre).trim();
        String actual = texto(etActual);
        String nueva = texto(etNueva);
        String confirmar = texto(etConfirmar);
        boolean cambiaPassword = !nueva.isEmpty() || !actual.isEmpty() || !confirmar.isEmpty();
        boolean cambiaNombre = !nombre.equals(nombreActual);

        boolean valido = true;
        if (!Validador.esNombreValido(nombre)) {
            tilNombre.setError("El nombre debe tener al menos 7 caracteres");
            valido = false;
        }
        if (cambiaPassword) {
            if (actual.isEmpty()) {
                tilActual.setError("Ingresa tu contraseña actual");
                valido = false;
            }
            if (!Validador.esPasswordValida(nueva)) {
                tilNueva.setError("Mínimo 8 caracteres, solo letras y números (al menos una letra y un número)");
                valido = false;
            }
            if (!nueva.equals(confirmar)) {
                tilConfirmar.setError("Las contraseñas no coinciden");
                valido = false;
            }
        }
        if (!valido) return;

        if (!cambiaNombre && !cambiaPassword) {
            Toast.makeText(this, "No hay cambios para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar cambios")
                .setMessage("¿Deseas guardar los cambios de tu perfil?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (d, w) ->
                        guardar(nombre, cambiaNombre, actual, nueva, cambiaPassword))
                .show();
    }

    // ---------- Guardado ----------

    private void guardar(String nombre, boolean cambiaNombre, String actual,
                         String nueva, boolean cambiaPassword) {
        if (cambiaPassword && !hayInternet()) {
            Toast.makeText(this, "Para cambiar la contraseña necesitas conexión a internet",
                    Toast.LENGTH_LONG).show();
            return;
        }
        btnGuardar.setEnabled(false);

        if (cambiaPassword) {
            cambiarPassword(actual, nueva, () -> {
                // Si "Recordarme" está activo, actualiza la contraseña cifrada guardada
                if (SecurePrefs.recordarActivo(this)) {
                    SecurePrefs.guardarCredenciales(this, usuario.getEmail(), nueva);
                }
                Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show();
                if (cambiaNombre) {
                    guardarNombre(nombre);
                } else {
                    terminar("Contraseña actualizada");
                }
            });
        } else {
            guardarNombre(nombre);
        }
    }

    private void cambiarPassword(String actual, String nueva, Runnable alExito) {
        AuthCredential credencial = EmailAuthProvider.getCredential(usuario.getEmail(), actual);
        usuario.reauthenticate(credencial)
                .addOnSuccessListener(a -> usuario.updatePassword(nueva)
                        .addOnSuccessListener(b -> alExito.run())
                        .addOnFailureListener(this::falloPassword))
                .addOnFailureListener(this::falloPassword);
    }

    private void falloPassword(Exception e) {
        btnGuardar.setEnabled(true);
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            tilActual.setError("La contraseña actual es incorrecta");
        } else if (e instanceof FirebaseNetworkException) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_LONG).show();
        } else {
            Log.e("PERFIL", "Error al cambiar la contraseña", e);
            Toast.makeText(this, "No se pudo cambiar la contraseña", Toast.LENGTH_LONG).show();
        }
    }

    // Guarda primero en Room (pendiente) y luego intenta la nube
    private void guardarNombre(String nombre) {
        String uid = usuario.getUid();
        String email = usuario.getEmail();
        io.execute(() -> {
            UsuarioLocal u = dao().obtenerPorUid(uid);
            if (u == null) {
                u = new UsuarioLocal(uid, email, nombre, null, true);
            } else {
                u.nombre = nombre;
                u.pendienteSync = true; // queda pendiente hasta que la nube confirme
            }
            dao().insertar(u);
            runOnUiThread(() -> subirNombreANube(uid, email, nombre,
                    () -> marcarSincronizado(uid, "Perfil actualizado en la nube y en local"),
                    () -> terminar("Guardado en local. Pendiente de sincronizar con la nube")));
        });
    }

    private void marcarSincronizado(String uid, String mensaje) {
        io.execute(() -> {
            UsuarioLocal u = dao().obtenerPorUid(uid);
            if (u != null) {
                u.pendienteSync = false;
                dao().actualizar(u);
            }
            runOnUiThread(() -> terminar(mensaje));
        });
    }

    // ---------- Sincronización con la nube ----------

    // Firestore no falla sin red: la tarea se queda esperando.
    // Por eso se revisa la conexión antes y se usa un tiempo máximo de 8 segundos.
    private void subirNombreANube(String uid, String email, String nombre,
                                  Runnable alExito, Runnable alFallo) {
        if (!hayInternet()) {
            alFallo.run();
            return;
        }
        AtomicBoolean terminado = new AtomicBoolean(false);
        Runnable timeout = () -> {
            if (terminado.compareAndSet(false, true)) alFallo.run();
        };
        mainHandler.postDelayed(timeout, 8000);

        Map<String, Object> datos = new HashMap<>();
        datos.put("uid", uid);
        datos.put("email", email);
        datos.put("nombre", nombre);

        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                .set(datos, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    if (terminado.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(timeout);
                        alExito.run();
                    }
                })
                .addOnFailureListener(e -> {
                    if (terminado.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(timeout);
                        alFallo.run();
                    }
                });
    }

    // A11: reintento de lo pendiente (automático al abrir y con el botón)
    private void reintentarSincronizacion(boolean manual) {
        String uid = usuario.getUid();
        io.execute(() -> {
            UsuarioLocal u = dao().obtenerPorUid(uid);
            if (u == null || !u.pendienteSync) {
                runOnUiThread(() -> actualizarBanner(false));
                return;
            }
            runOnUiThread(() -> subirNombreANube(uid, u.email, u.nombre,
                    () -> marcarSincronizado(uid, "Sincronizado con la nube"),
                    () -> {
                        if (manual) {
                            Toast.makeText(this, "Sin conexión. Se reintentará más tarde",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }));
        });
    }

    // ---------- Utilidades ----------

    private void terminar(String mensaje) {
        btnGuardar.setEnabled(true);
        etActual.setText("");
        etNueva.setText("");
        etConfirmar.setText("");
        nombreActual = texto(etNombre).trim();
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        io.execute(() -> {
            UsuarioLocal u = dao().obtenerPorUid(usuario.getUid());
            runOnUiThread(() -> actualizarBanner(u != null && u.pendienteSync));
        });
    }

    private void actualizarBanner(boolean pendiente) {
        tvPendiente.setVisibility(pendiente ? View.VISIBLE : View.GONE);
        btnReintentar.setVisibility(pendiente ? View.VISIBLE : View.GONE);
    }

    private boolean hayInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network red = cm.getActiveNetwork();
        if (red == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(red);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}