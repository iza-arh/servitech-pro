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
    }

    private void intentarRegistro() {
        layoutErrorMessage.setVisibility(View.GONE);

        String usuario = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmacion = etConfirmPassword.getText().toString();

        // Validaciones locales antes de llamar al servidor
        if (usuario.isEmpty()) {
            mostrarError("Ingresa tu usuario");
            return;
        }
        // Si el campo se queda como Email, agrega aquí:
        // if (!Patterns.EMAIL_ADDRESS.matcher(usuario).matches()) { ... }
        if (password.length() < 8) {
            mostrarError("La contraseña debe tener al menos 8 caracteres");
            return;
        }
        if (!password.equals(confirmacion)) {
            mostrarError("Las contraseñas no coinciden");
            return;
        }

        RegisterRequest request = new RegisterRequest();
        request.setUsername(usuario);
        request.setPassword(password);

        RetrofitClient.getAuthApi().register(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) { // 201
                    Toast.makeText(LoginActivity.this,
                            "Cuenta creada. Inicia sesión", Toast.LENGTH_SHORT).show();
                    etPassword.setText("");
                    etConfirmPassword.setText("");
                    cambiarModo(false); // vuelve al modo login
                } else if (response.code() == 409) {
                    mostrarError("Ese usuario ya está registrado");
                } else if (response.code() == 400) {
                    mostrarError("Datos inválidos");
                } else {
                    mostrarError("Error del servidor (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("REGISTER", "Fallo la peticion de registro", t);
                mostrarError("Error de conexión");
            }
        });
    }
}