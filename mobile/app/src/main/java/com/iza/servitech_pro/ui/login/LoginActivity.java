package com.iza.servitech_pro.ui.login;

import com.google.android.material.chip.Chip;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.data.remote.RetrofitClient;
import com.iza.servitech_pro.data.remote.dto.LoginRequest;
import com.iza.servitech_pro.data.remote.dto.LoginResponse;
import com.iza.servitech_pro.data.remote.dto.RegisterRequest;
import com.iza.servitech_pro.data.repository.SecurePrefs;
import com.iza.servitech_pro.ui.dashboard.DashboardActivity;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.iza.servitech_pro.util.Validador;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private LinearLayout layoutErrorMessage;
    private TextView tvError;
    private TextInputLayout tilConfirmPassword;
    private EditText etConfirmPassword;

    private TextView btnModoLogin, btnModoRegistro;
    private boolean modoRegistro = false;

    private TextInputLayout tilEmail, tilPassword;
    private Chip chipRecordarme;

    private TextInputLayout tilNombre;
    private EditText etNombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Carga el layout correcto del login
        setContentView(R.layout.activity_login);

        // enlaces a los elementos del layout
        etUsername = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        layoutErrorMessage = findViewById(R.id.layoutErrorMessage);
        tvError = findViewById(R.id.tvError);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnModoLogin = findViewById(R.id.btnModoLogin);
        btnModoRegistro = findViewById(R.id.btnModoRegistro);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);

        chipRecordarme = findViewById(R.id.chipRecordarme);
        tilNombre = findViewById(R.id.tilNombre);
        etNombre = findViewById(R.id.etNombre);

        btnModoLogin.setOnClickListener(v -> cambiarModo(false));
        btnModoRegistro.setOnClickListener(v -> cambiarModo(true));
        cambiarModo(false); // arranca en modo login
        cargarCredencialesRecordadas();

        // El mismo botón ejecuta login o registro según el modo actual
        btnLogin.setOnClickListener(v -> {
            if (modoRegistro) {
                intentarRegistro();
            } else {
                intentarLogin();
            }
        });

    }

    private void intentarLogin() {
        layoutErrorMessage.setVisibility(View.GONE);
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Validaciones locales antes de llamar a Firebase
        boolean valido = true;
        if (!Validador.esCorreoValido(email)) {
            tilEmail.setError("Ingresa un correo válido");
            valido = false;
        }
        if (!Validador.esPasswordValida(password)) {
            tilPassword.setError("Mínimo 8 caracteres, solo letras y números (al menos una letra y un número)");
            valido = false;
        }
        if (!valido) return;

        btnLogin.setEnabled(false);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        if (chipRecordarme.isChecked()) {
                            SecurePrefs.guardarCredenciales(this, email, password);
                        } else {
                            SecurePrefs.borrarCredenciales(this);
                        }
                        startActivity(new Intent(this, DashboardActivity.class));
                        finish();
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthInvalidCredentialsException
                                || e instanceof FirebaseAuthInvalidUserException) {
                            mostrarError(getString(R.string.login_error_credenciales));
                        } else if (e instanceof FirebaseNetworkException) {
                            mostrarError("Sin conexión a internet");
                        } else {
                            Log.e("LOGIN", "Error al iniciar sesión", e);
                            mostrarError("Error al iniciar sesión");
                        }
                    }
                });
    }

    private void cargarCredencialesRecordadas() {
        if (SecurePrefs.recordarActivo(this)) {
            etUsername.setText(SecurePrefs.leerEmailRecordado(this));
            etPassword.setText(SecurePrefs.leerPasswordRecordada(this));
            chipRecordarme.setChecked(true);
        }
    }

    // Muestra el mensaje de error debajo del campo de contraseña
    private void mostrarError(String mensaje) {
        tvError.setText(mensaje);
        layoutErrorMessage.setVisibility(View.VISIBLE);
    }

    private void irSegunRol(String rol) {
        Intent intent;
        switch (rol) {
            case "ADMIN":
            case "TECNICO":
            case "CLIENTE":
            default:
                intent = new Intent(this, DashboardActivity.class); // por ahora todos van al mismo destino
                break;
        }
        startActivity(intent);
        finish();
    }

    // Cambia entre modo login y modo registro sin abrir otra pantalla
    private void cambiarModo(boolean registro) {
        modoRegistro = registro;
        // Marca el segmento activo (el fondo blanco depende del estado "selected")
        btnModoLogin.setSelected(!registro);
        btnModoRegistro.setSelected(registro);

        layoutErrorMessage.setVisibility(View.GONE);
        tilConfirmPassword.setVisibility(registro ? View.VISIBLE : View.GONE);
        btnLogin.setText(registro ? R.string.login_boton_registro : R.string.login_boton);
        chipRecordarme.setVisibility(registro ? View.GONE : View.VISIBLE);
        tilNombre.setVisibility(registro ? View.VISIBLE : View.GONE);
        tilNombre.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void intentarRegistro() {
        layoutErrorMessage.setVisibility(View.GONE);
        tilNombre.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String nombre = etNombre.getText().toString().trim();
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmacion = etConfirmPassword.getText().toString();

        boolean valido = true;
        if (!Validador.esNombreValido(nombre)) {
            tilNombre.setError("El nombre debe tener al menos 7 caracteres");
            valido = false;
        }
        if (!Validador.esCorreoValido(email)) {
            tilEmail.setError("Ingresa un correo válido");
            valido = false;
        }
        if (!Validador.esPasswordValida(password)) {
            tilPassword.setError("Mínimo 8 caracteres, solo letras y números (al menos una letra y un número)");
            valido = false;
        }
        if (!password.equals(confirmacion)) {
            tilConfirmPassword.setError("Las contraseñas no coinciden");
            valido = false;
        }
        if (!valido) return;

        btnLogin.setEnabled(false);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        btnLogin.setEnabled(true);
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            mostrarError("Ese correo ya está registrado");
                        } else if (e instanceof FirebaseNetworkException) {
                            mostrarError("Sin conexión a internet");
                        } else {
                            Log.e("REGISTER", "Error al crear la cuenta", e);
                            mostrarError("No se pudo crear la cuenta");
                        }
                        return;
                    }

                    String uid = task.getResult().getUser().getUid();
                    Map<String, Object> perfil = new HashMap<>();
                    perfil.put("uid", uid);
                    perfil.put("email", email);
                    perfil.put("nombre", nombre); // la contraseña nunca se guarda aquí

                    FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                            .set(perfil)
                            .addOnSuccessListener(v -> {
                                // TODO (A7, parte Room): guardar también en Room (UsuarioLocal)
                                // cuando B1 esté en la rama
                                auth.signOut();
                                btnLogin.setEnabled(true);
                                Toast.makeText(this, "Cuenta creada. Inicia sesión",
                                        Toast.LENGTH_SHORT).show();
                                etNombre.setText("");
                                etPassword.setText("");
                                etConfirmPassword.setText("");
                                cambiarModo(false);
                            })
                            .addOnFailureListener(err -> {
                                Log.e("REGISTER", "Error al guardar el perfil", err);
                                auth.signOut();
                                btnLogin.setEnabled(true);
                                mostrarError("Error al guardar el perfil en la nube");
                            });
                });
    }
}