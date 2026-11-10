package com.iza.servitech_pro;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iza.servitech_pro.data.repository.SecurePrefs;
import com.iza.servitech_pro.data.sync.SyncEstado;
import com.iza.servitech_pro.data.sync.SyncManager;
import com.iza.servitech_pro.ui.login.LoginActivity;

public class ServiTechApp extends Application {

    private Activity actual; // pantalla visible en este momento

    @Override
    public void onCreate() {
        super.onCreate();
        SyncManager sync = SyncManager.getInstance(this);
        sync.iniciar();

        // Si el servidor dice que la sesión expiró, se vuelve al login
        sync.estado().observeForever(e -> {
            if (e == SyncEstado.SESION_EXPIRADA && actual != null && !(actual instanceof LoginActivity)) {
                cerrarPorExpiracion();
            }
        });

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity a) {
                actual = a;
                sync.alMostrarseActividad();
                if (!(a instanceof LoginActivity) && sync.estado().getValue() == SyncEstado.SESION_EXPIRADA) {
                    cerrarPorExpiracion();
                }
            }

            @Override
            public void onActivityStopped(@NonNull Activity a) {
                if (actual == a) {
                    actual = null;
                }
                sync.alOcultarseActividad();
            }

            @Override
            public void onActivityCreated(@NonNull Activity a, @Nullable Bundle b) { }

            @Override
            public void onActivityResumed(@NonNull Activity a) { }

            @Override
            public void onActivityPaused(@NonNull Activity a) { }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) { }

            @Override
            public void onActivityDestroyed(@NonNull Activity a) { }
        });
    }

    // Se borra solo la sesión; los datos locales se conservan por si hay cambios sin enviar
    private void cerrarPorExpiracion() {
        SecurePrefs.cerrarSesion(this);
        Intent i = new Intent(this, LoginActivity.class);
        i.putExtra(LoginActivity.EXTRA_EXPIRADA, true);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}