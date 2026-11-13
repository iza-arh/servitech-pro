package com.iza.servitech_pro.ui.common;

import android.content.Context;
import android.content.SharedPreferences;

import com.iza.servitech_pro.data.remote.RetrofitClient;
import com.iza.servitech_pro.data.remote.dto.UsuarioAdminDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class TecnicosCache {

    private static final String PREFS = "tecnicos_cache";

    // Solo el administrador puede consultar la lista de técnicos
    public static void actualizar(Context ctx, Runnable alTerminar) {
        final Context app = ctx.getApplicationContext();
        if (!Sesion.esAdmin(app)) {
            return;
        }
        RetrofitClient.getAdminApi(app).listar("TECNICO").enqueue(new Callback<List<UsuarioAdminDto.Usuario>>() {
            @Override
            public void onResponse(Call<List<UsuarioAdminDto.Usuario>> call,
                                   Response<List<UsuarioAdminDto.Usuario>> r) {
                if (!r.isSuccessful() || r.body() == null) {
                    return;
                }
                SharedPreferences.Editor e = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear();
                for (UsuarioAdminDto.Usuario u : r.body()) {
                    String nombre = (u.nombre != null && !u.nombre.trim().isEmpty()) ? u.nombre : u.username;
                    e.putString(String.valueOf(u.id), nombre);
                }
                e.apply();
                if (alTerminar != null) {
                    alTerminar.run();
                }
            }

            @Override
            public void onFailure(Call<List<UsuarioAdminDto.Usuario>> call, Throwable t) { }
        });
    }

    public static String nombre(Context ctx, Long id) {
        if (id == null) {
            return null;
        }
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(String.valueOf(id), null);
    }

    // Lista guardada la última vez que se consultó el servidor (sirve sin conexión)
    public static List<UsuarioAdminDto.Usuario> listar(Context ctx) {
        List<UsuarioAdminDto.Usuario> r = new ArrayList<>();
        Map<String, ?> todo = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getAll();
        for (Map.Entry<String, ?> e : todo.entrySet()) {
            try {
                UsuarioAdminDto.Usuario u = new UsuarioAdminDto.Usuario();
                u.id = Long.valueOf(e.getKey());
                u.nombre = String.valueOf(e.getValue());
                r.add(u);
            } catch (NumberFormatException ignorada) {
            }
        }
        Collections.sort(r, (a, b) -> a.nombre.compareToIgnoreCase(b.nombre));
        return r;
    }
}