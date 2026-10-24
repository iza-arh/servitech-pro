package com.iza.servitech_pro.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.iza.servitech_pro.R;
import com.iza.servitech_pro.ui.dashboard.DashboardActivity;
import com.iza.servitech_pro.ui.login.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long DURACION_MS = 2000;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable irSiguiente = () -> {
        // Si ya hay sesión de Firebase va al Home, si no al Login
        boolean haySesion = FirebaseAuth.getInstance().getCurrentUser() != null;
        Class<?> destino = haySesion ? DashboardActivity.class : LoginActivity.class;
        startActivity(new Intent(this, destino));
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(irSiguiente, DURACION_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(irSiguiente);
        super.onDestroy();
    }
}