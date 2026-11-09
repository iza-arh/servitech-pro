package com.iza.servitech_pro;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iza.servitech_pro.data.sync.SyncManager;

public class ServiTechApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SyncManager sync = SyncManager.getInstance(this);
        sync.iniciar();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity a) {
                sync.alMostrarseActividad();
            }

            @Override
            public void onActivityStopped(@NonNull Activity a) {
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
}